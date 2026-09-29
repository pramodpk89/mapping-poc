"""Repeatable local acceptance tests. AI analysis fixtures are explicitly synthetic."""
import copy
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest

ROOT=Path(__file__).resolve().parents[2]

class WorkflowTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory(prefix='mapping-tests-')
        self.root=Path(self.temp.name)/'Analyst folder Ω'
        shutil.copytree(ROOT,self.root,ignore=shutil.ignore_patterns('__pycache__','history','tests','Last-review.html','run-history.jsonl'))
        self.fw=self.root/'.framework'
    def tearDown(self):self.temp.cleanup()
    def run_script(self,name,*args):
        return subprocess.run([sys.executable,str(self.fw/name),*args],cwd=self.temp.name,capture_output=True,text=True)
    def load(self,name):return json.loads((self.fw/name).read_text(encoding='utf-8'))
    def save(self,name,data):(self.fw/name).write_text(json.dumps(data,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
    def prepare(self,*args):
        r=self.run_script('prepare_run.py','--purpose','TEST: provide availability information to ecommerce.',*args)
        self.assertEqual(r.returncode,0,r.stdout+r.stderr)
    def synthetic_analysis(self,mutator=None):
        """Simulate the AI-output boundary; this does not test Copilot reasoning."""
        i=self.load('input.json');a=self.load('analysis.json')
        a['revision']+=1;a['summary']='SYNTHETIC TEST: analysis fixture for workflow verification.'
        for rule in i['known_rules']:
            a['evidence']=[e for e in a['evidence'] if e['id']!=rule['id']]
            a['evidence'].append({'id':rule['id'],'title':'Test analyst decision','url':'Decisions.csv','note':rule['statement']})
        if mutator:mutator(i,a)
        a['input_sha256']=hashlib.sha256(json.dumps(i,sort_keys=True,ensure_ascii=False,separators=(',',':')).encode()).hexdigest()
        self.save('input.json',i);self.save('analysis.json',a)
    def generate(self,code=0):
        r=self.run_script('generate_report.py');self.assertEqual(r.returncode,code,r.stdout+r.stderr)
        return (self.root/'Report.html').read_text()
    def decisions(self,rows,delimiter=',',encoding='utf-8-sig'):
        import csv
        with (self.root/'Decisions.csv').open('w',encoding=encoding,newline='') as f:
            w=csv.writer(f,delimiter=delimiter);w.writerow(['Attribute','Decision','Explanation','Confirmed by']);w.writerows(rows)

    def test_01_missing_description_blocks(self):
        """First run names the missing purpose instead of producing mappings."""
        r=self.run_script('prepare_run.py');self.assertEqual(r.returncode,2)
        self.assertIn('Understanding.txt',r.stdout);self.assertNotIn('mapping-rows',(self.root/'Report.html').read_text())
    def test_02_basic_run(self):
        """One business description plus existing evidence produces a seven-field report."""
        self.prepare();self.synthetic_analysis();text=self.generate()
        self.assertIn('mapping-rows',text);self.assertEqual(len(self.load('analysis.json')['mappings']),7)
        self.assertTrue((self.root/'Last-review.html').exists())
    def test_03_multiline_description(self):
        """A description on the next line is accepted."""
        p=self.root/'Understanding.txt';p.write_text(p.read_text().replace('[Please add one sentence describing the business purpose]','\nProvides stock information.'))
        self.assertEqual(self.run_script('prepare_run.py').returncode,0)
    def test_04_utf16_notes(self):
        """Windows UTF-16 notes are read without errors."""
        self.prepare();p=self.root/'Understanding.txt';p.write_bytes(p.read_text().encode('utf-16'))
        self.assertEqual(self.run_script('prepare_run.py').returncode,0)
    def test_05_unknown_purpose_blocks(self):
        self.assertEqual(self.run_script('prepare_run.py','--purpose','Unknown.').returncode,2)
    def test_06_missing_source_blocks(self):
        self.prepare();shutil.rmtree(self.root/'Current/Source')
        self.assertEqual(self.run_script('prepare_run.py').returncode,2)
    def test_07_empty_xml_is_not_evidence(self):
        self.prepare();shutil.rmtree(self.root/'Current/Source');(self.root/'Current/Source').mkdir()
        (self.root/'Current/Source/empty.xml').write_text('<empty/>')
        self.assertEqual(self.run_script('prepare_run.py').returncode,2)
    def test_08_malformed_sample_with_good_contract(self):
        self.prepare();self.assertTrue(any('availability-excerpt.xml' in s for s in self.load('preflight.json')['warnings']))
    def test_09_missing_target_blocks(self):
        self.prepare();(self.root/'Shopify/Target-reference.json').unlink()
        self.assertEqual(self.run_script('prepare_run.py').returncode,2)
    def test_10_unofficial_target_blocks(self):
        self.prepare();p=self.root/'Shopify/Target-reference.json';t=json.loads(p.read_text())
        for e in t['evidence']:e['url']='https://shopify.dev.example.com/fake'
        p.write_text(json.dumps(t));self.assertEqual(self.run_script('prepare_run.py').returncode,2)
    def test_11_clarification_preserves_other_answers(self):
        """A second clarification preserves the first and makes old analysis stale."""
        self.prepare('--answer','Q01','Display information.');self.synthetic_analysis();self.generate()
        self.assertEqual(self.run_script('prepare_run.py','--answer','Q03','Absolute stock totals.').returncode,0)
        self.assertEqual(self.load('input.json')['answers']['Q01'],'Display information.')
        self.generate(3)
    def test_12_clearing_answer_is_not_ignored(self):
        self.prepare('--answer','Q03','An absolute total.');self.run_script('prepare_run.py','--answer','Q03','')
        self.assertEqual(self.load('input.json')['answers']['Q03'],'')
    def test_13_rerun_after_clarification(self):
        self.prepare();self.synthetic_analysis();self.generate()
        self.run_script('prepare_run.py','--answer','Q08','N is normal stock; do not send this attribute.','--decision','SKUType','Exclude SKUType from target payload.','Internal classification only.','Test analyst')
        def confirm(i,a):
            m=next(m for m in a['mappings'] if m['source_field']=='SKUType')
            m.update(status='excluded',decision_basis='confirmed',confirmed_by='Test analyst',target='Excluded by agreement',reason='The test analyst confirmed that SKUType is internal classification only.',proposed_rule='Do not send SKUType; preserve it in the source evidence.')
            m['evidence_ids'].append('decision:SKUType')
            q=next(q for q in a['questions'] if q['id']=='Q08');q.update(review_status='resolved',resolution_note='Test analyst confirmed exclusion.')
        self.synthetic_analysis(confirm);self.generate()
        self.assertEqual(next(m for m in self.load('analysis.json')['mappings'] if m['source_field']=='SKUType')['status'],'excluded')
    def test_14_changed_decision_reopens_old_confirmation(self):
        self.test_13_rerun_after_clarification()
        self.run_script('prepare_run.py','--decision','SKUType','Keep SKUType; classification rules are unclear.','Revised decision.','')
        self.synthetic_analysis();self.generate(2)
    def test_15_conflicting_decisions_allow_discovery_not_confirmation(self):
        self.prepare();self.decisions([['SKUType','Exclude','','Analyst A'],['SKUType','Keep','','Analyst B']])
        self.assertEqual(self.run_script('prepare_run.py').returncode,0)
        self.assertEqual(self.load('input.json')['known_rules'],[])
        self.assertTrue(any('Conflicting' in s for s in self.load('preflight.json')['warnings']))
        self.synthetic_analysis();self.assertIn('Conflicting',self.generate())
    def test_16_excel_semicolon_and_quoted_comma(self):
        self.prepare();self.decisions([['SKUType','Exclude, for now','A quoted, detailed note','Test analyst']],delimiter=';')
        self.assertEqual(self.run_script('prepare_run.py').returncode,0)
        self.assertEqual(self.load('input.json')['known_rules'][0]['statement'],'Exclude, for now')
    def test_17_bad_csv_columns_blocks(self):
        self.prepare();(self.root/'Decisions.csv').write_text('Bad,Columns\nfoo,bar\n')
        self.assertEqual(self.run_script('prepare_run.py').returncode,2)
    def test_18_no_invented_reviewer(self):
        self.prepare('--answer','Q08','N means normal.')
        def bad(i,a):a['mappings'][5].update(status='excluded',decision_basis='confirmed',confirmed_by='Imaginary reviewer')
        self.synthetic_analysis(bad);self.generate(2)
    def test_19_unknown_is_not_an_answer(self):
        self.prepare('--answer','Q08','unknown','--decision','SKUType','Exclude','','Test analyst')
        def bad(i,a):
            a['mappings'][5].update(status='excluded',decision_basis='confirmed',confirmed_by='Test analyst')
            a['mappings'][5]['evidence_ids'].append('decision:SKUType')
        self.synthetic_analysis(bad);self.generate(2)
    def test_20_missing_field_rejected(self):
        self.prepare();self.synthetic_analysis(lambda i,a:a['mappings'].pop());self.generate(2)
    def test_21_duplicate_field_rejected(self):
        self.prepare();self.synthetic_analysis(lambda i,a:a['mappings'].append(copy.deepcopy(a['mappings'][0])));self.generate(2)
    def test_22_unknown_evidence_rejected(self):
        self.prepare();self.synthetic_analysis(lambda i,a:a['mappings'][0]['evidence_ids'].append('invented'));self.generate(2)
    def test_23_blank_source_fields_rejected(self):
        self.prepare();self.synthetic_analysis(lambda i,a:(i['source'].update(fields=[]),a.update(mappings=[])));self.generate(2)
    def test_24_source_type_mismatch_rejected(self):
        self.prepare();self.synthetic_analysis(lambda i,a:a['mappings'][2].update(source_type='xs:string'));self.generate(2)
    def test_25_changed_source_invalidates_report(self):
        self.prepare();self.synthetic_analysis();self.generate()
        with (self.root/'Current/Source/checkout-1.wsdl').open('a') as f:f.write('\n')
        self.generate(3)
    def test_26_last_review_survives_failure(self):
        self.prepare();self.synthetic_analysis();self.generate();saved=(self.root/'Last-review.html').read_bytes()
        self.run_script('prepare_run.py','--purpose','unknown');self.generate(2)
        self.assertEqual(saved,(self.root/'Last-review.html').read_bytes())
        self.assertIn('Last-review.html',(self.root/'Report.html').read_text())
    def test_27_html_answer_roundtrip(self):
        """A report-export-shaped JSON file imports into analyst documents and reruns."""
        self.prepare();self.synthetic_analysis();self.generate();payload=self.load('input.json')
        payload['answers']['Q06']='The future date means unavailable; do not promise that date.'
        file=self.root/'saved-answers.json';file.write_text(json.dumps(payload))
        self.assertEqual(self.run_script('prepare_run.py','--import-answers',str(file)).returncode,0)
        self.assertIn('future date',self.load('input.json')['answers']['Q06']);self.generate(3)
        self.synthetic_analysis();self.generate()
    def test_28_stale_import_does_not_overwrite_newer_notes(self):
        self.prepare();payload=self.load('input.json');f=self.root/'old-answers.json';f.write_text(json.dumps(payload))
        self.run_script('prepare_run.py','--answer','Q01','A newer clarification.')
        before=(self.root/'Questions.txt').read_bytes()
        self.assertEqual(self.run_script('prepare_run.py','--import-answers',str(f)).returncode,2)
        self.assertEqual(before,(self.root/'Questions.txt').read_bytes())
    def test_29_foreign_interface_import_rejected(self):
        self.prepare();payload=self.load('input.json');payload['interface']['id']='other'
        f=self.root/'wrong.json';f.write_text(json.dumps(payload));self.assertEqual(self.run_script('prepare_run.py','--import-answers',str(f)).returncode,2)
    def test_30_no_internal_only_clarification(self):
        """Refreshing a hash cannot hide a made-up answer absent from analyst files."""
        self.prepare();self.synthetic_analysis(lambda i,a:i['answers'].update(Q01='Invented business answer.'));self.generate(2)
    def test_31_resolved_question_needs_review_note(self):
        self.prepare('--answer','Q08','Normal stock.')
        self.synthetic_analysis(lambda i,a:a['questions'][7].update(review_status='resolved'));self.generate(2)
    def test_32_html_injection_is_inert(self):
        self.prepare('--context','</script><img src=x onerror=alert(1)>');self.synthetic_analysis();text=self.generate()
        self.assertNotIn('</script><img src=x',text);self.assertIn('\\u003c/script',text)
    def test_33_working_directory_and_spaces(self):
        self.prepare();self.synthetic_analysis();self.generate()
    def test_34_boolean_settings_must_be_boolean(self):
        p=self.fw/'Required-inputs.json';cfg=json.loads(p.read_text());cfg['require_description']='false';p.write_text(json.dumps(cfg))
        self.assertEqual(self.run_script('prepare_run.py').returncode,2)
    def test_35_duplicate_question_ids_rejected(self):
        self.prepare();p=self.root/'Questions.txt';p.write_text(p.read_text()+'\nQ01: duplicate\nAnswer: conflicting\n')
        self.assertEqual(self.run_script('prepare_run.py').returncode,2)
    def test_36_original_evidence_preserved(self):
        before={p.name:p.read_bytes() for p in (self.root/'Current/Source').iterdir() if p.is_file()}
        self.prepare('--answer','Q02','Unknown origin.');self.synthetic_analysis();self.generate()
        self.assertEqual(before,{p.name:p.read_bytes() for p in (self.root/'Current/Source').iterdir() if p.is_file()})
    def test_37_ready_field_with_business_confirmation(self):
        """A supported field can become ready while overall interface gates remain open."""
        self.prepare('--answer','Q01','Update sellable inventory.','--answer','Q03','ERP owns absolute sellable totals.','--answer','Q04','One fulfillment location.','--decision','AvailableQuantity','Map the absolute sellable total to available inventory.','One location; IDs are a separate lookup.','Test analyst')
        def confirm(i,a):
            i['target']['selected_operation']='inventorySetQuantities'
            a['mappings'][2].update(status='ready',decision_basis='confirmed',confirmed_by='Test analyst',target='inventorySetQuantities.input.quantities[].quantity',reason='The test analyst confirmed absolute sellable stock for one fulfillment location.',proposed_rule='Use the source integer as the available quantity. Preserve zero; missing values require review. Resolve item and location IDs separately.')
            a['mappings'][2]['evidence_ids'].append('decision:AvailableQuantity')
        self.synthetic_analysis(confirm);self.generate()
        self.assertTrue(any(g['status']=='open' for g in self.load('analysis.json')['interface_gates']))
    def test_38_ready_requires_target_choice(self):
        self.test_37_ready_field_with_business_confirmation()
        self.synthetic_analysis(lambda i,a:i['target'].update(selected_operation=None));self.generate(2)
    def test_39_new_contract_field_requires_new_mapping(self):
        self.prepare();p=self.root/'Current/Source/checkout-1.wsdl';text=p.read_text()
        start=text.index('complexType name="WebAvailabilityItem"');pos=text.index('</xs:sequence>',start)
        text=text[:pos]+'<xs:element minOccurs="0" name="NewField" type="xs:string"/>'+text[pos:];p.write_text(text)
        self.assertEqual(self.run_script('prepare_run.py').returncode,0)
        self.synthetic_analysis();self.generate(2)
    def test_40_unknown_question_does_not_erase_answers(self):
        self.prepare('--answer','Q01','Known answer.');before=(self.root/'Questions.txt').read_bytes()
        self.assertEqual(self.run_script('prepare_run.py','--answer','Q999','Mistyped ID.').returncode,2)
        self.assertEqual(before,(self.root/'Questions.txt').read_bytes())
    def test_41_unicode_multiline_and_shell_characters_preserved(self):
        answer='Analyst’s note: "available" means stock.\nKeep ₹, commas, $(text) and `literal` unchanged.'
        self.prepare('--answer','Q03',answer);self.assertEqual(self.load('input.json')['answers']['Q03'],answer)
    def test_42_missing_framework_setting_reports_actionable_error(self):
        self.prepare();(self.fw/'Required-inputs.json').write_text('{broken')
        self.assertEqual(self.run_script('prepare_run.py').returncode,2)
        self.assertIn('Input needs attention',(self.root/'Report.html').read_text())
    def test_43_status_change_cannot_keep_old_explanation(self):
        self.test_13_rerun_after_clarification()
        def stale_copy(i,a):
            a['mappings'][5].update(status='needs_input',decision_basis='proposal',confirmed_by=None)
        self.synthetic_analysis(stale_copy);self.generate(2)

if __name__=='__main__':unittest.main(verbosity=2)
