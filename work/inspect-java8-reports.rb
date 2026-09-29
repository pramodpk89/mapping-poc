# Maintainer-only static inspection; does not execute browser JavaScript.
require 'json'
require 'digest'
root = ARGV.fetch(0, 'outputs/java8-test-results')
raise "Execution suite failed; reports may be stale" unless JSON.parse(File.read(File.join(root, "results.json"))).fetch("failed")==0
rows = %w[basic clarified reopened alternate-target proposed].map do |name|
  file = File.join(root, 'scenarios', name, 'Report.html')
  html = File.read(file)
  payload = JSON.parse(html.match(/<script id="pack-data" type="application\/json">(.*?)<\/script>/m)[1])
  input = payload.fetch('input'); analysis = payload.fetch('analysis')
  raise 'raw answers embedded' if input.key?('answers')
  raise 'answer controls' if html.match?(/<(input|textarea|select)\b|contenteditable\s*=/i)
  %w[answer-count updateAnswers input.answers save-answers].each { |s| raise "Old feature #{s}" if html.include?(s) }
  raise 'mapping coverage' unless analysis['mappings'].map{|m|m['source_field']}.sort == input['source']['fields'].map{|f|f['name']}.sort
  raise 'missing XML trace' unless input['source']['fields'].all?{|f|f['path'].start_with?('/WebItemAvailability/result/WebAvailabilityData/') && f['evidence_files']==['Current/Source/availability-excerpt.xml'] && f['type'].start_with?('XML text')}
  raise 'resolved question rendering' unless html.include?("filter(q=>q.review_status!=='resolved')")
  raise "Proposal not generated" if name=="proposed" && analysis["mappings"].none?{|m|m["status"]=="proposed"}
  expected = name=='alternate-target' ? 'productUpdate' : 'inventorySetQuantities'
  raise 'wrong selected fixture target' unless input['target']['selected_operation']==expected
  row=analysis['mappings'].find{|m|m['source_field']=='SKUType'}
  raise 'clarification not incorporated' if name=='clarified' && row['status']!='excluded'
  raise 'decision not reopened' if name=='reopened' && row['status']!='needs_input'
  if %w[clarified reopened].include?(name)
    raise 'chat answer not saved' unless File.read(File.join(root,'scenarios',name,'Questions.txt')).include?('SYNTHETIC: N is normal stock')
  end
  {'scenario'=>name,'static_checks'=>'passed','xml_fields'=>input['source']['fields'].size,'selected_target_fixture'=>expected,'html_sha256'=>Digest::SHA256.file(file).hexdigest}
end
result={'inspection_method'=>'Static HTML/embedded JSON and saved analyst files; browser JavaScript not executed', 'synthetic'=>true,'scenarios'=>rows,'visual_browser_review'=>'Blocked: browser could not verify admin-enforced policy; no bypass attempted','print_pdf'=>'Not exercised','analyst_copilot_pilot'=>'Not exercised'}
Dir.mkdir(File.join(root,'inspection')) unless Dir.exist?(File.join(root,'inspection'))
File.write(File.join(root,'inspection','html-inspection.json'),JSON.pretty_generate(result)+"\n")
puts JSON.pretty_generate(result)
