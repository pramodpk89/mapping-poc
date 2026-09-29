from pathlib import Path
import shutil,json,csv,hashlib
base=Path('/Users/pramodp/Documents/Codex/2026-09-29/basi/outputs')
old=base/'availability-poc';new=base/'cowork-mapping-poc';fw=new/'.framework'
for name in ['checkout-1.wsdl','checkout-2.wsdl','availability-excerpt.xml','availability-normalized.xml']:
 shutil.copy2(old/'source'/name,new/'Current/Source'/name)
for name in ['input.schema.json','analysis.schema.json']:
 shutil.copy2(old/'schemas'/name,fw/'schemas'/name)
shutil.copy2(old/'render.py',fw/'renderer.py')
shutil.copy2(old/'report-template.html',fw/'report-template.html')
a=json.loads((old/'analysis.json').read_text());i=json.loads((old/'input.json').read_text())
for e in a['evidence']:
 if e['url'].startswith('source/'):e['url']=e['url'].replace('source/','Current/Source/',1)
i['source']['contracts']=[x.replace('source/','Current/Source/',1) for x in i['source']['contracts']]
for key in ['sample','normalized_sample']:i['source'][key]=i['source'][key].replace('source/','Current/Source/',1)
i['human_inputs_sha256']='not-yet-analyzed'
a['input_sha256']='not-yet-analyzed'
for name,data in [('input',i),('analysis',a)]:
 (fw/(name+'.json')).write_text(json.dumps(data,indent=2,ensure_ascii=False)+'\n')
schema=json.loads((fw/'schemas/input.schema.json').read_text());schema['properties']['human_inputs_sha256']={'type':'string'};schema['required'].append('human_inputs_sha256');(fw/'schemas/input.schema.json').write_text(json.dumps(schema,indent=2)+'\n')
target={'platform':'Shopify','api_version':'2026-07','retrieved_on':'2026-09-29','method':'official_public_documentation','candidates':a['target_candidates'],'requirements':a['target_requirements'],'evidence':[e for e in a['evidence'] if e['url'].startswith('https://')]}
(new/'Shopify/Target-reference.json').write_text(json.dumps(target,indent=2,ensure_ascii=False)+'\n')
with (new/'Decisions.csv').open('w',newline='',encoding='utf-8-sig') as f:
 w=csv.writer(f);w.writerow(['Attribute','Decision','Explanation','Confirmed by'])
 for field in i['source']['fields']:w.writerow([field['name'],'','',''])
questions='QUESTIONS FOR THE ANALYST\nLeave unknown answers blank. Cowork updates the report after reviewing your answers.\n\n'
for q in a['questions']:questions+=f"{q['id']}: {q['question']}\n{q['hint']}\nAnswer:\n\n"
(new/'Questions.txt').write_text(questions,encoding='utf-8')
(new/'Understanding.txt').write_text('Interface name: GetWebItemAvailability\nWhat this interface does: [Please add one sentence describing the business purpose]\n\nCurrent flow: ERP -> middleware -> Sitecore\nTarget flow: ERP -> middleware -> Shopify\nPayload origin: Unknown\n\nAdditional context:\nAdd your current understanding, examples and corrections here. Unknown business rules can remain unknown.\n',encoding='utf-8')
(new/'Current/Source/Read-me.txt').write_text('Put source payloads, WSDLs and schemas here. The supplied files are already included.\nPayload origin is unconfirmed. availability-normalized.xml only adds missing closing tags; it is not independent evidence.\nDo not edit original evidence to make it appear complete. Add newer examples as separate files.\n',encoding='utf-8')
(new/'Current/Target/Read-me.txt').write_text('Put existing Sitecore payloads, mappings and notes here if available.\nNo confirmed Sitecore transformation was supplied for this example. Missing target details are an open question, not a reason to invent rules.\n',encoding='utf-8')
(new/'Shopify/Read-me.txt').write_text('Cowork stores official Shopify API research here. Add target requirements or store-specific notes if you know them.\nThe included research is for API version 2026-07, retrieved 2026-09-29. Inventory updates remain a candidate.\n',encoding='utf-8')
# Adapt the reused presentation to the simpler, document-led analyst workflow.
p=fw/'report-template.html';s=p.read_text();s=s.replace("/^source\\/[a-zA-Z0-9._-]+$/", "/^Current\\/(Source|Target)\\/[a-zA-Z0-9._-]+$/")
s=s.replace('Works offline. Enter answers below, download them, then give the JSON to your AI assistant for re-analysis. Typing an answer does not confirm a mapping.','Edit Understanding.txt, Decisions.csv or Questions.txt in this folder, then ask Cowork to read RUN.md and regenerate Report.html. The optional form below can also export answers for Cowork.')
s=s.replace('const pack=JSON.parse', 'const pack=JSON.parse')
p.write_text(s)
# Source paths in this edition are relative to the analyst folder, not the internal engine.
p=fw/'renderer.py';s=p.read_text();s=s.replace("if evidence['url'].startswith('source/')", "if evidence['url'].startswith('Current/')");s=s.replace("source=(BASE/evidence['url']).resolve()", "source=(BASE.parent/evidence['url']).resolve()");s=s.replace('source.is_relative_to(BASE)', 'source.is_relative_to(BASE.parent)');p.write_text(s)
settings={'execution_mode':'claude_cowork','provider':'Anthropic','model':'selected_in_cowork','credentials':'managed_by_cowork','api_key_required':False,'actual_model':'record_if_exposed_otherwise_unknown','note':'This describes the execution mode; it does not change Cowork model settings.'}
(fw/'AI-settings.json').write_text(json.dumps(settings,indent=2)+'\n')
requirements={'require_description':True,'require_parseable_source_contract_or_payload':True,'require_official_shopify_reference':True,'unknown_attribute_rules':'allow_analysis_but_block_affected_mappings','missing_current_target_details':'allow_and_flag','missing_model_name':'record_unknown','missing_python_runtime':'stop_and_explain'}
(fw/'Required-inputs.json').write_text(json.dumps(requirements,indent=2)+'\n')
print('Created Cowork folder structure and carried over source evidence and draft analysis.')
