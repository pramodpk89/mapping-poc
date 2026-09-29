# Mapping POC — next session

## Next priority

**The team's Windows machines do not have Python.** The current implementation requires Python 3.9+ and therefore does not run in the stated environment. Redesign the execution path to work without an analyst-installed Python runtime. No replacement has been chosen or implemented. Do not assume Node.js or another runtime is installed either. Confirm the available Windows/Copilot environment before selecting an approach, and retain enforceable validation rather than relying only on model instructions.

The user requested saving the work and continuing in a new session; do not interpret this note as evidence that a runtime migration is already complete.

## Purpose and user preferences

- Migration: ERP → middleware → Sitecore becomes ERP → middleware → Shopify.
- More than 45 interfaces; start with item availability and reuse the framework.
- Users are functional analysts. They can provide payloads, WSDLs, notes and attribute decisions, but should not maintain internal JSON or run technical commands.
- Preferred workflow: VS Code + GitHub Copilot Agent mode, `/map-interface`; model and authentication managed by Copilot.
- Required-input checks must block incomplete runs. Unknown business rules must remain open questions.
- Reports are ordinary browser-friendly HTML, read-only. Editable boxes were explicitly removed because they did not execute AI analysis. Clarifications go in chat, are saved, and are reviewed on rerun.
- Keep explanations short. Do not use Canvas for reports.

## Repository and current files

- GitHub: https://github.com/pramodpk89/mapping-poc
- Branch: `main`.
- Current pack: `outputs/copilot-mapping-poc/`; open this folder in VS Code for its `.github` skill discovery.
- ZIP: `outputs/copilot-mapping-poc.zip`.
- HTML test results: `outputs/mapping-test-results/Test-results.html`.
- Three generated demo reports under `outputs/mapping-test-results/scenarios/`: basic, clarified, and reopened after a correction.
- Earlier `availability-poc` and `cowork-mapping-poc` folders are archived prototypes for reference, not the active design.
- Development scripts are under `work/`; some have original-machine absolute paths and need portability work if reused.

## Interface evidence and unresolved questions

Interface: GetWebItemAvailability. The sample has nine records and seven fields: AvailableDate, AvailableDateRange, AvailableQuantity, InStock, SKU, SKUType, WebProductId.

The pasted XML was missing two closing tags. Preserve the original excerpt; the separately labelled normalized copy only adds those tags.

**WSDL provenance and role are unconfirmed.** They were placed in `Current/Source` based on an unsupported assumption. The user challenged this. A WSDL can describe a source SOAP service or a current target service depending on data direction, but the role of these supplied WSDLs is still unknown. No files were moved and no provenance was confirmed. Revisit evidence classification and source-schema conclusions before treating them as authoritative. The current check relies on the embedded WSDL schema because the original XML is incomplete; moving the files requires addressing that dependency honestly.

The business purpose remains a placeholder in the real pack. Source origin, transformation and business rules are unknown. The real run stops at the required-purpose check. Demo reports use synthetic analyst answers; they are not real confirmed business decisions. The screenshot's typed “yes” was not a meaningful confirmed business-purpose answer and was not saved into the real analyst files.

## Shopify research

- Proposed candidate: GraphQL Admin `inventorySetQuantities`, if the feed updates authoritative absolute stock quantities. Not approved as the final target.
- Candidate mapping: AvailableQuantity → quantity; SKU → inventoryItemId lookup; locationId needs a business rule and store lookup.
- Dates, messages and other attributes need further decisions.
- `outputs/copilot-mapping-poc/Shopify/README.md` explains the candidate and links eight saved official pages in `Shopify/Docs/`.
- `Shopify/Target-reference.json` holds structured research; `Shopify/Docs/manifest.json` records source URLs, retrieval date and hashes.
- Research came from public shopify.dev documentation, not Shopify MCP or a store connection. The user initially requested MCP and later accepted public documentation for the POC.
- Saved API version is 2026-07, retrieved 2026-09-29. Reruns reuse the snapshot; automatic refresh is not implemented.
- Saved mutation examples use legacy concurrency wording that differs from the input reference. Live schema/store validation remains outstanding.

## Implemented and tested

Current Python helpers import chat clarifications into analyst files, preserve earlier answers, validate prerequisites and mapping evidence, reject stale analysis/imports, and generate the report. Ready/excluded mappings require an attributed field decision. Failed reruns retain the last completed review. Conflicting clarifications reopen affected mappings without losing unrelated decisions.

43 automated tests passed locally. Six current browser checks cover the read-only reports, recorded answers, filtering and corrected-decision scenario. All three report scenarios use synthetic analysis fixtures: this validates framework behavior, not Copilot reasoning. No actual Windows/Copilot execution, live Shopify write, or Print/PDF validation has been performed. The UI contains no answer editing/import/export controls; legacy answer-file import support remains in the helper for compatibility.

## Continue with

1. Establish a workable Windows execution path without Python and without assuming another installed runtime.
2. Preserve the mandatory checks, traceable mapping decisions, clarification/rerun behavior and read-only HTML output.
3. Resolve evidence roles and obtain the interface purpose before a real mapping run.
4. Update the pack, setup instructions and tests for the selected runtime; perform a real Windows + Copilot pilot before team release.
