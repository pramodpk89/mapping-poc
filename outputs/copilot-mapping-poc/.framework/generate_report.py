"""Guarded HTML generation after Copilot completes mapping analysis."""
import json
import sys
import shutil
from datetime import datetime,timezone
from preflight import ROOT,FW,check,write_notice
import renderer
from prepare_run import assemble

def main():
    status='blocked'
    try:
        checked=check()
        if checked['missing']:
            write_notice('Required input is missing',checked['missing']);print('BLOCKED: see Report.html');return 2
        inp=json.loads((FW/'input.json').read_text(encoding='utf-8-sig'))
        analysis=json.loads((FW/'analysis.json').read_text(encoding='utf-8-sig'))
        if inp.get('human_inputs_sha256')!=checked['human_inputs_sha256'] or analysis.get('input_sha256')!=renderer.fingerprint(inp):
            status='needs_analysis'
            write_notice('Copilot needs to analyse the updated files',['Required inputs passed, but the current analysis does not correspond to these files. Ask Copilot to continue RUN.md and review the new input.']);print('NEEDS ANALYSIS: see Report.html');return 3
        assembled=assemble(inp,analysis,checked)
        for key in ('interface','answers','known_rules','additional_context'):
            if inp[key]!=assembled[key]:raise ValueError('Analyst '+key+' changed or was not imported. Run prepare_run.py and review the resulting input.')
        if inp['source']['fields']!=assembled['source']['fields']:raise ValueError('Source fields changed; prepare and analyse the current contract.')
        renderer.validate_pack(inp,analysis)
        prior_files=sorted((FW/'history').glob('*/analysis.json')) if (FW/'history').exists() else []
        if prior_files:
            prior=json.loads(prior_files[-1].read_text(encoding='utf-8'))
            old_rows={m['source_field']:m for m in prior['mappings']}
            for row in analysis['mappings']:
                old=old_rows.get(row['source_field'])
                if old and old['status']!=row['status'] and old['reason'].strip()==row['reason'].strip():
                    raise ValueError(row['source_field']+': status changed but the explanation is unchanged. Review and update the reason.')
        if renderer.render(inp,analysis,ROOT/'Report.html',warnings=checked['warnings']):
            status='needs_analysis'
            write_notice('Source evidence changed',['Copilot must review the changed source evidence and refresh the analysis before generating a mapping report.']);return 3
        status='generated'
        archive=FW/'history'/datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
        archive.mkdir(parents=True)
        for filename in ('input.json','analysis.json'):shutil.copy2(FW/filename,archive/filename)
        saved=(ROOT/'Report.html').read_text(encoding='utf-8').replace('<body><main>','<body><main><div class="notice">Saved review — this may predate your latest clarifications. Run /map-interface for the current result.</div>',1)
        (ROOT/'Last-review.html').write_text(saved,encoding='utf-8')
        print('Generated Report.html');return 0
    except (OSError,ValueError,TypeError,KeyError) as e:
        write_notice('The report needs a correction',[str(e),'Copilot should correct the analysis or configuration and rerun the checked generator.']);print('BLOCKED:',e);return 2
    finally:
        log={'time':datetime.now(timezone.utc).isoformat(),'status':status,'configured_execution_host':'github_copilot_vscode','actual_execution_host':'unknown','actual_model':'unknown'}
        with (FW/'run-history.jsonl').open('a',encoding='utf-8') as f:f.write(json.dumps(log)+'\n')

if __name__=='__main__':sys.exit(main())
