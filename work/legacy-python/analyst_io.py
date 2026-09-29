"""Read analyst documents without requiring precise spacing or Excel delimiters."""
import csv
import io
import json
import re
from pathlib import Path

LABELS = ['Interface name','What this interface does','Current flow','Target flow',
          'Payload origin','Reviewer','Additional context']

def read_text(path):
    raw=Path(path).read_bytes()
    return raw.decode('utf-16' if raw.startswith((b'\xff\xfe',b'\xfe\xff')) else 'utf-8-sig')

def unknown(text):
    text=text.strip().lower().strip(' .!?')
    return not text or text.startswith('[') or text in {
        'unknown','tbd','todo','not sure','n/a','na','none','i don\'t know','i do not know','not known'}

def understanding(path):
    text=read_text(path) if Path(path).exists() else ''
    pattern=r'^\s*('+ '|'.join(re.escape(k) for k in LABELS)+r')\s*:\s*'
    marks=list(re.finditer(pattern,text,re.M|re.I))
    return {m.group(1).lower():text[m.end():marks[n+1].start() if n+1<len(marks) else len(text)].strip()
            for n,m in enumerate(marks)}

def set_understanding(path,updates):
    data=understanding(path)
    for k,v in updates.items():data[k.lower()]=v
    Path(path).write_text('\n\n'.join(k+': '+data.get(k.lower(),'') for k in LABELS)+'\n',encoding='utf-8')

def questions(path):
    text=read_text(path) if Path(path).exists() else ''
    marks=list(re.finditer(r'^\s*(Q\d+)\s*:',text,re.M))
    result={}
    for n,m in enumerate(marks):
        block=text[m.end():marks[n+1].start() if n+1<len(marks) else len(text)]
        answer=re.search(r'^\s*Answer\s*:[ \t]*(.*)',block,re.M|re.I)
        if m.group(1) in result:raise ValueError('Questions.txt contains duplicate question '+m.group(1))
        result[m.group(1)]=block[answer.start(1):].strip() if answer else ''
    return result

def set_answers(path,updates,definitions):
    # Keep the questions and every untouched answer; replace only the named answer blocks.
    text=read_text(path) if Path(path).exists() else 'QUESTIONS FOR THE ANALYST\n\n'
    questions(path)  # validate duplicate IDs before writing
    for qid,value in updates.items():
        marks=list(re.finditer(r'^\s*(Q\d+)\s*:',text,re.M))
        match=next(((n,m) for n,m in enumerate(marks) if m.group(1)==qid),None)
        if match:
            n,m=match;end=marks[n+1].start() if n+1<len(marks) else len(text)
            block=text[m.end():end];answer=re.search(r'^\s*Answer\s*:',block,re.M|re.I)
            prefix=block[:answer.start()] if answer else block.rstrip()+'\n'
            text=text[:m.end()]+prefix.rstrip()+'\nAnswer: '+value+'\n\n'+text[end:]
        else:
            q=next((q for q in definitions if q['id']==qid),None)
            if not q:raise ValueError('Unknown question '+qid+'; add its definition before recording an answer')
            text+=f"\n{qid}: {q['question']}\n{q['hint']}\nAnswer: {value}\n"
    Path(path).write_text(text,encoding='utf-8')

def decisions(path):
    if not Path(path).exists():return []
    text=read_text(path)
    try:dialect=csv.Sniffer().sniff(text[:4096],delimiters=',;\t')
    except csv.Error:dialect=csv.excel
    reader=csv.DictReader(io.StringIO(text),dialect=dialect)
    required=['Attribute','Decision','Explanation','Confirmed by']
    headers={h.strip().lower():h for h in (reader.fieldnames or [])}
    if any(h.lower() not in headers for h in required):
        raise ValueError('Decisions.csv needs the columns Attribute, Decision, Explanation and Confirmed by.')
    result=[]
    for row in reader:
        if None in row:raise ValueError('Decisions.csv has extra cells. Quote text containing commas, or save it again from Excel.')
        data={h:(row.get(headers[h.lower()]) or '').strip() for h in required}
        if any(data.values()):result.append(data)
    return result

def write_decisions(path,rows):
    with Path(path).open('w',encoding='utf-8-sig',newline='') as f:
        writer=csv.DictWriter(f,fieldnames=['Attribute','Decision','Explanation','Confirmed by'])
        writer.writeheader();writer.writerows(rows)

def confirmed_rules(rows):
    groups={}
    for r in rows:
        if r['Decision']:groups.setdefault(r['Attribute'],[]).append(r)
    rules=[];warnings=[]
    for attr,entries in groups.items():
        if len({r['Decision'] for r in entries})>1:
            warnings.append('Conflicting decisions for '+attr+'. That field needs clarification; no confirmation was carried forward.')
            continue
        row=entries[-1]
        if not unknown(row['Confirmed by']) and not unknown(row['Decision']):
            rules.append({'id':'decision:'+attr,'attribute':attr,'statement':row['Decision'],
                          'confirmed_by':row['Confirmed by']})
    return rules,warnings

def write_json(path,value):
    Path(path).write_text(json.dumps(value,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
