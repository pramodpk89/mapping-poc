---
name: map-interface
description: Map supplied XML or business contracts to Shopify attributes. Discover relevant Shopify APIs, inspect any preferred API URL, and produce a useful evidence-backed report with concrete destinations and rules.
---

# Functional analyst mapping

Read [RUN.md](../../../RUN.md), then complete discovery, mapping and report generation.
The analyst supplies XML or a contract. Shopify setup is only an optional API URL in
Shopify/API-Endpoint.txt or chat. Save chat URLs through prepare --edits shopify_reference;
never ask the analyst to maintain a documentation folder. Historical documents are in
.framework/references/shopify for agent reference only.
The agent researches APIs and fields. Do not ask for curated documentation, internal JSON,
API version research or per-field approvals before producing a draft.

Inspect the actual source and infer the likely interface purpose without inventing business
facts. Browse official Shopify documentation and follow relevant input/field links. If a URL
is supplied, inspect it; otherwise discover suitable APIs from the evidence. Missing purpose
prose or a URL does not block proposals. Do not default every interface to inventory updates.

Produce concrete attribute destinations, real sample values, conversion/lookup rules and
citations. Use `proposed` for useful evidence-backed mappings before business confirmation.
Clearly distinguish native fields, lookups and proposed custom metafields. Never describe a
custom key as a built-in Shopify attribute, or fabricate IDs, meanings, locations or approval.
Unclear special dates or field meaning should affect that row, not prevent other mappings.

Keep WSDL metadata out of mappings. XML fields drive the inventory when available; otherwise
select the relevant business schema element using the agent-managed contract selection.
Parsing a schema does not establish its provenance or interface role. Preserve original files.

Use the Java 8 helper and guarded generator in RUN.md. Save actual chat clarifications through
prepare --edits; keep agent research in target-discovery.json and analysis.json, not in analyst
answers. Preserve stable questions, unrelated answers and the last successful review. Changed
proposals and confirmed decisions reopen selectively. The HTML has no answer controls or
answer counters. Never copy synthetic test answers or decisions into the real analyst pack.

Return the report link and concise counts of concrete proposals and remaining decisions.
Do not present workflow-test reports as the real interface mapping. No live store calls,
credentials, extra modules, runtime installations or company-policy bypass.
