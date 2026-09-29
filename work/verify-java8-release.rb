# Maintainer packaging/evidence verification; not an analyst runtime dependency.
require 'json'
require 'digest'
require 'open3'
zip='outputs/copilot-mapping-poc.zip'
base='outputs/copilot-mapping-poc'
entries, status=Open3.capture2('unzip','-Z1',zip)
raise 'cannot read ZIP' unless status.success?
files=entries.lines.map(&:strip).reject{|f|f.end_with?('/')}
files.each do |name|
  raise "Unexpected ZIP content: #{name}" unless name.start_with?('copilot-mapping-poc/') && !name.match?(/\.py[c]?\z|\.ps1\z|\/history\/|\/build\/|run-history|run.lock|clarifications.json|Last-review.html/)
  bytes, status=Open3.capture2('unzip','-p',zip,name)
  raise "ZIP mismatch #{name}" unless status.success? && bytes.b==File.binread('outputs/'+name)
end
expected=Dir.glob(base+'/**/*',File::FNM_DOTMATCH).select{|f|File.file?(f)}.reject{|f|f.match?(/\/history\/|\/build\/|run-history|run.lock|clarifications.json|\/Test-results\/|\.DS_Store/) }.map{|f|f.sub('outputs/','')}
raise 'ZIP coverage differs from folder' unless files.sort==expected.sort
raise 'Copilot skill missing' unless files.include?('copilot-mapping-poc/.github/skills/map-interface/SKILL.md')
refs=Dir.glob(base+'/Current/Source/*').select{|f|f.match?(/\.(xml|wsdl)$/)}+Dir.glob(base+'/Shopify/Docs/*')+[base+'/Shopify/Target-reference.json']
baseline = ARGV.fetch(0, "19e2fc0")
refs.each do |file|
 old, status=Open3.capture2('git','show',baseline+':'+file)
 raise "Evidence changed: #{file}" unless status.success? && old.b==File.binread(file)
end
raise "Real target must remain unselected" unless JSON.parse(File.read(base+"/.framework/input.json"))["target"]["selected_operation"].nil?
raise "Real preflight must block" unless JSON.parse(File.read(base+"/.framework/preflight.json"))["status"]=="blocked"
result={'reference_baseline'=>baseline,'zip_files_verified'=>files.size,'preserved_reference_files'=>refs.size,'zip_sha256'=>Digest::SHA256.file(zip).hexdigest,'jar_sha256'=>Digest::SHA256.file(base+'/.framework/mapping.jar').hexdigest,'original_evidence'=>'byte-for-byte unchanged','single_working_folder'=>base,'real_purpose'=>'unconfirmed','real_shopify_target'=>'not supplied','wsdl_provenance'=>'unconfirmed'}
puts JSON.pretty_generate(result)
