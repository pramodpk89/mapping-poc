# Mapping POC — session handoff

## Next session: user-reported issues and revised direction

The user requested saving work and starting a new session. **The issues below are not
fixed yet.** Previous automated test passes do not establish that the report or analyst
workflow meets the user's expectations. Latest implementation before this note: 8ebac63.

1. **Remove the report's "your answers" feature/section.** The user reports that it is still
   present and does not save answers. Inspect the actual shipped/generated reports and
   template, including whether an older report is being opened, without dismissing the
   issue. Remove answer-entry/display UI, answer counters and associated navigation or
   misleading save instructions from the HTML. Keep clarification collection in Copilot
   chat; save those answers to the analyst files and incorporate them on rerun. Unresolved
   questions may remain a simple read-only list, without suggesting the HTML saves answers.
2. **Investigate and correct "WSDL attribute" appearing in mappings.** The user objects to
   what they see in the mapping rows. Trace the displayed fields back to supplied business
   payload evidence. Do not turn WSDL metadata or service definitions into business mapping
   attributes, or assume a supplied WSDL is the source contract because of its folder.
   Current extraction is hardcoded to embedded WebAvailabilityItem; revisit that dependency
   and source classification. Preserve the original WSDL/XML files. WSDL provenance/role
   and the real interface purpose remain unconfirmed.
3. **Simplify Shopify target setup.** For each interface, the analyst supplies the Shopify
   API endpoint/reference they want to target; the agent should determine relevant request
   fields, API requirements and mapping from that target and source evidence. Replace the
   analyst-facing documentation/research setup and mandatory prebuilt research-bundle
   dependency with this simple input. Do not force inventorySetQuantities or ask analysts
   to curate documentation or internal JSON. Keep existing docs as reference/archive;
   do not delete evidence just to simplify the workflow. A generic GraphQL URL alone may
   not identify an operation: ask only for essential missing operation/version context
   rather than inventing it. Keep useful official-source citations in the agent's analysis.

Retain Java 1.8 and java-home.properties, the /map-interface workflow, mandatory meaningful
input checks, evidence-backed decisions, saved chat clarifications, selective reopening,
read-only HTML and last-successful-review recovery. No Python, Node.js, extra modules,
administrator access, company-policy bypass or production writes.

Next session should implement these changes, update affected tests/instructions/JAR/ZIP,
inspect the resulting reports, rerun applicable Java 8 tests (Windows CI is available),
update this handoff, and push main. Separate real execution from static review and any
remaining visual/analyst pilot limitations. Never use synthetic decisions as business facts.
Keep progress updates and the final summary short.

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

Local execution: Java 1.8.0_382 (Amazon Corretto), macOS, actual JDK 8 compilation and
execution. 69 portable checks passed, including cases 01-43 ported from the original
Python suite. Six Windows-only checks are correctly recorded as outstanding on macOS.
Results: outputs/java8-test-results/Test-results.html and results.json.

Actual Windows CI testing exposed and corrected line-ending conversion of evidence,
Unicode CLI argument loss and launcher working-directory handling. Final Windows validation: **75 passed, 0 failed, 0 outstanding automated checks**,
Java 1.8.0_504 (Temurin), Windows Server 2025. Sources also compiled with JDK 8.
Evidence: outputs/windows-java8-test-results/Test-results.html and results.json.
Successful CI: https://github.com/pramodpk89/mapping-poc/actions/runs/36536958866 . Native Java 8
failed to load libraries from a Unicode installation folder. The supported Java-home path
therefore uses ASCII characters (spaces supported). Unicode pack, evidence, edits-file and
answer paths are handled through map.cmd's Unicode environment boundary. Do not bypass
policy or change registry/short-name settings. Use an approved ASCII Java installation path.

Three generated report scenarios were inspected statically: basic, clarified and reopened.
Checked embedded JSON, seven-field coverage, read-only controls, preserved answers and
reopened status. Browser visual inspection remains outstanding: browser security policy
blocked local file URLs; no workaround was attempted. Print/PDF was not exercised.
The ZIP was checked for internal integrity and byte-for-byte correspondence, including the
JAR, hidden Copilot skill, source evidence and saved Shopify docs; no Python/PowerShell or
transient run history is included. The real pack still blocks on missing business purpose.

Tests do not validate Copilot reasoning, organization-specific policy or actual analyst
Copilot skill discovery. A real analyst pilot and live Shopify/schema validation remain
outstanding. All synthetic decisions are confined to temporary tests and labelled reports.
