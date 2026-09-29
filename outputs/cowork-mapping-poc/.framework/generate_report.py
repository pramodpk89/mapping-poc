"""Guarded HTML generation after Cowork completes mapping analysis."""
import json
import sys
from datetime import datetime,timezone
from preflight import ROOT,FW,check,write_notice
import renderer

def main():
    status='blocked'
    try:
        checked=check()
        if checked['missing']:
            write_notice('Required input is missing',checked['missing']);print('BLOCKED: see Report.html');return 2
        inp=json.loads((FW/'input.json').read_text(encoding='utf-8-sig'))
        analysis=json.loads((FW/'analysis.json').read_text(encoding='utf-8-sig'))
        if inp.get('human_inputs_sha256')!=checked['human_inputs_sha256'] or analysis.get('input_sha256')!=renderer.fingerprint(inp):
            write_notice('Cowork needs to analyse the updated files',['Required inputs passed, but the current analysis does not correspond to these files. Ask Cowork to continue RUN.md and review the new input.']);print('NEEDS ANALYSIS: see Report.html');return 3
        renderer.validate_pack(inp,analysis)
        if renderer.render(inp,analysis,ROOT/'Report.html'):
            write_notice('Source evidence changed',['Cowork must review the changed source evidence and refresh the analysis before generating a mapping report.']);return 3
        status='generated';print('Generated Report.html');return 0
    except (OSError,ValueError,TypeError,KeyError) as e:
        write_notice('The report needs a correction',[str(e),'Cowork should correct the analysis or configuration and rerun the checked generator.']);print('BLOCKED:',e);return 2
    finally:
        log={'time':datetime.now(timezone.utc).isoformat(),'status':status,'configured_execution_host':'claude_cowork','actual_execution_host':'unknown','actual_model':'unknown'}
        with (FW/'run-history.jsonl').open('a',encoding='utf-8') as f:f.write(json.dumps(log)+'\n')

if __name__=='__main__':sys.exit(main())
