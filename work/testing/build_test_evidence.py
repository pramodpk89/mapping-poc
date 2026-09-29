from pathlib import Path
import importlib.util,json,shutil,unittest,html,datetime
BASE=Path('/Users/pramodp/Documents/Codex/2026-09-29/basi')
OUT=BASE/'outputs/mapping-test-results';OUT.mkdir(exist_ok=True)
spec=importlib.util.spec_from_file_location('workflow',BASE/'outputs/copilot-mapping-poc/.framework/tests/test_workflow.py')
mod=importlib.util.module_from_spec(spec);spec.loader.exec_module(mod)
class Recorded(unittest.TextTestResult):
 def startTest(self,test):super().startTest(test);self.rows=getattr(self,'rows',[])
 def addSuccess(self,test):super().addSuccess(test);self.rows.append({'test':test._testMethodName,'status':'pass','description':test.shortDescription() or test._testMethodName.split('_',2)[-1].replace('_',' ')})
 def addFailure(self,test,err):super().addFailure(test,err);self.rows.append({'test':test._testMethodName,'status':'fail','description':self._exc_info_to_string(err,test)})
 def addError(self,test,err):super().addError(test,err);self.rows.append({'test':test._testMethodName,'status':'error','description':self._exc_info_to_string(err,test)})
with (OUT/'automated-tests.txt').open('w') as stream:
 result=unittest.TextTestRunner(stream=stream,verbosity=2,resultclass=Recorded).run(unittest.defaultTestLoader.loadTestsFromTestCase(mod.WorkflowTests))
(OUT/'results.json').write_text(json.dumps({'tests_run':result.testsRun,'passed':len([x for x in result.rows if x['status']=='pass']),'tests':result.rows},indent=2)+'\n')
if not result.wasSuccessful():raise SystemExit('Tests failed. See automated-tests.txt')

def save_scenario(case,name,label):
 dest=OUT/'scenarios'/name;dest.mkdir(parents=True,exist_ok=True)
 for folder in ('Current','Shopify'):shutil.copytree(case.root/folder,dest/folder,dirs_exist_ok=True)
 for filename in ('Understanding.txt','Questions.txt','Decisions.csv'):shutil.copy2(case.root/filename,dest/filename)
 content=(case.root/'Report.html').read_text().replace('<body><main>','<body><main><div class="notice"><strong>TEST SCENARIO — '+label+'</strong><br>Synthetic analyst answers; these are not confirmed rules for the real interface.</div>',1)
 (dest/'Report.html').write_text(content)

case=mod.WorkflowTests('test_02_basic_run');case.setUp()
try:
 case.prepare();case.synthetic_analysis();case.generate();save_scenario(case,'01-basic','First mapping run')
 case.test_13_rerun_after_clarification();case.test_37_ready_field_with_business_confirmation()
 def polish(i,a):
  a['target_candidates'][0]['state']='selected'
  for r in a['target_requirements']:
   if r['field']=='input.name':r.update(status='ready',note='Use available, as confirmed by the test analyst.')
   if r['field']=='input.quantities[].quantity':r.update(status='ready',note='Use the source absolute sellable quantity for the agreed location; preserve zero.')
  a['summary']='TEST SCENARIO: quantity mapping confirmed; SKUType excluded. Identity and location lookups still prevent an executable integration.'
  a['changes']=['Reviewed explicit test-analyst clarifications. AvailableQuantity is ready at field level. SKUType is excluded. Interface gates remain open.']
  for q in a['questions']:
   if q['id'] in ('Q01','Q03','Q04'):q.update(review_status='resolved',resolution_note='Reviewed synthetic analyst answer for this scenario.')
 case.synthetic_analysis(polish);case.generate();save_scenario(case,'02-clarified','Clarifications reviewed')
 case.run_script('prepare_run.py','--answer','Q04','Correction: this is an aggregate across three warehouses; allocation is unknown.','--decision','AvailableQuantity','Reopen: warehouse allocation must be agreed.','New clarification contradicts the earlier single-location assumption.','')
 def reopen(i,a):
  i['target']['selected_operation']=None
  a['target_candidates'][0]['state']='candidate'
  for r in a['target_requirements']:
   if r['field']=='input.quantities[].quantity':r.update(status='needs_input',note='Warehouse allocation is unresolved after the analyst correction.')
  m=next(m for m in a['mappings'] if m['source_field']=='AvailableQuantity')
  m.update(status='needs_input',decision_basis='proposal',confirmed_by=None,reason='The new answer says quantity is aggregated across warehouses; allocation is unresolved.',proposed_rule='Hold the quantity mapping until the source-to-location allocation is confirmed.')
  m['evidence_ids']=[e for e in m['evidence_ids'] if e!='decision:AvailableQuantity']
  a['evidence']=[e for e in a['evidence'] if e['id']!='decision:AvailableQuantity']
  q=next(q for q in a['questions'] if q['id']=='Q04');q.update(review_status='needs_clarification',resolution_note='Earlier single-location confirmation was superseded.')
  a['summary']='TEST SCENARIO: a contradictory clarification reopens the quantity mapping. The unrelated SKUType exclusion is preserved.'
  a['changes']=['Reopened AvailableQuantity after the analyst changed the location scope from one location to a three-warehouse aggregate. Preserved other answers and the SKUType decision.']
 case.synthetic_analysis(reopen);case.generate();save_scenario(case,'03-reopened','Decision reopened after correction')
finally:case.tearDown()
print(f'{result.testsRun} automated tests passed; three scenario reports generated.')
