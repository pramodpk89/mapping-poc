# Mapping POC — session handoff

## Current direction

Repository: https://github.com/pramodpk89/mapping-poc, branch main.
Active pack: outputs/copilot-mapping-poc. Open that entire folder in VS Code.
The user explicitly replaced the PowerShell migration request with **Java 1.8** and asked
for a file that sets the Java folder before running. The active implementation is now
Java 8, standard library only, shipped as .framework/mapping.jar with source alongside it.
Set java.home in java-home.properties to an approved JRE/JDK 8 folder. map.cmd reads this
setting; Test-Windows.cmd runs the bundled acceptance suite. No Python, Node.js, PowerShell
helper, extra module or admin access is part of the analyst workflow. Do not bypass company
execution policies or application controls. Runtime installation is a team setup concern.

## Preserved workflow

Copilot /map-interface reads RUN.md. Chat clarifications are saved to a structured JSON data
file, then imported into Understanding.txt, Questions.txt and Decisions.csv using
`map.cmd prepare --edits FILE`. Do not replay an old clarification file; prepare without
edits for later unchanged runs. Unknown purpose/evidence block the run. Analysis is performed
by Copilot, not the Java helper. `map.cmd fingerprint` provides the current input hash;
`map.cmd generate` validates the analysis and renders the read-only report.

Changed decisions reopen affected confirmations; unrelated answers are retained. Conflicting
CSV decisions are not confirmed. Same-reviewer decision changes cannot retain stale evidence
or merely refresh a hash. Ready/excluded evidence must link Decisions.csv and quote the exact
current decision. File locks prevent concurrent writes. Atomic file saves and backups preserve
the last successful review on failure. Completed history includes input, analysis and HTML.

## Evidence and business uncertainty — unchanged

Real interface purpose is still a placeholder, and the real run must stop there.
WSDL provenance and role are unconfirmed. Their Current/Source placement does not establish
that they describe the source service. The helper uses embedded WebAvailabilityItem structure
for this POC and always warns that provenance is unconfirmed. Other schemas need a reviewed
adapter; absence of that inventory blocks rather than reusing stale fields.
The original incomplete XML excerpt and separately labelled normalized copy remain unchanged.
No source evidence or saved Shopify documentation has been moved or rewritten.
All test purposes, analyst identities, answers, decisions and analysis are **synthetic**.
Do not import them into the real pack or treat them as business approval.

Shopify research is the preserved public shopify.dev snapshot, API 2026-07, retrieved
2026-09-29. inventorySetQuantities is only a candidate if the feed updates authoritative
absolute stock. It is not an approved target. No live store calls/writes were made. Saved
concurrency examples versus live schema still need validation.

## Validation

Local execution: Java 1.8.0_382 (Amazon Corretto), macOS, actual compilation and execution.
67 portable checks passed, including the 43 ported Python scenarios. Five Windows-specific
checks were correctly marked outstanding on macOS. Results: outputs/java8-test-results/.
The GitHub Windows Java 8 workflow is being used to validate the actual Windows launcher,
Unicode paths, configured Java folder, and locked-file recovery; its final outcome is to be
recorded here before completion. Browser inspection and final ZIP verification are in progress.
The suite does not validate Copilot reasoning or the organization's policy configuration.
A real Windows analyst Copilot pilot, Print/PDF and live Shopify validation remain outstanding.

## Files and maintenance

- START-HERE.txt, RUN.md, TESTING.md and .github instructions describe the Java workflow.
- .framework/java contains Json.java, Mapping.java and WorkflowTests.java.
- The Windows acceptance workflow runs the shipped JAR and compiles sources with JDK 8.
- Legacy Python helpers/tests are archived in work/legacy-python for traceability only.
- Older prototype packs and test reports are historical, not the active implementation.
- Exit codes: 0 success; 2 blocked/setup/invalid input; 3 analysis stale; test failure 1.

Before a real mapping review, obtain the business purpose and clarify WSDL/source provenance.
Never fill those gaps using test fixtures.
