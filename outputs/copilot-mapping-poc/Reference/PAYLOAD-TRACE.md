# Why the source fields changed

The previous helper extracted `xs:element` declarations from a hardcoded WSDL complex type,
`WebAvailabilityItem`. These were schema declarations, not WSDL service attributes, but that
method incorrectly treated an unconfirmed WSDL as the source contract and copied its types
and optionality into mapping rows. The active report does not use that extraction anymore.

The actual XML excerpt has nine `WebAvailabilityData` records. Its business leaf elements
are `AvailableDate`, `AvailableDateRange`, `AvailableQuantity`, `InStock`, `SKU`, `SKUType`
and `WebProductId`, each under `/WebItemAvailability/result/WebAvailabilityData/`.
Each generated row links the original XML and displays that full field path.

The original `Current/Source/availability-excerpt.xml` ends after the last record and lacks
closing `result` and `WebItemAvailability` tags. The separately labelled normalized copy is
accepted for extraction only after verifying the original content is unchanged and only
closing tags were appended. It is not independent evidence. Both files are preserved.

The WSDL instead describes `GetWebItemAvailabilityResponse` →
`GetWebItemAvailabilityResult` → `WebAvailabilityData` → `WebAvailabilityItem`.
That wrapper mismatch does not establish the direction, source role or transformation of
this interface. WSDL provenance and interface purpose remain unconfirmed. Folder location
is not evidence of service ownership. WSDL/schema definitions and SOAP headers are excluded
from the business payload inventory. Sample XML text does not establish types, optionality,
nullability or business meanings.

The helper also extracts other XML element/attribute names, distinguishes duplicate leaf
names by namespace-aware paths, and rejects missing/unreadable payload evidence. It makes
no Shopify request. Copilot must inspect the analyst's selected target API and propose the
actual target attributes, requirements and transformations from evidence.

`prior-candidate-analysis.json` and the saved Shopify documents preserve earlier research.
They are references only, not a selected operation, approved mapping or business confirmation.
