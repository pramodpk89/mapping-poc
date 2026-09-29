"""Validate an AI mapping analysis and produce a portable HTML review. Python 3.9+."""
from pathlib import Path
import argparse
import hashlib
import json
import sys

BASE = Path(__file__).resolve().parent

def fingerprint(value):
    return hashlib.sha256(json.dumps(value, sort_keys=True, ensure_ascii=False,
                                     separators=(',', ':')).encode('utf-8')).hexdigest()

def validate_schema(value, schema, path='$'):
    # The schemas bundled with this POC use only this documented subset.
    types = {'object':lambda x:isinstance(x,dict), 'array':lambda x:isinstance(x,list),
             'string':lambda x:isinstance(x,str), 'integer':lambda x:type(x) is int,
             'boolean':lambda x:type(x) is bool, 'null':lambda x:x is None}
    expected = schema.get('type')
    if expected:
        expected = expected if isinstance(expected,list) else [expected]
        if not any(types[t](value) for t in expected):
            raise ValueError(f'{path}: expected {expected}')
    if 'enum' in schema and value not in schema['enum']:
        raise ValueError(f'{path}: unexpected value {value!r}')
    if 'const' in schema and value != schema['const']:
        raise ValueError(f'{path}: expected {schema["const"]!r}')
    if 'minimum' in schema and value < schema['minimum']:
        raise ValueError(f'{path}: below minimum')
    if isinstance(value,dict):
        for key in schema.get('required',[]):
            if key not in value: raise ValueError(f'{path}: missing {key}')
        props = schema.get('properties',{})
        extra = schema.get('additionalProperties',True)
        for key,item in value.items():
            if key in props: validate_schema(item,props[key],f'{path}.{key}')
            elif extra is False: raise ValueError(f'{path}: unexpected property {key}')
            elif isinstance(extra,dict): validate_schema(item,extra,f'{path}.{key}')
    if isinstance(value,list) and 'items' in schema:
        for i,item in enumerate(value): validate_schema(item,schema['items'],f'{path}[{i}]')

def unique_ids(items, key, label):
    values=[x[key] for x in items]
    if len(values) != len(set(values)): raise ValueError(f'Duplicate {label}')
    return set(values)

def validate_pack(inp, analysis):
    for name,value in [('input',inp),('analysis',analysis)]:
        validate_schema(value,json.loads((BASE/'schemas'/f'{name}.schema.json').read_text(encoding='utf-8')))
    if inp['interface']['id'] != analysis['interface_id']:
        raise ValueError('Input and analysis refer to different interfaces')
    fields=unique_ids(inp['source']['fields'],'name','source field')
    mapped=unique_ids(analysis['mappings'],'source_field','mapping field')
    if fields != mapped: raise ValueError(f'Mapping coverage mismatch: {sorted(fields ^ mapped)}')
    qids=unique_ids(analysis['questions'],'id','question ID')
    eids=unique_ids(analysis['evidence'],'id','evidence ID')
    unique_ids(analysis['interface_gates'],'id','gate ID')
    if set(inp['answers']) - qids: raise ValueError('Answers contain unknown question IDs')
    for q in analysis['questions']:
        if set(q['affected_fields']) - fields - {'*'}: raise ValueError(f'{q["id"]}: unknown affected field')
    for collection in ['mappings','target_requirements','target_candidates','observations','interface_gates']:
        for row in analysis[collection]:
            if set(row.get('question_ids',[])) - qids: raise ValueError(f'{collection}: unknown question reference')
            if set(row.get('evidence_ids',[])) - eids: raise ValueError(f'{collection}: unknown evidence reference')
    for row in analysis['mappings']:
        if row['status'] in ('ready','excluded'):
            if row['decision_basis'] != 'confirmed' or not (row['confirmed_by'] or '').strip():
                raise ValueError(f'{row["source_field"]}: ready/excluded needs a confirmed decision and reviewer')
            if not row['evidence_ids']: raise ValueError('Confirmed mapping needs supporting evidence')
            if any(not inp['answers'].get(q,'').strip() for q in row['question_ids']):
                raise ValueError(f'{row["source_field"]}: unanswered mapping dependency')
    return fingerprint(inp) != analysis['input_sha256']

def render(inp, analysis, destination):
    stale=validate_pack(inp,analysis)
    for evidence in analysis['evidence']:
        if evidence['url'].startswith('Current/') and evidence.get('sha256'):
            source=(BASE.parent/evidence['url']).resolve()
            if not source.is_relative_to(BASE.parent):
                raise ValueError('Source evidence path escapes the pack')
            if not source.is_file() or hashlib.sha256(source.read_bytes()).hexdigest()!=evidence['sha256']:
                stale=True
    data={'input':inp,'analysis':analysis,'stale':stale}
    # Prevent user-supplied XML or </script> sequences from escaping the data element.
    payload=json.dumps(data,ensure_ascii=False).replace('<','\\u003c').replace('>','\\u003e').replace('&','\\u0026')
    template=(BASE/'report-template.html').read_text(encoding='utf-8')
    Path(destination).write_text(template.replace('__PACK_DATA__',payload),encoding='utf-8')
    return stale

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--input',type=Path,default=BASE/'input.json')
    p.add_argument('--analysis',type=Path,default=BASE/'analysis.json')
    p.add_argument('--output',type=Path,default=BASE/'report.html')
    p.add_argument('--fingerprint',action='store_true',help='Print input hash for an AI analysis revision')
    args=p.parse_args()
    try:
        inp=json.loads(args.input.read_text(encoding='utf-8-sig'))
        if args.fingerprint:
            print(fingerprint(inp));return 0
        analysis=json.loads(args.analysis.read_text(encoding='utf-8-sig'))
        stale=render(inp,analysis,args.output)
        print(f'Report saved: {args.output}')
        if stale: print('INPUT CHANGED: previous mappings are marked for AI re-analysis.')
        return 0
    except (OSError,ValueError,KeyError,TypeError) as e:
        print(f'Cannot generate report: {e}',file=sys.stderr);return 1

if __name__=='__main__': sys.exit(main())
