---
title: InventorySetQuantitiesInput - GraphQL Admin
description: The input fields required to set inventory quantities.
api_version: 2026-07
source_url:
  html: >-
    https://shopify.dev/docs/api/admin-graphql/latest/input-objects/InventorySetQuantitiesInput
  md: >-
    https://shopify.dev/docs/api/admin-graphql/latest/input-objects/InventorySetQuantitiesInput.md
api_name: admin
api_type: graphql
type: input-object
---

# Inventory​Set​Quantities​Input

input\_object

The input fields required to set inventory quantities.

## Fields

* name

  [String!](https://shopify.dev/docs/api/admin-graphql/latest/scalars/String)

  non-null

  The name of quantities to be changed. The only accepted values are: `available` or `on_hand`.

* quantities

  [\[Inventory​Quantity​Input!\]!](https://shopify.dev/docs/api/admin-graphql/latest/input-objects/InventoryQuantityInput)

  required

  The values to which each quantities will be set.

* reason

  [String!](https://shopify.dev/docs/api/admin-graphql/latest/scalars/String)

  non-null

  The reason for the quantity changes. The value must be one of the [possible reasons](https://shopify.dev/docs/apps/fulfillment/inventory-management-apps/quantities-states#set-inventory-quantities-on-hand).

* reference​Document​Uri

  [String](https://shopify.dev/docs/api/admin-graphql/latest/scalars/String)

  A URI that represents why the inventory change happened, identifying the source system and document that caused this adjustment. Enables complete audit trails and brand visibility in Shopify admin inventory history.

  Preferred format - Global ID (GID): gid://\[your-app-name]/\[entity-type]/\[id]

  Examples:

  * gid://warehouse-app/PurchaseOrder/PO-2024-001 (stock received)
  * gid://3pl-system/CycleCount/CC-2024-0125 (cycle count adjustment)
  * gid://pos-app/Transaction/TXN-98765 (in-store sale)
  * gid://erp-connector/SyncJob/SYNC-2024-11-21-001 (ERP sync)
  * gid://shopify/Order/1234567890 (Shopify order reference)

  Benefits: Your app name appears directly in merchant inventory history, reducing support tickets and providing clear audit trails for compliance.

  Alternative formats (also supported): <https://myapp.com/documents/12345>, custom-scheme://identifier

  Requirements: Valid URI with scheme and content. For GID format, all components (app, entity, id) must be present.

***

## Mutations using this input

* [inventory​Set​Quantities.input](https://shopify.dev/docs/api/admin-graphql/latest/mutations/inventorySetQuantities#arguments-input)

  ARGUMENT

***

## Map

### Mutations using this input

* [inventory​Set​Quantities.input](https://shopify.dev/docs/api/admin-graphql/latest/mutations/inventorySetQuantities#arguments-input)
