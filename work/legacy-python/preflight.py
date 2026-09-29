"""Check analyst input before an AI mapping run. Standard-library Python 3.9+."""
from pathlib import Path
from urllib.parse import urlparse
from datetime import datetime, timezone
import csv
import hashlib
import html
import json
import sys
import xml.etree.ElementTree as ET
from analyst_io import read_text, understanding, unknown, decisions, confirmed_rules, questions

ROOT=Path(__file__).resolve().parent.parent
FW=ROOT/'.framework'

def digest(value):
    return hashlib.sha256(json.dumps(value,sort_keys=True,ensure_ascii=False,separators=(',',':')).encode()).hexdigest()

def write_notice(title,items):
    content=''.join('<li>'+html.escape(x)+'</li>' for x in items)
    page='''<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Mapping input check</title><style>body{max-width:800px;margin:60px auto;padding:0 24px;background:#f7f8f3;color:#173a30;font:17px/1.65 system-ui}h1{font-size:30px}li{margin:14px 0}.note{padding:18px;background:#fff0cb;border-left:4px solid #ba881e}code{font-size:15px}</style></head><body>'''
    page+='<p>INTERFACE MAPPING · INPUT CHECK</p><h1>'+html.escape(title)+'</h1><div class="note"><ul>'+content+'</ul></div><p>Tell Copilot the missing details in chat, then run <strong>/map-interface</strong> again. Copilot will save the clarification for you. You can also edit the indicated files.</p><p>No new mapping analysis was generated.</p>'
    if (ROOT/'Last-review.html').exists():page+='<p><a href="Last-review.html">Open the last completed review (may be outdated)</a></p>'
    page+='</body></html>'
    (ROOT/'Report.html').write_text(page,encoding='utf-8')

def check():
    rules=json.loads(read_text(FW/'Required-inputs.json'))
    for key in ('require_description','require_parseable_source_contract_or_payload','require_official_shopify_reference'):
        if type(rules.get(key)) is not bool:raise ValueError('Framework configuration needs a boolean for '+key)
    missing=[];warnings=[]
    path=ROOT/'Understanding.txt'
    fields=understanding(path)
    purpose=fields.get('what this interface does','')
    if rules['require_description'] and unknown(purpose):
        missing.append('Understanding.txt: replace the placeholder after “What this interface does:” with the business purpose. One sentence is enough.')
    if unknown(fields.get('interface name','')):missing.append('Understanding.txt: provide the interface name.')
    usable=[]
    for p in sorted((ROOT/'Current/Source').rglob('*')):
        if not p.is_file() or p.name=='availability-normalized.xml':continue
        try:
            if p.suffix.lower() in ('.xml','.wsdl','.xsd'):
                raw=p.read_bytes()
                if b'<!DOCTYPE' in raw.upper() or b'<!ENTITY' in raw.upper():
                    warnings.append(p.name+': DTD/entity declarations are not supported; provide an ordinary payload or schema.');continue
                root=ET.fromstring(raw)
                if root.tag.endswith('definitions') and not root.findall('.//{http://www.w3.org/2001/XMLSchema}element'):
                    warnings.append(p.name+': imports external schemas; not used as the sole parseable contract.');continue
                if not list(root) and not (root.text or '').strip():
                    warnings.append(p.name+': empty document is not source evidence.');continue
                usable.append(str(p.relative_to(ROOT)))
            elif p.suffix.lower()=='.json':
                value=json.loads(read_text(p))
                if isinstance(value,(dict,list)) and value:usable.append(str(p.relative_to(ROOT)))
        except (ValueError,ET.ParseError,OSError) as e:
            warnings.append(p.name+': incomplete or invalid structured file; kept as supplementary evidence.')
    if rules['require_parseable_source_contract_or_payload'] and not usable:
        missing.append('Current/Source: add a readable WSDL with embedded schemas, XSD, complete XML payload or nonempty JSON payload. A notes file or repaired sample alone does not satisfy this check.')
    target_path=ROOT/'Shopify/Target-reference.json'
    try:
        t=json.loads(read_text(target_path))
        good=t.get('platform')=='Shopify' and bool(t.get('api_version')) and bool(t.get('candidates')) and any(urlparse(e.get('url','')).scheme=='https' and urlparse(e.get('url','')).hostname=='shopify.dev' for e in t.get('evidence',[]))
    except (OSError,ValueError,TypeError,AttributeError):good=False
    if rules['require_official_shopify_reference'] and not good:
        missing.append('Shopify/Target-reference.json: Copilot must obtain official Shopify target documentation, with an API version and candidate operations, before analysis.')
    try:
        rows=decisions(ROOT/'Decisions.csv');_,decision_warnings=confirmed_rules(rows);warnings.extend(decision_warnings)
        questions(ROOT/'Questions.txt')
    except ValueError as e:missing.append(str(e))
    paths=[ROOT/'Understanding.txt',ROOT/'Questions.txt',ROOT/'Decisions.csv',FW/'Required-inputs.json']
    for folder in ['Current','Shopify']:paths.extend(p for p in (ROOT/folder).rglob('*') if p.is_file())
    outside=[p for p in paths if p.exists() and not p.resolve().is_relative_to(ROOT)]
    if outside:raise ValueError('An input file points outside this interface folder. Copy the evidence into the folder.')
    hashes={str(p.relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(set(paths)) if p.is_file()}
    result={'status':'blocked' if missing else 'pass','checked_at':datetime.now(timezone.utc).isoformat(),'missing':missing,'warnings':warnings,'usable_source_files':usable,'human_inputs_sha256':digest(hashes)}
    (FW/'preflight.json').write_text(json.dumps(result,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
    return result

def main():
    try:result=check()
    except (OSError,ValueError,KeyError,TypeError,csv.Error) as e:
        write_notice('Cannot run the input checks',[str(e)]);print('Input checks failed:',e);return 2
    if result['missing']:
        write_notice('One more detail before we run' if len(result['missing'])==1 else 'Add these details before we run',result['missing'])
        print('\n'.join(result['missing']));return 2
    print('Required inputs passed. Copilot can now perform mapping analysis.')
    return 0

if __name__=='__main__':sys.exit(main())
