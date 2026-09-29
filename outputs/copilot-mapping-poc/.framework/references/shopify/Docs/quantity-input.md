---
title: InventoryQuantityInput - GraphQL Admin
description: >-
  The input fields for the quantity to be set for an inventory item at a
  location.
api_version: 2026-07
source_url:
  html: >-
    https://shopify.dev/docs/api/admin-graphql/latest/input-objects/InventoryQuantityInput
  md: >-
    https://shopify.dev/docs/api/admin-graphql/latest/input-objects/InventoryQuantityInput.md
api_name: admin
api_type: graphql
type: input-object
---

# Inventory​Quantity​Input

input\_object

The input fields for the quantity to be set for an inventory item at a location.

## Fields

* change​From​Quantity

  [Int](https://shopify.dev/docs/api/admin-graphql/latest/scalars/Int)

  The quantity currently expected at this location, before setting the new quantity.

  This field enables a compare-and-swap (CAS) safety check. If the location’s current quantity doesn't match the value you provide, then the mutation fails with a `CHANGE_FROM_QUANTITY_STALE` error. This helps prevent unintended overwrites when the request is based on stale inventory data.

  To skip the CAS check, pass `null`. Use this only when your system is the source of truth for this inventory and you don’t need to protect against concurrent updates. This field is mandatory: you must explicitly pass in a value, even if that value is `null`, or the mutation returns an error.

  For more information, refer to the [compare and swap documentation](https://shopify.dev/docs/apps/build/orders-fulfillment/inventory-management-apps/manage-quantities-states#compare-and-swap).

* inventory​Item​Id

  [ID!](https://shopify.dev/docs/api/admin-graphql/latest/scalars/ID)

  non-null

  Specifies the inventory item to which the quantity will be set.

* location​Id

  [ID!](https://shopify.dev/docs/api/admin-graphql/latest/scalars/ID)

  non-null

  Specifies the location at which the quantity will be set.

* quantity

  [Int!](https://shopify.dev/docs/api/admin-graphql/latest/scalars/Int)

  non-null

  The quantity to which the inventory quantity will be set.

***

## Input objects using this input

* [Inventory​Set​Quantities​Input.quantities](https://shopify.dev/docs/api/admin-graphql/latest/input-objects/InventorySetQuantitiesInput#fields-quantities)

  INPUT OBJECT

  The input fields required to set inventory quantities.

***

## Map

### Input objects using this input

* [Inventory​Set​Quantities​Input.quantities](https://shopify.dev/docs/api/admin-graphql/latest/input-objects/InventorySetQuantitiesInput#fields-quantities)
