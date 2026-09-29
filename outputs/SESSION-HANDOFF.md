# Mapping POC — session handoff

Repository: https://github.com/pramodpk89/mapping-poc, branch main.
Only working pack: **outputs/copilot-mapping-poc**; distribution: copilot-mapping-poc.zip.
Open that entire folder in VS Code. Old Cowork/availability packs, their ZIPs, obsolete
prototype results and Python helpers were removed at the user's explicit request.

## Implemented in this session

- Removed the HTML answer feature completely: no answer text, entry controls, counters,
  download/save flow or raw answers embedded in report data. Unresolved questions remain
  read-only. Copilot collects chat clarifications and persists them to Understanding.txt,
  Questions.txt and Decisions.csv via prepare --edits; reruns incorporate them.
- Replaced hardcoded WSDL WebAvailabilityItem extraction with actual XML business leaf
  elements/attributes, namespace-aware paths and original-file evidence links. WSDL/XSD
  service metadata and SOAP headers are excluded. Types/optionality are not borrowed from
  an unconfirmed WSDL. The supplied incomplete original is extracted through the separately
  labelled normalized copy only after verifying that only closing tags were appended.
  Detailed trace: copilot-mapping-poc/Reference/PAYLOAD-TRACE.md.
- The analyst supplies a Shopify API endpoint/reference in chat. The helper infers operation
  and version when present and asks only for essential missing details. Copilot inspects the
  API and derives relevant request fields, requirements and mappings. Versioned official
  citations are required at generation. No mandatory research bundle and no default to
  inventorySetQuantities. Existing docs and prior candidate analysis remain references.

## Preserved workflow and boundaries

Java **1.8**, standard library only. Team configures java.home in java-home.properties.
Use map.cmd and /map-interface. No Python, Node.js, extra modules, administrator access,
policy bypass or production writes. Java installation paths must be ASCII; spaces supported.
Unicode pack/evidence/clarification paths remain supported through the Windows launcher.

Mandatory purpose, XML payload and target-input checks stop incomplete runs. Analysis is
Copilot's responsibility, not the helper's. Exact decision evidence, reviewer attribution,
input freshness, selective reopening, file locks, atomic saves and last-successful-review
recovery remain enforced. Do not replay old clarification files; import only new edits,
then prepare without edits on unchanged runs. Synthetic tests never supply business approval.

## Real evidence remains unconfirmed

The XML supplies seven business fields under /WebItemAvailability/result/WebAvailabilityData.
The WSDL wrapper differs and its role/provenance remains unconfirmed. The real purpose and
Shopify target reference have not been supplied, so the real Report.html correctly blocks.
No original XML/WSDL or saved Shopify documentation was changed. No live Shopify schema/store
validation was performed. Prior research is not a target selection or a business confirmation.

## Validation status

Actual local compilation/execution: Corretto Java 1.8.0_382, macOS, **83 passed, 0 failed**.
Actual Windows CI: Temurin Java 1.8.0_504, Windows Server 2025, **89 passed, 0 failed,
0 outstanding automated checks**. The shipped JAR/launcher ran and sources compiled with JDK 8.
Implementation commit: `0c41ee8b7f54c224e8731bc4b7fe2998ad6367ab`.
Successful CI: https://github.com/pramodpk89/mapping-poc/actions/runs/36542653010 .
Downloaded execution evidence: outputs/windows-java8-test-results/; local evidence:
outputs/java8-test-results/. Later documentation/result commits do not change the tested JAR.

Static inspection passed for four reports from each host: basic, clarified, reopened and
alternate target. It checked no answer feature/raw answer data, seven XML paths, selected
fixture target, saved chat answers and reopened decisions. All scenarios are synthetic.
Visual inspection was attempted but the browser could not verify the admin-enforced policy
and denied access. No bypass was attempted. Browser JavaScript, Print/PDF and an actual
analyst Copilot pilot remain unverified; no claim of visual execution is made.

The final ZIP has one copilot-mapping-poc root, includes the hidden Copilot skill, Java sources,
JAR and tests, and excludes transient history/build files. All file bytes were compared with
the active folder. Fourteen original source/reference files were verified unchanged against
pre-session commit 19e2fc0. See outputs/java8-release-validation.json for artifact hashes.

Implementation, instructions, tests, JAR, ZIP and handoff are updated; no further code work
is outstanding for these requested fixes. The next real run needs the actual interface purpose
and intended Shopify API reference from the analyst. Do not use synthetic test data to fill them.
Keep updates and the final summary short.
