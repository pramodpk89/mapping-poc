# Mapping POC — session handoff

Repository: https://github.com/pramodpk89/mapping-poc, branch main.
One working folder: outputs/copilot-mapping-poc. Old packs are removed.

## Latest user direction

The user will show this to a customer VP. Functional analysts supply XML/contracts; the
agent must produce real attribute mappings, not an empty test report. A Shopify API URL is
optional. If supplied, the agent inspects it; otherwise it discovers APIs from the source.
This explicitly supersedes the earlier mandatory-purpose and mandatory-target-URL gates.
Missing business decisions must not block all evidence-backed proposals. Keep responses short.

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

New implementation: actual JDK 8 compilation and **91 portable checks passed, 0 failed**
on macOS/Corretto 1.8.0_382. Six Windows-only checks await the new Windows CI run.
Previous 89-check Windows results do not validate this changed implementation.
Five synthetic workflow scenarios are inspected statically, separately from the real report.
Browser rendering and Print/PDF remain unverified: browser access was denied because the
admin-enforced policy could not be verified. No bypass attempted. Actual Copilot pilot remains
outstanding. Tests must never be used as customer business confirmations.

Next: finish Windows CI, save actual results, refresh ZIP verification and push final records.
