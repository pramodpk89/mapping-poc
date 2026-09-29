from pathlib import Path
import re
root=Path('outputs/copilot-mapping-poc')
p=root/'.framework/report-template.html'
s=p.read_text()
s=re.sub(r'<div class="toolbar"><button class="primary".*?</div>\n<div id="flash".*?</textarea>', '<div class="toolbar"><button id="print">Print / PDF</button></div>', s, count=1, flags=re.S)
s=s.replace('Tell Copilot your clarifications in chat and run /map-interface again. You can also answer below and copy the changes into Copilot. Your source files and earlier decisions are preserved.', 'Read-only report. To answer a question or correct a detail, tell Copilot in chat and run /map-interface again. Copilot saves your clarification and generates a new report.')
s=s.replace('Interface input</a>', 'Interface details</a>').replace('Your answers</a>', 'Questions &amp; recorded answers</a>')
a=s.index('<section id="overview"');b=s.index('<section id="mapping"',a)
s=s[:a]+'''<section id="overview" class="section"><details class="panel"><summary>Interface details</summary><div style="margin-top:18px"><h3>Purpose</h3><p id="description" class="recorded"></p><div class="grid"><div><h3>Payload origin</h3><p id="source-origin" class="recorded"></p></div><div><h3>Reviewer</h3><p id="reviewer" class="recorded"></p></div></div><h3>Additional context</h3><p id="context" class="recorded"></p><div id="known-rules"></div></div></details></section>
'''+s[b:]
s=s.replace('<h2>Your answers ', '<h2>Questions &amp; recorded answers ')
s=s.replace('Start with the intended behavior, quantity meaning and location. Leave anything you don’t know blank.', 'Answer these questions in Copilot chat, using the question number. Recorded answers below reflect this report’s last run.')
s=s.replace('let input=structuredClone(pack.input), filter=null, unsaved=false;', 'const input=pack.input;\nlet filter=null;')
a=s.index('const stable=');b=s.index('const node=',a)
s=s[:a]+'const stale=()=>Boolean(pack.stale);\n'+s[b:]
s=re.sub(r'function setFlash\(message\).*?\n','',s)
a=s.index('function clarificationText()');b=s.index('function start(){',a)
s=s[:a]+'''function populateDetails(){
 for(const [id,value] of [['description',input.interface.description],['source-origin',input.interface.source_origin],['reviewer',input.interface.reviewer],['context',input.additional_context]])$(id).textContent=value||'Not provided';
 if(input.known_rules.length){$('known-rules').append(node('h3','Confirmed rules'));for(const r of input.known_rules)$('known-rules').append(node('p',r.statement+' — '+r.confirmed_by));}
 updateAnswers();renderMappings();
}
'''+s[b:]
a=s.index(' for(const q of analysis.questions){const d=');b=s.index(' for(const c of analysis.target_candidates)',a)
s=s[:a]+''' for(const q of analysis.questions){const d=node('div',undefined,'question');d.id='answer-'+q.id;const head=node('div',undefined,'question-head');head.append(node('span',q.id,'qid'),node('strong',q.question));const status=node('span',undefined,'answer-status');status.id='status-'+q.id;head.append(status);const answer=input.answers[q.id]||'';const recorded=node('p',answer.trim()?answer:'No answer recorded.','recorded'+(answer.trim()?'':' muted'));const why=node('p',q.why,'hint muted');d.append(head,why,recorded);if(q.resolution_note)d.append(node('p',q.resolution_note,'hint muted'));$('question-list').append(d);}
'''+s[b:]
a=s.index(" $('copy-clarifications').onclick");b=s.index('\n}\nstart();',a)
s=s[:a]+''' $('all-fields').onclick=()=>{filter=null;renderMappings();};
 let printDetails=[];window.addEventListener('beforeprint',()=>{printDetails=[...document.querySelectorAll('details')].map(d=>[d,d.open]);for(const [d] of printDetails)d.open=true;});window.addEventListener('afterprint',()=>{for(const [d,open] of printDetails)d.open=open;});$('print').onclick=()=>{window.print();};populateDetails();
'''+s[b:]
s=s.replace('</style>', '.recorded{white-space:pre-wrap;overflow-wrap:anywhere}.question{scroll-margin-top:20px}</style>')
p.write_text(s)
p=root/'START-HERE.txt';s=p.read_text();a=s.index('or Decisions.csv (Excel).');b=s.index('\n\nIf /map-interface',a)
s=s[:a]+'''or Decisions.csv (Excel). The HTML report is read-only: give new answers and corrections
in Copilot chat, then run /map-interface again. You do not need to edit JSON or run Python
yourself. Last-review.html preserves your last completed review if a rerun fails.'''+s[b:];p.write_text(s)
