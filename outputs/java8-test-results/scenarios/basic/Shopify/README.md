# Shopify target for item availability

**Proposed API: `inventorySetQuantities` — GraphQL Admin API.**

This is a candidate for updating absolute inventory quantities. It is not yet an approved mapping: the interface's purpose, quantity meaning, identifiers, and location rules still need confirmation.

## Read the target documentation

These are saved copies of official Shopify documentation, retrieved on 29 September 2026 for API version 2026-07. They are included in this pack so analysts and AI assistants can read them directly. Reruns reuse saved research; they do not automatically refresh these pages.

- [inventorySetQuantities: proposed inventory update](Docs/set-quantities.md)
- [InventorySetQuantitiesInput: request fields](Docs/set-input.md)
- [InventoryQuantityInput: per-item fields](Docs/quantity-input.md)
- [inventoryItems: find inventory items using SKU](Docs/items.md)
- [locations: look up Shopify locations](Docs/locations.md)
- [inventoryActivate: inventory item activation at a location](Docs/activation.md)
- [ProductVariant: customer-facing availability](Docs/storefront-variant.md)
- [metafieldsSet: possible storage for custom availability data](Docs/metafields.md)

The saved mutation prose/examples contain legacy concurrency-field wording that differs from the saved input reference. This discrepancy remains a validation item; no live Shopify schema or store validation has been performed.

## Proposed mapping

- `AvailableQuantity` could supply `quantity`, after its business meaning is confirmed.
- `SKU` could locate a Shopify `inventoryItemId`; actual store matches are needed.
- `locationId` requires a location rule that is absent from the source payload.
- Dates, availability messages, and the other source fields require further decisions.

## Files for the framework

- [Target-reference.json](Target-reference.json): structured candidate operations, requirements, source URLs, and research notes.
- [Docs/manifest.json](Docs/manifest.json): original source URLs, retrieval date, and checksums for the saved pages.

Add your target requirements and store-specific notes in this Shopify folder. Give clarifications in Copilot chat and rerun the mapping workflow to incorporate them.

Original reference: [Shopify inventorySetQuantities documentation](https://shopify.dev/docs/api/admin-graphql/latest/mutations/inventorySetQuantities).
