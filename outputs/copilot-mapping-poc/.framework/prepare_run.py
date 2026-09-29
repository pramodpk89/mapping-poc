"""Record chat clarifications and assemble analyst input. Run by Copilot, not the analyst."""
import argparse
import copy
import json
import shutil
import sys
from datetime import datetime,timezone
import xml.etree.ElementTree as ET
from analyst_io import read_text,write_json,understanding,set_understanding,questions,set_answers,decisions,write_decisions,confirmed_rules
from preflight import ROOT,FW,check,write_notice

def assemble(previous,analysis,checked):
    data=copy.deepcopy(previous);text=understanding(ROOT/'Understanding.txt')
    for source,target in [('interface name','name'),('what this interface does','description'),('current flow','current_flow'),('target flow','target_flow'),('payload origin','source_origin'),('reviewer','reviewer')]:
        if source in text:data['interface'][target]=text[source]
    data['additional_context']=text.get('additional context','')
    parsed=questions(ROOT/'Questions.txt')
    data['answers']={q['id']:parsed.get(q['id'],'') for q in analysis['questions']}
    data['answers'].update(parsed)
    data['known_rules'],_=confirmed_rules(decisions(ROOT/'Decisions.csv'))
    data['human_inputs_sha256']=checked['human_inputs_sha256']
    # This first POC knows the WebAvailabilityItem contract; do not silently reuse old fields.
    matching=[]
    for path in (ROOT/'Current/Source').glob('*.wsdl'):
        try:
            root=ET.fromstring(path.read_bytes())
            elements=root.findall('.//{http://www.w3.org/2001/XMLSchema}complexType[@name="WebAvailabilityItem"]/{http://www.w3.org/2001/XMLSchema}sequence/{http://www.w3.org/2001/XMLSchema}element')
            if elements:matching.append([{'name':e.get('name'),'type':e.get('type'),'optional':e.get('minOccurs')=='0','nullable':e.get('nillable')=='true'} for e in elements])
        except (ET.ParseError,OSError):continue
    if matching:
        if any(f!=matching[0] for f in matching[1:]):raise ValueError('Source contracts disagree about WebAvailabilityItem. Clarify which contract applies.')
        data['source']['fields']=matching[0]
    return data

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--purpose');p.add_argument('--source-origin');p.add_argument('--reviewer');p.add_argument('--context')
    p.add_argument('--answer',nargs=2,action='append',default=[],metavar=('QUESTION','TEXT'))
    p.add_argument('--decision',nargs=4,action='append',default=[],metavar=('ATTRIBUTE','DECISION','EXPLANATION','CONFIRMED_BY'))
    p.add_argument('--import-answers',type=str)
    args=p.parse_args()
    try:
        old=json.loads(read_text(FW/'input.json'));analysis=json.loads(read_text(FW/'analysis.json'))
        updates={};answers=dict(args.answer);rows=decisions(ROOT/'Decisions.csv')
        known={q['id'] for q in analysis['questions']}
        if set(answers)-known:raise ValueError('Unknown question ID: '+', '.join(sorted(set(answers)-known)))
        if args.import_answers:
            if args.purpose is not None or args.answer or args.decision or args.context is not None:raise ValueError('Import saved answers separately from other edits.')
            value=json.loads(read_text(args.import_answers))
            import renderer
            renderer.validate_schema(value,json.loads(read_text(FW/'schemas/input.schema.json')))
            if value['interface']['id']!=old['interface']['id']:raise ValueError('These saved answers belong to another interface.')
            if value['human_inputs_sha256']!=check()['human_inputs_sha256']:raise ValueError('Files changed since these answers were saved. Ask Copilot to reconcile the answers with the current notes; no files were overwritten.')
            if value['source']!=old['source'] or value['target']!=old['target'] or value['known_rules']!=old['known_rules']:raise ValueError('The answer file changes source, target or confirmed rules. Review those changes separately.')
            if set(value['answers'])-known:raise ValueError('Saved answers contain unknown question IDs.')
            for k,t in [('description','What this interface does'),('source_origin','Payload origin'),('reviewer','Reviewer')]:updates[t]=value['interface'][k]
            updates['Additional context']=value['additional_context'];answers=value['answers']
        for value,label in [(args.purpose,'What this interface does'),(args.source_origin,'Payload origin'),(args.reviewer,'Reviewer')]:
            if value is not None:updates[label]=value
        if args.context is not None:
            prior=understanding(ROOT/'Understanding.txt').get('additional context','')
            updates['Additional context']=(prior+'\n'+args.context).strip()
        for attr,decision,explanation,confirmer in args.decision:
            if attr not in {x['name'] for x in old['source']['fields']}:raise ValueError('Unknown source attribute '+attr)
            rows=[r for r in rows if r['Attribute']!=attr]
            rows.append({'Attribute':attr,'Decision':decision,'Explanation':explanation,'Confirmed by':confirmer})
        if updates or answers or args.decision:
            stamp=datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
            archive=FW/'history'/'input-edits'/stamp;archive.mkdir(parents=True)
            for name in ['Understanding.txt','Questions.txt','Decisions.csv']:
                if (ROOT/name).exists():shutil.copy2(ROOT/name,archive/name)
            if updates:set_understanding(ROOT/'Understanding.txt',updates)
            if answers:set_answers(ROOT/'Questions.txt',answers,analysis['questions'])
            if args.decision:write_decisions(ROOT/'Decisions.csv',rows)
        checked=check()
        if checked['missing']:
            write_notice('Add these details before we run',checked['missing']);print('\n'.join(checked['missing']));return 2
        data=assemble(old,analysis,checked)
        write_json(FW/'input.json',data)
        print('Inputs prepared. Copilot should now review the evidence and update the analysis.')
        return 0
    except (OSError,ValueError,KeyError,TypeError) as e:
        write_notice('Input needs attention',[str(e)]);print(str(e));return 2

if __name__=='__main__':sys.exit(main())
