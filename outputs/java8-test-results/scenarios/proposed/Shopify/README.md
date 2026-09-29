# Choose the Shopify API for this interface

Give Copilot the **Shopify API endpoint or reference URL you want to target** in chat,
then run `/map-interface`. Copilot inspects the reference, determines relevant request fields
and requirements, and proposes destinations for fields in the supplied XML payload.

A versioned operation reference is enough. A generic GraphQL URL also needs the operation;
Copilot asks for the API version only when it cannot determine it from the supplied reference.
For REST, provide the HTTP method if it is not identified by the reference. Copilot saves
these details in Understanding.txt. You do not need to curate documentation or internal JSON.

No API operation is selected by default. Missing field meanings and business decisions stay
open for clarification in chat. The HTML report is read-only.

## Preserved references

[Target-reference.json](Target-reference.json) and [Docs/manifest.json](Docs/manifest.json)
contain earlier research and checksums for eight saved official pages, retrieved 2026-09-29
for API 2026-07. They are historical references; the framework does not require this bundle.
The earlier inventory candidate is not a business confirmation or a target selection.
Copilot should check relevance and version against the newly supplied reference, record
current citations, and identify any unresolved documentation discrepancies. No live store
or schema validation has been performed for this supplied interface.
