# Mapping POC — session handoff

Repository: https://github.com/pramodpk89/mapping-poc, branch main.
One working folder: outputs/copilot-mapping-poc. Old packs are removed.

## Latest user direction

The user will show this to a customer VP. Functional analysts supply XML/contracts; the
agent must produce real attribute mappings, not an empty test report. A Shopify API URL is
optional. If supplied, the agent inspects it; otherwise it discovers APIs from the source.
This explicitly supersedes the earlier mandatory-purpose and mandatory-target-URL gates.
Missing business decisions must not block all evidence-backed proposals. Keep responses short.

## Latest simplification

Shopify now contains only API-Endpoint.txt. Paste one URL there or in Copilot chat;
blank still supports source-driven discovery. The helper imports that file, saves chat URLs
there, removes the duplicate Understanding URL label, and preserves/reopens reviews.
All prior Shopify docs/config/notes moved unchanged into .framework/references/shopify.
They are internal evidence, not analyst setup. Three new endpoint regression checks pass.
The rebuilt JAR passes 94 portable checks locally and all 100 checks on Windows, using Java 8.

## Current real report

outputs/copilot-mapping-poc/Report.html now contains **five proposed mappings and two
candidate mappings needing clarification**, based on the actual nine XML records and
browsed official Shopify documentation for API 2026-07. It is not a synthetic test report.

- AvailableQuantity → inventory quantity input (conditional on stock meaning/authority).
- SKU → inventoryItems SKU lookup → inventoryItemId.
- AvailableDateRange → proposed custom availability_message metafield.
- InStock → proposed custom source_in_stock boolean; never a direct availableForSale write.
- WebProductId → proposed custom erp_product_id string, never a fabricated Shopify GID.
- AvailableDate → candidate date_time custom field; timezone/sentinel meaning unresolved.
- SKUType → candidate raw-code custom field or exclusion after clarification.

Custom namespaces/keys are explicitly design proposals, not native Shopify attributes or
existing customer definitions. The API recommendation is agent-inferred from XML and kept
in target-discovery.json, not written into analyst answers as if the user had chosen it.
Business purpose, WSDL provenance, stock authority, locations and special-date meanings
remain unconfirmed. No real customer decisions, identities or Shopify IDs were fabricated.

## Framework changes

Intake accepts source evidence without purpose prose, a URL or preselected API/version.
The agent researches before generating. A supplied URL overrides unrelated discovery.
`proposed` mappings have concrete destinations/rules and source/API evidence but do not need
business confirmations. `ready`/`excluded` retain exact decision/attribution requirements.
Source or decision changes selectively reopen affected proposals and confirmations.
XML drives fields when present; simple XSD business structures are supported without XML.
WSDLs require an agent-managed selection of the relevant schema element. Service metadata
is never mapped and folder location never establishes provenance. Unsupported contract
structures stop instead of returning a truncated/guessed inventory.

Read-only HTML includes samples, mapping kinds and destinations; there is no answer feature.
Chat clarifications persist in human files; last-successful-review recovery is retained.
Java 1.8, java-home.properties, map.cmd and /map-interface remain. No Python, Node.js,
extra modules, administrator access, execution-policy bypass or live ERP/Shopify writes.
Original XML/WSDL and saved Shopify docs remain unchanged.

## Validation

Actual local JDK 8 compilation and execution: **94 passed, 0 failed**, Corretto 1.8.0_382
on macOS; six Windows checks outstanding locally. Actual Windows execution: **100 passed,
0 failed, 0 outstanding**, Windows Server 2025 / Temurin Java 1.8.0_504. The shipped JAR
and CMD launcher ran; sources compiled with JDK 8. Tested implementation:
257685cd3e99bc1304b9c41af2182b3fb24c43fc.
CI: https://github.com/pramodpk89/mapping-poc/actions/runs/36545777188 .
Downloaded results: outputs/windows-java8-test-results. Tests use synthetic decisions only.

The real report and five scenarios from each host passed static HTML/data inspection.
Browser rendering, Print/PDF and an actual Copilot pilot remain unverified. Browser access
was previously denied because admin-enforced policy could not be verified; no bypass attempted.
The rebuilt single ZIP contains 47 files, verified byte-for-byte against the working folder.
Fourteen original XML/WSDL/Shopify references remain byte-for-byte unchanged after relocation.
Updated implementation, instructions, report, JAR, ZIP and validation are pushed to main.
Use the real Report.html for customer review, not synthetic test scenarios. Business purpose,
WSDL provenance and unresolved business decisions remain unconfirmed.
