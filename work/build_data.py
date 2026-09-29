from pathlib import Path
from datetime import datetime, timezone
import json, hashlib, xml.etree.ElementTree as ET

ROOT = Path('/Users/pramodp/Documents/Codex/2026-09-29/basi/outputs/availability-poc')
def save(name, value):
    p = ROOT / name
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(value, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')

records = [('LV4000000',2),('LV4000001',10),('LV4000002',0),('LV4000003',3),('LV3000000',5),('LV3000001',6),('LV3000002',13),('KB5038140',21),('K03015680',15)]
sample = '<WebItemAvailability>\n<result>\n' + '\n'.join(f'''<WebAvailabilityData>
<AvailableDate>{'2049-12-31' if q == 0 else '2026-09-28'}T00:00:00</AvailableDate>
{'<AvailableDateRange>Not Available</AvailableDateRange>' if q == 0 else '<AvailableDateRange />'}
<AvailableQuantity>{q}</AvailableQuantity>
<InStock>{'true' if q else 'false'}</InStock>
<SKU>{sku}</SKU>
<SKUType>N</SKUType>
<WebProductId>{sku}</WebProductId>
</WebAvailabilityData>''' for sku,q in records)
(ROOT/'source/availability-excerpt.xml').write_text(sample+'\n',encoding='utf-8')
(ROOT/'source/availability-normalized.xml').write_text(sample+'\n</result>\n</WebItemAvailability>\n',encoding='utf-8')
ns={'x':'http://www.w3.org/2001/XMLSchema'}
root=ET.parse(ROOT/'source/checkout-1.wsdl')
fields=[]
for e in root.findall('.//x:complexType[@name="WebAvailabilityItem"]/x:sequence/x:element',ns):
    fields.append({'name':e.get('name'),'type':e.get('type'),'optional':e.get('minOccurs')=='0','nullable':e.get('nillable')=='true'})
save('source/fields.json',fields)

questions=[
('Q01','Start here','What should Shopify do with this feed?','Update sellable stock, display delivery/availability information, or both?','Determines whether an inventory write is the right target.',['AvailableQuantity','InStock','AvailableDate','AvailableDateRange']),
('Q02','Source','Where is this XML produced?','ERP, middleware, Sitecore, or an export? Please also provide a complete response if available.','The sample wrapper differs from the WSDL and is missing two closing tags.',['*']),
('Q03','Start here','What does AvailableQuantity represent?','Is this an absolute total or a change? Sellable stock or physical stock? Which system owns it?','An absolute inventory set is suitable only if the source owns the inventory total.',['AvailableQuantity']),
('Q04','Start here','Which location does the quantity belong to?','One warehouse/location, a combined total, or separate values elsewhere?','Shopify inventory writes need a location ID; none is supplied.',['AvailableQuantity']),
('Q05','Identity','How do these identifiers match Shopify?','Does SKU match the Shopify SKU? Can duplicates exist? What does WebProductId identify?','Resolve actual inventory IDs; do not turn an ERP identifier into a Shopify ID.',['SKU','WebProductId']),
('Q06','Dates','What do the availability dates mean?','Meaning of 2049-12-31, timezone, empty date range, and whether customers should see these values.','A possible sentinel date must not become a real delivery promise.',['AvailableDate','AvailableDateRange']),
('Q07','Availability','What controls InStock?','Is it derived from quantity, or does it include backorders, fulfilment or other rules?','A source flag is not automatically equivalent to Shopify availability for sale.',['InStock']),
('Q08','Product types','What does SKUType mean?','Please explain N, other possible values and any special processing.','Product type rules could change how identity and inventory should be handled.',['SKUType']),
('Q09','Operation','How is the feed delivered?','Schedule or event? Full snapshot or partial feed? What should happen to missing, duplicate, late or failed records?','Defines processing rules beyond individual field assignments.',['*'])]

inp={'schema_version':'1.0','interface':{'id':'web-item-availability','name':'GetWebItemAvailability','description':'','current_flow':'ERP → middleware → Sitecore','target_flow':'ERP → middleware → Shopify','source_origin':'unknown','reviewer':''},
 'source':{'contracts':['source/checkout-1.wsdl','source/checkout-2.wsdl'],'sample':'source/availability-excerpt.xml','normalized_sample':'source/availability-normalized.xml','fields':fields,'normalization_note':'The pasted excerpt lacks </result> and </WebItemAvailability>. Only these closing tags were added in the normalized copy. The excerpt is transcribed from the chat; whitespace may differ.'},
 'target':{'platform':'Shopify','api_version':'2026-07','research_method':'official_public_documentation','selected_operation':None},'known_rules':[], 'answers':{q[0]:'' for q in questions},'additional_context':''}
save('input.json',inp)

manifest=json.loads(Path('/Users/pramodp/Documents/Codex/2026-09-29/basi/work/shopify-docs/manifest.json').read_text())
evidence=[{'id':'source-contract','title':'Checkout WSDL with embedded schemas','url':'source/checkout-1.wsdl','note':'GetWebItemAvailability has no request fields. WebAvailabilityItem defines all seven sample fields; each is optional.'},
{'id':'source-sample','title':'User-provided availability excerpt','url':'source/availability-excerpt.xml','note':'Nine records: eight positive quantities and one zero. SKU and WebProductId match in these nine examples only.'}]
notes={
'set-quantities':('Inventory write','Sets absolute quantities. Requires write_inventory and inventory update permission. Use for an authoritative stock source. Version 2026-07 requires an idempotency key.'),
'set-input':('Inventory set input','name, reason and quantities are required. name accepts available or on_hand. referenceDocumentUri is optional.'),
'quantity-input':('Per-item quantity input','inventoryItemId, locationId and quantity are non-null. changeFromQuantity must be explicitly supplied, even though its type is nullable. Prefer a current quantity value for concurrency checking.'),
'items':('Inventory item lookup','inventoryItems supports filtering by SKU. Search results must be checked for exact matching and ambiguity.'),
'locations':('Inventory locations','locations provides location records. Business rules must identify the correct destination.'),
'storefront-variant':('Customer-facing availability','ProductVariant exposes availableForSale and quantityAvailable as read fields. These are not writable destinations for this source feed.'),
'metafields':('Custom field alternative','metafieldsSet can store custom data. Owner, namespace, key, type and business meaning would need a separate design.'),
'activation':('Inventory activation','inventoryActivate establishes an inventory item at a location. Existing activation must be checked before quantity updates.')}
for m in manifest:
    title,note=notes[m['id']]
    evidence.append({**m,'title':title,'note':note,'api_version':'2026-07','acquisition':'official public Markdown documentation'})

def mapping(field,status,target,reason,rule,qs,refs):
    f=next(f for f in fields if f['name']==field)
    return {'source_field':field,'source_type':f['type'],'status':status,'target':target,'reason':reason,'proposed_rule':rule,'question_ids':qs,'evidence_ids':['source-contract','source-sample']+refs,'decision_basis':'proposal','confirmed_by':None}
maps=[
mapping('AvailableDate','needs_input','No direct field in inventorySetQuantities','Meaning, timezone and customer display requirements are unknown.','Preserve raw value. Consider a custom field only after agreeing its meaning; do not treat 2049-12-31 as a confirmed delivery date.',['Q01','Q06'],['quantity-input','metafields']),
mapping('AvailableDateRange','needs_input','No direct field in inventorySetQuantities','An empty element and Not Available are observed; their rules are unknown.','Preserve the difference between empty, absent and populated values. Custom display data is a candidate only.',['Q01','Q06'],['quantity-input','metafields']),
mapping('AvailableQuantity','needs_input','inventorySetQuantities.input.quantities[].quantity (candidate)','The source quantity meaning, authority and location scope have not been established.','If confirmed as an authoritative absolute sellable total for a mapped location, propose name=available and use the integer value. Zero remains zero; missing is not zero.',['Q01','Q03','Q04'],['set-quantities','set-input','quantity-input']),
mapping('InStock','unmapped','No direct writable field in the selected candidate','The inventory mutation has no InStock input. Storefront availableForSale is a read result, not a write mapping.','Retain for rule analysis; do not map to a read-only availability field or silently discard.',['Q01','Q07'],['quantity-input','storefront-variant']),
mapping('SKU','needs_lookup','inventoryItems(query: sku:...) → inventoryItemId (candidate)','Requires a confirmed identity rule and a unique match in the actual Shopify store.','Keep as text. Resolve the Shopify inventory item ID; reject missing or ambiguous matches. Do not select the first match blindly.',['Q05','Q08'],['items','quantity-input']),
mapping('SKUType','needs_input','Undecided','Only N appears in this sample; its meaning and other values are unknown.','No product-type translation, filter or exclusion until the code list and rules are confirmed.',['Q08'],['quantity-input']),
mapping('WebProductId','needs_input','Identity cross-reference or custom field (candidate)','It equals SKU in this sample; that does not establish a general identity rule.','Keep as text. Do not substitute this value for a Shopify product, variant or inventory ID.',['Q05'],['items','metafields'])]

target_fields=[
('input.name','String!','required','needs_input','available or on_hand; choose after confirming quantity meaning.',['Q03'],['set-input']),
('input.quantities[].quantity','Int!','required','needs_input','Candidate source: AvailableQuantity.',['Q01','Q03'],['quantity-input']),
('input.quantities[].inventoryItemId','ID!','required','needs_lookup','Resolve using confirmed identity rules and actual store data.',['Q05'],['items','quantity-input']),
('input.quantities[].locationId','ID!','required','needs_lookup','Location mapping is absent. Combined stock must not be copied into each warehouse.',['Q04'],['locations','quantity-input']),
('input.quantities[].changeFromQuantity','Int','explicit value required','needs_lookup','Read the expected current quantity. An explicit null skips the concurrency check and needs an agreed policy.',['Q03','Q09'],['quantity-input']),
('input.reason','String!','required','needs_input','Choose the documented reason matching the agreed business event.',['Q03','Q09'],['set-input']),
('@idempotent(key)','String!','required in 2026-07','needs_input','Define one key per logical request and reuse it on retries.',['Q09'],['set-quantities']),
('input.referenceDocumentUri','String','optional','needs_input','Consider a source document or batch reference for traceability.',['Q09'],['set-input'])]

analysis={'schema_version':'1.0','interface_id':inp['interface']['id'],'revision':1,'input_sha256':hashlib.sha256(json.dumps(inp,sort_keys=True,ensure_ascii=False,separators=(',',':')).encode()).hexdigest(),'generated_at':datetime.now(timezone.utc).isoformat(),
'summary':'All seven source fields have been assessed. No end-to-end mapping is confirmed yet. Start with intended Shopify behavior, quantity meaning and location scope.',
'target_candidates':[
{'name':'Inventory synchronization','operation':'inventorySetQuantities','state':'candidate','when':'If the feed owns absolute stock totals and is intended to update inventory.','evidence_ids':['set-quantities','set-input','quantity-input']},
{'name':'Availability display','operation':'Storefront ProductVariant / optional metafieldsSet','state':'alternative','when':'If this interface supplies customer-facing availability. Storefront fields can be read; custom date/message storage needs a separate design.','evidence_ids':['storefront-variant','metafields']}],
'observations':[
{'kind':'observed','text':'The embedded WSDL schema defines seven optional fields. Dates are dateTime, quantity is int, InStock is boolean, and the remaining fields are strings.','evidence_ids':['source-contract']},
{'kind':'observed','text':'Nine records: eight in stock and one out of stock. The zero-stock record has 2049-12-31 and Not Available. This co-occurrence does not establish a business rule.','evidence_ids':['source-sample']},
{'kind':'data_issue','text':'The pasted excerpt is incomplete XML. A separate normalized copy adds only the two missing closing tags.','evidence_ids':['source-sample']},
{'kind':'data_issue','text':'The sample repeats WebAvailabilityData directly. The WSDL uses GetWebItemAvailabilityResponse → GetWebItemAvailabilityResult → WebAvailabilityData → WebAvailabilityItem. Origin or transformation is unknown.','evidence_ids':['source-contract','source-sample']},
{'kind':'research_note','text':'Shopify mutation examples/prose include legacy compareQuantity wording. The 2026-07 input reference specifies changeFromQuantity. This POC follows that input reference; no live schema validation has run.','evidence_ids':['set-quantities','quantity-input']}],
'mappings':maps,
'questions':[{'id':id,'group':group,'question':q,'hint':hint,'why':why,'affected_fields':affected} for id,group,q,hint,why,affected in questions],
'target_requirements':[{'field':f,'type':t,'requirement':req,'status':s,'note':n,'question_ids':qs,'evidence_ids':refs} for f,t,req,s,n,qs,refs in target_fields],
'interface_gates':[{'id':'G01','description':'Choose stock synchronization, availability display, or both.','status':'open','question_ids':['Q01']},{'id':'G02','description':'Confirm source contract and payload origin.','status':'open','question_ids':['Q02']},{'id':'G03','description':'Confirm identity, quantity ownership, locations and product-type rules.','status':'open','question_ids':['Q03','Q04','Q05','Q08']},{'id':'G04','description':'Define scheduling, retries, partial failures and stale-message handling.','status':'open','question_ids':['Q09']},{'id':'G05','description':'Validate schema, permissions, inventory tracking and location activation in a Shopify test store.','status':'open','question_ids':[]}],
'evidence':evidence,'validation_scenarios':[
{'scenario':'Positive and zero quantities','expected':'Keep zero as an explicit value. Do not infer confirmed stock semantics from the sample.'},
{'scenario':'Missing optional quantity','expected':'Flag a missing value; never silently set stock to zero.'},
{'scenario':'Unknown or duplicate SKU','expected':'Require resolution; do not invent an ID or use an arbitrary search result.'},
{'scenario':'Unknown location or aggregated quantity','expected':'Hold the proposed write until allocation is defined.'},
{'scenario':'2049-12-31 or absent date range','expected':'Keep original values and request the rule; do not publish an inferred delivery promise.'},
{'scenario':'Retry, concurrency conflict or older source message','expected':'Apply the agreed idempotency and ordering rules. Those rules remain unresolved.'}],
'changes':['Initial analysis from two supplied WSDLs, nine sample records and official Shopify documentation.'],
'limitations':['AI-assisted draft; business decisions are unconfirmed.','Public documentation research only. No Shopify MCP or live store connection was used.','The report collects answers but does not run an AI model. Re-analysis is a separate assistant step.']}
save('analysis.json',analysis)

# Portable schemas: the bundled validator implements the keywords used here.
def obj(props,required=None): return {'type':'object','properties':props,'required':list(props) if required is None else required,'additionalProperties':False}
S={'type':'string'}; NS={'type':['string','null']}; A=lambda items:{'type':'array','items':items}
source_field=obj({'name':S,'type':S,'optional':{'type':'boolean'},'nullable':{'type':'boolean'}})
input_schema=obj({'schema_version':{'const':'1.0'},'interface':obj({k:S for k in inp['interface']}),'source':obj({'contracts':A(S),'sample':S,'normalized_sample':S,'fields':A(source_field),'normalization_note':S}),'target':obj({'platform':S,'api_version':S,'research_method':S,'selected_operation':NS}),'known_rules':A(obj({'id':S,'statement':S,'confirmed_by':S})),'answers':{'type':'object','additionalProperties':S},'additional_context':S})
status={'enum':['ready','needs_input','needs_lookup','unmapped','excluded']}
analysis_schema=obj({'schema_version':{'const':'1.0'},'interface_id':S,'revision':{'type':'integer','minimum':1},'input_sha256':S,'generated_at':S,'summary':S,
'target_candidates':A(obj({'name':S,'operation':S,'state':{'enum':['candidate','alternative','selected']},'when':S,'evidence_ids':A(S)})),
'observations':A(obj({'kind':S,'text':S,'evidence_ids':A(S)})),
'mappings':A(obj({'source_field':S,'source_type':S,'status':status,'target':S,'reason':S,'proposed_rule':S,'question_ids':A(S),'evidence_ids':A(S),'decision_basis':{'enum':['proposal','confirmed']},'confirmed_by':NS})),
'questions':A(obj({'id':S,'group':S,'question':S,'hint':S,'why':S,'affected_fields':A(S)})),
'target_requirements':A(obj({'field':S,'type':S,'requirement':S,'status':status,'note':S,'question_ids':A(S),'evidence_ids':A(S)})),
'interface_gates':A(obj({'id':S,'description':S,'status':{'enum':['open','closed']},'question_ids':A(S)})),
'evidence':A(obj({'id':S,'title':S,'url':S,'note':S,'retrieved_on':S,'sha256':S,'bytes':{'type':'integer'},'api_version':S,'acquisition':S},['id','title','url','note'])),
'validation_scenarios':A(obj({'scenario':S,'expected':S})),'changes':A(S),'limitations':A(S)})
for n,sch in [('input',input_schema),('analysis',analysis_schema)]:
    save('schemas/'+n+'.schema.json',{'$schema':'https://json-schema.org/draft/2020-12/schema','title':n.title()+' contract',**sch})
template=json.loads(json.dumps(inp));template['interface'].update(id='new-interface',name='',description='');template['source'].update(contracts=[],sample='',normalized_sample='',fields=[],normalization_note='');template['answers']={}
save('templates/input.json',template)
print('Created source evidence, structured input, seven-field analysis and JSON contracts.')
