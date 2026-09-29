---
title: inventoryActivate - GraphQL Admin
description: >-
  Activates an inventory item at a
  [`Location`](https://shopify.dev/docs/api/admin-graphql/latest/objects/Location)
  by creating an
  [`InventoryLevel`](https://shopify.dev/docs/api/admin-graphql/latest/objects/InventoryLevel)

  that tracks stock quantities. This enables you to manage inventory for a
  [`ProductVariant`](https://shopify.dev/docs/api/admin-graphql/latest/objects/ProductVariant)

  at the specified location.


  When you activate an inventory item, you can set its initial quantities. The
  [`available`](https://shopify.dev/docs/api/admin-graphql/latest/mutations/inventoryActivate#arguments-available)

  argument sets the quantity that's available for sale.
  [`onHand`](https://shopify.dev/docs/api/admin-graphql/latest/mutations/inventoryActivate#arguments-onHand)

  argument sets the total physical quantity at the location. If you don't

  specify quantities, then `available` and `onHand` default to zero.


  > Caution:

  > As of version `2026-01`, this mutation supports an optional idempotency key
  using the `@idempotent` directive.

  > As of version `2026-04`, the idempotency key is required and must be
  provided using the `@idempotent` directive.

  > For more information, see the [idempotency
  documentation](https://shopify.dev/docs/api/usage/idempotent-requests).


  Learn more about [managing inventory quantities and
  states](https://shopify.dev/docs/apps/build/orders-fulfillment/inventory-management-apps/manage-quantities-states).
api_version: 2026-07
source_url:
  html: >-
    https://shopify.dev/docs/api/admin-graphql/latest/mutations/inventoryActivate
  md: >-
    https://shopify.dev/docs/api/admin-graphql/latest/mutations/inventoryActivate.md
api_name: admin
api_type: graphql
type: mutation
---

# inventory​Activate

mutation

Requires `write_inventory` access scope. Also: The user must have a permission to activate an inventory item.

Activates an inventory item at a [`Location`](https://shopify.dev/docs/api/admin-graphql/latest/objects/Location) by creating an [`InventoryLevel`](https://shopify.dev/docs/api/admin-graphql/latest/objects/InventoryLevel) that tracks stock quantities. This enables you to manage inventory for a [`ProductVariant`](https://shopify.dev/docs/api/admin-graphql/latest/objects/ProductVariant) at the specified location.

When you activate an inventory item, you can set its initial quantities. The [`available`](https://shopify.dev/docs/api/admin-graphql/latest/mutations/inventoryActivate#arguments-available) argument sets the quantity that's available for sale. [`onHand`](https://shopify.dev/docs/api/admin-graphql/latest/mutations/inventoryActivate#arguments-onHand) argument sets the total physical quantity at the location. If you don't specify quantities, then `available` and `onHand` default to zero.

***

**Caution:** As of version \<code>2026-01\</code>, this mutation supports an optional idempotency key using the \<code>@idempotent\</code> directive. As of version \<code>2026-04\</code>, the idempotency key is required and must be provided using the \<code>@idempotent\</code> directive. For more information, see the \<a href="https://shopify.dev/docs/api/usage/idempotent-requests">idempotency documentation\</a>.

***

Learn more about [managing inventory quantities and states](https://shopify.dev/docs/apps/build/orders-fulfillment/inventory-management-apps/manage-quantities-states).

## Arguments

* available

  [Int](https://shopify.dev/docs/api/admin-graphql/latest/scalars/Int)

  The initial available quantity of the inventory item being activated at the location.

* inventory​Item​Id

  [ID!](https://shopify.dev/docs/api/admin-graphql/latest/scalars/ID)

  required

  The ID of the inventory item to activate.

* location​Id

  [ID!](https://shopify.dev/docs/api/admin-graphql/latest/scalars/ID)

  required

  The ID of the location of the inventory item being activated.

* on​Hand

  [Int](https://shopify.dev/docs/api/admin-graphql/latest/scalars/Int)

  The initial on\_hand quantity of the inventory item being activated at the location.

* stock​At​Legacy​Location

  [Boolean](https://shopify.dev/docs/api/admin-graphql/latest/scalars/Boolean)

  DeprecatedDefault:false

***

## Inventory​Activate​Payload returns

* inventory​Level

  [Inventory​Level](https://shopify.dev/docs/api/admin-graphql/latest/objects/InventoryLevel)

  The inventory level that was activated.

* user​Errors

  [\[User​Error!\]!](https://shopify.dev/docs/api/admin-graphql/latest/objects/UserError)

  non-null

  The list of errors that occurred from executing the mutation.

***

## Examples

* ### Activate an inventory item at a location with an initial available quantity

  #### Query

  ```graphql
  mutation ActivateInventoryItem($inventoryItemId: ID!, $locationId: ID!, $available: Int) {
    inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId, available: $available) {
      inventoryLevel {
        id
        quantities(names: ["available"]) {
          name
          quantity
        }
        item {
          id
        }
        location {
          id
        }
      }
    }
  }
  ```

  #### Variables

  ```json
  {
    "inventoryItemId": "gid://shopify/InventoryItem/43729076",
    "locationId": "gid://shopify/Location/346779380",
    "available": 42
  }
  ```

  #### cURL

  ```bash
  curl -X POST \
  https://your-development-store.myshopify.com/admin/api/2026-07/graphql.json \
  -H 'Content-Type: application/json' \
  -H 'X-Shopify-Access-Token: {access_token}' \
  -d '{
  "query": "mutation ActivateInventoryItem($inventoryItemId: ID!, $locationId: ID!, $available: Int) { inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId, available: $available) { inventoryLevel { id quantities(names: [\"available\"]) { name quantity } item { id } location { id } } } }",
   "variables": {
      "inventoryItemId": "gid://shopify/InventoryItem/43729076",
      "locationId": "gid://shopify/Location/346779380",
      "available": 42
    }
  }'
  ```

  #### React Router

  ```javascript
  import { authenticate } from "../shopify.server";

  export const loader = async ({request}) => {
    const { admin } = await authenticate.admin(request);
    const response = await admin.graphql(
      `#graphql
    mutation ActivateInventoryItem($inventoryItemId: ID!, $locationId: ID!, $available: Int) {
      inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId, available: $available) {
        inventoryLevel {
          id
          quantities(names: ["available"]) {
            name
            quantity
          }
          item {
            id
          }
          location {
            id
          }
        }
      }
    }`,
    {
      variables: {
          "inventoryItemId": "gid://shopify/InventoryItem/43729076",
          "locationId": "gid://shopify/Location/346779380",
          "available": 42
      },
    },
    );
    const json = await response.json();
    return json.data;
  }
  ```

  #### Ruby

  ```ruby
  session = ShopifyAPI::Auth::Session.new(
    shop: "your-development-store.myshopify.com",
    access_token: access_token
  )
  client = ShopifyAPI::Clients::Graphql::Admin.new(
    session: session
  )

  query = <<~QUERY
    mutation ActivateInventoryItem($inventoryItemId: ID!, $locationId: ID!, $available: Int) {
      inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId, available: $available) {
        inventoryLevel {
          id
          quantities(names: ["available"]) {
            name
            quantity
          }
          item {
            id
          }
          location {
            id
          }
        }
      }
    }
  QUERY

  variables = {
    "inventoryItemId": "gid://shopify/InventoryItem/43729076",
    "locationId": "gid://shopify/Location/346779380",
    "available": 42
  }

  response = client.query(query: query, variables: variables)
  ```

  #### Node.js

  ```javascript
  const client = new shopify.clients.Graphql({session});
  const data = await client.query({
    data: {
      "query": `mutation ActivateInventoryItem($inventoryItemId: ID!, $locationId: ID!, $available: Int) {
        inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId, available: $available) {
          inventoryLevel {
            id
            quantities(names: ["available"]) {
              name
              quantity
            }
            item {
              id
            }
            location {
              id
            }
          }
        }
      }`,
      "variables": {
          "inventoryItemId": "gid://shopify/InventoryItem/43729076",
          "locationId": "gid://shopify/Location/346779380",
          "available": 42
      },
    },
  });
  ```

  #### Shopify CLI

  ```bash
  shopify app execute \
  --query \
  'mutation ActivateInventoryItem($inventoryItemId: ID!, $locationId: ID!, $available: Int) {
    inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId, available: $available) {
      inventoryLevel {
        id
        quantities(names: ["available"]) {
          name
          quantity
        }
        item {
          id
        }
        location {
          id
        }
      }
    }
  }' \
  --variables \
  '{
    "inventoryItemId": "gid://shopify/InventoryItem/43729076",
    "locationId": "gid://shopify/Location/346779380",
    "available": 42
  }'
  ```

  #### Direct API Access

  ```javascript
  const response = await fetch('shopify:admin/api/2026-07/graphql.json', {
    method: 'POST',
    body: JSON.stringify({
      query: `
        mutation ActivateInventoryItem($inventoryItemId: ID!, $locationId: ID!, $available: Int) {
          inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId, available: $available) {
            inventoryLevel {
              id
              quantities(names: ["available"]) {
                name
                quantity
              }
              item {
                id
              }
              location {
                id
              }
            }
          }
        }
      `,
      variables: {
          "inventoryItemId": "gid://shopify/InventoryItem/43729076",
          "locationId": "gid://shopify/Location/346779380",
          "available": 42
      },
    }),
  });

  const { data } = await response.json();
  console.log(data);
  ```

  #### Response

  ```json
  {
    "inventoryActivate": {
      "inventoryLevel": {
        "id": "gid://shopify/InventoryLevel/523463154?inventory_item_id=43729076",
        "quantities": [
          {
            "name": "available",
            "quantity": 42
          }
        ],
        "item": {
          "id": "gid://shopify/InventoryItem/43729076"
        },
        "location": {
          "id": "gid://shopify/Location/346779380"
        }
      }
    }
  }
  ```

* ### Activate an inventory item at a location with an initial available quantity

  #### Query

  ```graphql
  mutation ActivateInventoryItem($inventoryItemId: ID!, $locationId: ID!, $available: Int, $idempotencyKey: String!) {
    inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId, available: $available) @idempotent(key: $idempotencyKey) {
      inventoryLevel {
        id
        quantities(names: ["available"]) {
          name
          quantity
        }
        item {
          id
        }
        location {
          id
        }
      }
    }
  }
  ```

  #### Variables

  ```json
  {
    "inventoryItemId": "gid://shopify/InventoryItem/43729076",
    "locationId": "gid://shopify/Location/346779380",
    "available": 42,
    "idempotencyKey": "6c423d89-5ddd-42b5-a9c5-b4af92657453"
  }
  ```

  #### cURL

  ```bash
  curl -X POST \
  https://your-development-store.myshopify.com/admin/api/2026-07/graphql.json \
  -H 'Content-Type: application/json' \
  -H 'X-Shopify-Access-Token: {access_token}' \
  -d '{
  "query": "mutation ActivateInventoryItem($inventoryItemId: ID!, $locationId: ID!, $available: Int, $idempotencyKey: String!) { inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId, available: $available) @idempotent(key: $idempotencyKey) { inventoryLevel { id quantities(names: [\"available\"]) { name quantity } item { id } location { id } } } }",
   "variables": {
      "inventoryItemId": "gid://shopify/InventoryItem/43729076",
      "locationId": "gid://shopify/Location/346779380",
      "available": 42,
      "idempotencyKey": "6c423d89-5ddd-42b5-a9c5-b4af92657453"
    }
  }'
  ```

  #### React Router

  ```javascript
  import { authenticate } from "../shopify.server";

  export const loader = async ({request}) => {
    const { admin } = await authenticate.admin(request);
    const response = await admin.graphql(
      `#graphql
    mutation ActivateInventoryItem($inventoryItemId: ID!, $locationId: ID!, $available: Int, $idempotencyKey: String!) {
      inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId, available: $available) @idempotent(key: $idempotencyKey) {
        inventoryLevel {
          id
          quantities(names: ["available"]) {
            name
            quantity
          }
          item {
            id
          }
          location {
            id
          }
        }
      }
    }`,
    {
      variables: {
          "inventoryItemId": "gid://shopify/InventoryItem/43729076",
          "locationId": "gid://shopify/Location/346779380",
          "available": 42,
          "idempotencyKey": "6c423d89-5ddd-42b5-a9c5-b4af92657453"
      },
    },
    );
    const json = await response.json();
    return json.data;
  }
  ```

  #### Ruby

  ```ruby
  session = ShopifyAPI::Auth::Session.new(
    shop: "your-development-store.myshopify.com",
    access_token: access_token
  )
  client = ShopifyAPI::Clients::Graphql::Admin.new(
    session: session
  )

  query = <<~QUERY
    mutation ActivateInventoryItem($inventoryItemId: ID!, $locationId: ID!, $available: Int, $idempotencyKey: String!) {
      inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId, available: $available) @idempotent(key: $idempotencyKey) {
        inventoryLevel {
          id
          quantities(names: ["available"]) {
            name
            quantity
          }
          item {
            id
          }
          location {
            id
          }
        }
      }
    }
  QUERY

  variables = {
    "inventoryItemId": "gid://shopify/InventoryItem/43729076",
    "locationId": "gid://shopify/Location/346779380",
    "available": 42,
    "idempotencyKey": "6c423d89-5ddd-42b5-a9c5-b4af92657453"
  }

  response = client.query(query: query, variables: variables)
  ```

  #### Node.js

  ```javascript
  const client = new shopify.clients.Graphql({session});
  const data = await client.query({
    data: {
      "query": `mutation ActivateInventoryItem($inventoryItemId: ID!, $locationId: ID!, $available: Int, $idempotencyKey: String!) {
        inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId, available: $available) @idempotent(key: $idempotencyKey) {
          inventoryLevel {
            id
            quantities(names: ["available"]) {
              name
              quantity
            }
            item {
              id
            }
            location {
              id
            }
          }
        }
      }`,
      "variables": {
          "inventoryItemId": "gid://shopify/InventoryItem/43729076",
          "locationId": "gid://shopify/Location/346779380",
          "available": 42,
          "idempotencyKey": "6c423d89-5ddd-42b5-a9c5-b4af92657453"
      },
    },
  });
  ```

  #### Shopify CLI

  ```bash
  shopify app execute \
  --query \
  'mutation ActivateInventoryItem($inventoryItemId: ID!, $locationId: ID!, $available: Int, $idempotencyKey: String!) {
    inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId, available: $available) @idempotent(key: $idempotencyKey) {
      inventoryLevel {
        id
        quantities(names: ["available"]) {
          name
          quantity
        }
        item {
          id
        }
        location {
          id
        }
      }
    }
  }' \
  --variables \
  '{
    "inventoryItemId": "gid://shopify/InventoryItem/43729076",
    "locationId": "gid://shopify/Location/346779380",
    "available": 42,
    "idempotencyKey": "6c423d89-5ddd-42b5-a9c5-b4af92657453"
  }'
  ```

  #### Direct API Access

  ```javascript
  const response = await fetch('shopify:admin/api/2026-07/graphql.json', {
    method: 'POST',
    body: JSON.stringify({
      query: `
        mutation ActivateInventoryItem($inventoryItemId: ID!, $locationId: ID!, $available: Int, $idempotencyKey: String!) {
          inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId, available: $available) @idempotent(key: $idempotencyKey) {
            inventoryLevel {
              id
              quantities(names: ["available"]) {
                name
                quantity
              }
              item {
                id
              }
              location {
                id
              }
            }
          }
        }
      `,
      variables: {
          "inventoryItemId": "gid://shopify/InventoryItem/43729076",
          "locationId": "gid://shopify/Location/346779380",
          "available": 42,
          "idempotencyKey": "6c423d89-5ddd-42b5-a9c5-b4af92657453"
      },
    }),
  });

  const { data } = await response.json();
  console.log(data);
  ```

  #### Response

  ```json
  {
    "inventoryActivate": {
      "inventoryLevel": {
        "id": "gid://shopify/InventoryLevel/523463154?inventory_item_id=43729076",
        "quantities": [
          {
            "name": "available",
            "quantity": 42
          }
        ],
        "item": {
          "id": "gid://shopify/InventoryItem/43729076"
        },
        "location": {
          "id": "gid://shopify/Location/346779380"
        }
      }
    }
  }
  ```

* ### Activate an inventory item at a location without setting an available quantity

  #### Query

  ```graphql
  mutation ActivateInventoryItem($inventoryItemId: ID!, $locationId: ID!) {
    inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId) {
      inventoryLevel {
        id
        quantities(names: ["available"]) {
          name
          quantity
        }
        item {
          id
        }
        location {
          id
        }
      }
    }
  }
  ```

  #### Variables

  ```json
  {
    "inventoryItemId": "gid://shopify/InventoryItem/43729076",
    "locationId": "gid://shopify/Location/346779380"
  }
  ```

  #### cURL

  ```bash
  curl -X POST \
  https://your-development-store.myshopify.com/admin/api/2026-07/graphql.json \
  -H 'Content-Type: application/json' \
  -H 'X-Shopify-Access-Token: {access_token}' \
  -d '{
  "query": "mutation ActivateInventoryItem($inventoryItemId: ID!, $locationId: ID!) { inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId) { inventoryLevel { id quantities(names: [\"available\"]) { name quantity } item { id } location { id } } } }",
   "variables": {
      "inventoryItemId": "gid://shopify/InventoryItem/43729076",
      "locationId": "gid://shopify/Location/346779380"
    }
  }'
  ```

  #### React Router

  ```javascript
  import { authenticate } from "../shopify.server";

  export const loader = async ({request}) => {
    const { admin } = await authenticate.admin(request);
    const response = await admin.graphql(
      `#graphql
    mutation ActivateInventoryItem($inventoryItemId: ID!, $locationId: ID!) {
      inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId) {
        inventoryLevel {
          id
          quantities(names: ["available"]) {
            name
            quantity
          }
          item {
            id
          }
          location {
            id
          }
        }
      }
    }`,
    {
      variables: {
          "inventoryItemId": "gid://shopify/InventoryItem/43729076",
          "locationId": "gid://shopify/Location/346779380"
      },
    },
    );
    const json = await response.json();
    return json.data;
  }
  ```

  #### Ruby

  ```ruby
  session = ShopifyAPI::Auth::Session.new(
    shop: "your-development-store.myshopify.com",
    access_token: access_token
  )
  client = ShopifyAPI::Clients::Graphql::Admin.new(
    session: session
  )

  query = <<~QUERY
    mutation ActivateInventoryItem($inventoryItemId: ID!, $locationId: ID!) {
      inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId) {
        inventoryLevel {
          id
          quantities(names: ["available"]) {
            name
            quantity
          }
          item {
            id
          }
          location {
            id
          }
        }
      }
    }
  QUERY

  variables = {
    "inventoryItemId": "gid://shopify/InventoryItem/43729076",
    "locationId": "gid://shopify/Location/346779380"
  }

  response = client.query(query: query, variables: variables)
  ```

  #### Node.js

  ```javascript
  const client = new shopify.clients.Graphql({session});
  const data = await client.query({
    data: {
      "query": `mutation ActivateInventoryItem($inventoryItemId: ID!, $locationId: ID!) {
        inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId) {
          inventoryLevel {
            id
            quantities(names: ["available"]) {
              name
              quantity
            }
            item {
              id
            }
            location {
              id
            }
          }
        }
      }`,
      "variables": {
          "inventoryItemId": "gid://shopify/InventoryItem/43729076",
          "locationId": "gid://shopify/Location/346779380"
      },
    },
  });
  ```

  #### Shopify CLI

  ```bash
  shopify app execute \
  --query \
  'mutation ActivateInventoryItem($inventoryItemId: ID!, $locationId: ID!) {
    inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId) {
      inventoryLevel {
        id
        quantities(names: ["available"]) {
          name
          quantity
        }
        item {
          id
        }
        location {
          id
        }
      }
    }
  }' \
  --variables \
  '{
    "inventoryItemId": "gid://shopify/InventoryItem/43729076",
    "locationId": "gid://shopify/Location/346779380"
  }'
  ```

  #### Direct API Access

  ```javascript
  const response = await fetch('shopify:admin/api/2026-07/graphql.json', {
    method: 'POST',
    body: JSON.stringify({
      query: `
        mutation ActivateInventoryItem($inventoryItemId: ID!, $locationId: ID!) {
          inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId) {
            inventoryLevel {
              id
              quantities(names: ["available"]) {
                name
                quantity
              }
              item {
                id
              }
              location {
                id
              }
            }
          }
        }
      `,
      variables: {
          "inventoryItemId": "gid://shopify/InventoryItem/43729076",
          "locationId": "gid://shopify/Location/346779380"
      },
    }),
  });

  const { data } = await response.json();
  console.log(data);
  ```

  #### Response

  ```json
  {
    "inventoryActivate": {
      "inventoryLevel": {
        "id": "gid://shopify/InventoryLevel/523463154?inventory_item_id=43729076",
        "quantities": [
          {
            "name": "available",
            "quantity": 0
          }
        ],
        "item": {
          "id": "gid://shopify/InventoryItem/43729076"
        },
        "location": {
          "id": "gid://shopify/Location/346779380"
        }
      }
    }
  }
  ```

* ### Activate an inventory item at a location without setting an available quantity

  #### Query

  ```graphql
  mutation ActivateInventoryItem($inventoryItemId: ID!, $locationId: ID!, $idempotencyKey: String!) {
    inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId) @idempotent(key: $idempotencyKey) {
      inventoryLevel {
        id
        quantities(names: ["available"]) {
          name
          quantity
        }
        item {
          id
        }
        location {
          id
        }
      }
    }
  }
  ```

  #### Variables

  ```json
  {
    "inventoryItemId": "gid://shopify/InventoryItem/43729076",
    "locationId": "gid://shopify/Location/346779380",
    "idempotencyKey": "394d9bb1-17cf-4fe9-8b42-b8d69fc4d2cc"
  }
  ```

  #### cURL

  ```bash
  curl -X POST \
  https://your-development-store.myshopify.com/admin/api/2026-07/graphql.json \
  -H 'Content-Type: application/json' \
  -H 'X-Shopify-Access-Token: {access_token}' \
  -d '{
  "query": "mutation ActivateInventoryItem($inventoryItemId: ID!, $locationId: ID!, $idempotencyKey: String!) { inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId) @idempotent(key: $idempotencyKey) { inventoryLevel { id quantities(names: [\"available\"]) { name quantity } item { id } location { id } } } }",
   "variables": {
      "inventoryItemId": "gid://shopify/InventoryItem/43729076",
      "locationId": "gid://shopify/Location/346779380",
      "idempotencyKey": "394d9bb1-17cf-4fe9-8b42-b8d69fc4d2cc"
    }
  }'
  ```

  #### React Router

  ```javascript
  import { authenticate } from "../shopify.server";

  export const loader = async ({request}) => {
    const { admin } = await authenticate.admin(request);
    const response = await admin.graphql(
      `#graphql
    mutation ActivateInventoryItem($inventoryItemId: ID!, $locationId: ID!, $idempotencyKey: String!) {
      inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId) @idempotent(key: $idempotencyKey) {
        inventoryLevel {
          id
          quantities(names: ["available"]) {
            name
            quantity
          }
          item {
            id
          }
          location {
            id
          }
        }
      }
    }`,
    {
      variables: {
          "inventoryItemId": "gid://shopify/InventoryItem/43729076",
          "locationId": "gid://shopify/Location/346779380",
          "idempotencyKey": "394d9bb1-17cf-4fe9-8b42-b8d69fc4d2cc"
      },
    },
    );
    const json = await response.json();
    return json.data;
  }
  ```

  #### Ruby

  ```ruby
  session = ShopifyAPI::Auth::Session.new(
    shop: "your-development-store.myshopify.com",
    access_token: access_token
  )
  client = ShopifyAPI::Clients::Graphql::Admin.new(
    session: session
  )

  query = <<~QUERY
    mutation ActivateInventoryItem($inventoryItemId: ID!, $locationId: ID!, $idempotencyKey: String!) {
      inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId) @idempotent(key: $idempotencyKey) {
        inventoryLevel {
          id
          quantities(names: ["available"]) {
            name
            quantity
          }
          item {
            id
          }
          location {
            id
          }
        }
      }
    }
  QUERY

  variables = {
    "inventoryItemId": "gid://shopify/InventoryItem/43729076",
    "locationId": "gid://shopify/Location/346779380",
    "idempotencyKey": "394d9bb1-17cf-4fe9-8b42-b8d69fc4d2cc"
  }

  response = client.query(query: query, variables: variables)
  ```

  #### Node.js

  ```javascript
  const client = new shopify.clients.Graphql({session});
  const data = await client.query({
    data: {
      "query": `mutation ActivateInventoryItem($inventoryItemId: ID!, $locationId: ID!, $idempotencyKey: String!) {
        inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId) @idempotent(key: $idempotencyKey) {
          inventoryLevel {
            id
            quantities(names: ["available"]) {
              name
              quantity
            }
            item {
              id
            }
            location {
              id
            }
          }
        }
      }`,
      "variables": {
          "inventoryItemId": "gid://shopify/InventoryItem/43729076",
          "locationId": "gid://shopify/Location/346779380",
          "idempotencyKey": "394d9bb1-17cf-4fe9-8b42-b8d69fc4d2cc"
      },
    },
  });
  ```

  #### Shopify CLI

  ```bash
  shopify app execute \
  --query \
  'mutation ActivateInventoryItem($inventoryItemId: ID!, $locationId: ID!, $idempotencyKey: String!) {
    inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId) @idempotent(key: $idempotencyKey) {
      inventoryLevel {
        id
        quantities(names: ["available"]) {
          name
          quantity
        }
        item {
          id
        }
        location {
          id
        }
      }
    }
  }' \
  --variables \
  '{
    "inventoryItemId": "gid://shopify/InventoryItem/43729076",
    "locationId": "gid://shopify/Location/346779380",
    "idempotencyKey": "394d9bb1-17cf-4fe9-8b42-b8d69fc4d2cc"
  }'
  ```

  #### Direct API Access

  ```javascript
  const response = await fetch('shopify:admin/api/2026-07/graphql.json', {
    method: 'POST',
    body: JSON.stringify({
      query: `
        mutation ActivateInventoryItem($inventoryItemId: ID!, $locationId: ID!, $idempotencyKey: String!) {
          inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId) @idempotent(key: $idempotencyKey) {
            inventoryLevel {
              id
              quantities(names: ["available"]) {
                name
                quantity
              }
              item {
                id
              }
              location {
                id
              }
            }
          }
        }
      `,
      variables: {
          "inventoryItemId": "gid://shopify/InventoryItem/43729076",
          "locationId": "gid://shopify/Location/346779380",
          "idempotencyKey": "394d9bb1-17cf-4fe9-8b42-b8d69fc4d2cc"
      },
    }),
  });

  const { data } = await response.json();
  console.log(data);
  ```

  #### Response

  ```json
  {
    "inventoryActivate": {
      "inventoryLevel": {
        "id": "gid://shopify/InventoryLevel/523463154?inventory_item_id=43729076",
        "quantities": [
          {
            "name": "available",
            "quantity": 0
          }
        ],
        "item": {
          "id": "gid://shopify/InventoryItem/43729076"
        },
        "location": {
          "id": "gid://shopify/Location/346779380"
        }
      }
    }
  }
  ```

* ### Connects an inventory item to a location

  #### Query

  ```graphql
  mutation InventoryActivate($inventoryItemId: ID!, $locationId: ID!) {
    inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId) {
      inventoryLevel {
        id
        canDeactivate
        createdAt
        item {
          id
        }
        location {
          id
        }
        quantities(names: ["available"]) {
          name
          quantity
        }
        updatedAt
      }
    }
  }
  ```

  #### Variables

  ```json
  {
    "inventoryItemId": "gid://shopify/InventoryItem/43729076",
    "locationId": "gid://shopify/Location/346779380"
  }
  ```

  #### cURL

  ```bash
  curl -X POST \
  https://your-development-store.myshopify.com/admin/api/2026-07/graphql.json \
  -H 'Content-Type: application/json' \
  -H 'X-Shopify-Access-Token: {access_token}' \
  -d '{
  "query": "mutation InventoryActivate($inventoryItemId: ID!, $locationId: ID!) { inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId) { inventoryLevel { id canDeactivate createdAt item { id } location { id } quantities(names: [\"available\"]) { name quantity } updatedAt } } }",
   "variables": {
      "inventoryItemId": "gid://shopify/InventoryItem/43729076",
      "locationId": "gid://shopify/Location/346779380"
    }
  }'
  ```

  #### React Router

  ```javascript
  import { authenticate } from "../shopify.server";

  export const loader = async ({request}) => {
    const { admin } = await authenticate.admin(request);
    const response = await admin.graphql(
      `#graphql
    mutation InventoryActivate($inventoryItemId: ID!, $locationId: ID!) {
      inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId) {
        inventoryLevel {
          id
          canDeactivate
          createdAt
          item {
            id
          }
          location {
            id
          }
          quantities(names: ["available"]) {
            name
            quantity
          }
          updatedAt
        }
      }
    }`,
    {
      variables: {
          "inventoryItemId": "gid://shopify/InventoryItem/43729076",
          "locationId": "gid://shopify/Location/346779380"
      },
    },
    );
    const json = await response.json();
    return json.data;
  }
  ```

  #### Ruby

  ```ruby
  session = ShopifyAPI::Auth::Session.new(
    shop: "your-development-store.myshopify.com",
    access_token: access_token
  )
  client = ShopifyAPI::Clients::Graphql::Admin.new(
    session: session
  )

  query = <<~QUERY
    mutation InventoryActivate($inventoryItemId: ID!, $locationId: ID!) {
      inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId) {
        inventoryLevel {
          id
          canDeactivate
          createdAt
          item {
            id
          }
          location {
            id
          }
          quantities(names: ["available"]) {
            name
            quantity
          }
          updatedAt
        }
      }
    }
  QUERY

  variables = {
    "inventoryItemId": "gid://shopify/InventoryItem/43729076",
    "locationId": "gid://shopify/Location/346779380"
  }

  response = client.query(query: query, variables: variables)
  ```

  #### Node.js

  ```javascript
  const client = new shopify.clients.Graphql({session});
  const data = await client.query({
    data: {
      "query": `mutation InventoryActivate($inventoryItemId: ID!, $locationId: ID!) {
        inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId) {
          inventoryLevel {
            id
            canDeactivate
            createdAt
            item {
              id
            }
            location {
              id
            }
            quantities(names: ["available"]) {
              name
              quantity
            }
            updatedAt
          }
        }
      }`,
      "variables": {
          "inventoryItemId": "gid://shopify/InventoryItem/43729076",
          "locationId": "gid://shopify/Location/346779380"
      },
    },
  });
  ```

  #### Shopify CLI

  ```bash
  shopify app execute \
  --query \
  'mutation InventoryActivate($inventoryItemId: ID!, $locationId: ID!) {
    inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId) {
      inventoryLevel {
        id
        canDeactivate
        createdAt
        item {
          id
        }
        location {
          id
        }
        quantities(names: ["available"]) {
          name
          quantity
        }
        updatedAt
      }
    }
  }' \
  --variables \
  '{
    "inventoryItemId": "gid://shopify/InventoryItem/43729076",
    "locationId": "gid://shopify/Location/346779380"
  }'
  ```

  #### Direct API Access

  ```javascript
  const response = await fetch('shopify:admin/api/2026-07/graphql.json', {
    method: 'POST',
    body: JSON.stringify({
      query: `
        mutation InventoryActivate($inventoryItemId: ID!, $locationId: ID!) {
          inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId) {
            inventoryLevel {
              id
              canDeactivate
              createdAt
              item {
                id
              }
              location {
                id
              }
              quantities(names: ["available"]) {
                name
                quantity
              }
              updatedAt
            }
          }
        }
      `,
      variables: {
          "inventoryItemId": "gid://shopify/InventoryItem/43729076",
          "locationId": "gid://shopify/Location/346779380"
      },
    }),
  });

  const { data } = await response.json();
  console.log(data);
  ```

  #### Response

  ```json
  {
    "inventoryActivate": {
      "inventoryLevel": {
        "id": "gid://shopify/InventoryLevel/523463154?inventory_item_id=43729076",
        "canDeactivate": true,
        "createdAt": "2024-11-07T20:59:45Z",
        "item": {
          "id": "gid://shopify/InventoryItem/43729076"
        },
        "location": {
          "id": "gid://shopify/Location/346779380"
        },
        "quantities": [
          {
            "name": "available",
            "quantity": 0
          }
        ],
        "updatedAt": "2024-11-07T20:59:45Z"
      }
    }
  }
  ```

* ### Connects an inventory item to a location

  #### Query

  ```graphql
  mutation InventoryActivate($inventoryItemId: ID!, $locationId: ID!, $idempotencyKey: String!) {
    inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId) @idempotent(key: $idempotencyKey) {
      inventoryLevel {
        id
        canDeactivate
        createdAt
        item {
          id
        }
        location {
          id
        }
        quantities(names: ["available"]) {
          name
          quantity
        }
        updatedAt
      }
    }
  }
  ```

  #### Variables

  ```json
  {
    "inventoryItemId": "gid://shopify/InventoryItem/43729076",
    "locationId": "gid://shopify/Location/346779380",
    "idempotencyKey": "ae436231-e684-4622-8620-51c4f877ef11"
  }
  ```

  #### cURL

  ```bash
  curl -X POST \
  https://your-development-store.myshopify.com/admin/api/2026-07/graphql.json \
  -H 'Content-Type: application/json' \
  -H 'X-Shopify-Access-Token: {access_token}' \
  -d '{
  "query": "mutation InventoryActivate($inventoryItemId: ID!, $locationId: ID!, $idempotencyKey: String!) { inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId) @idempotent(key: $idempotencyKey) { inventoryLevel { id canDeactivate createdAt item { id } location { id } quantities(names: [\"available\"]) { name quantity } updatedAt } } }",
   "variables": {
      "inventoryItemId": "gid://shopify/InventoryItem/43729076",
      "locationId": "gid://shopify/Location/346779380",
      "idempotencyKey": "ae436231-e684-4622-8620-51c4f877ef11"
    }
  }'
  ```

  #### React Router

  ```javascript
  import { authenticate } from "../shopify.server";

  export const loader = async ({request}) => {
    const { admin } = await authenticate.admin(request);
    const response = await admin.graphql(
      `#graphql
    mutation InventoryActivate($inventoryItemId: ID!, $locationId: ID!, $idempotencyKey: String!) {
      inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId) @idempotent(key: $idempotencyKey) {
        inventoryLevel {
          id
          canDeactivate
          createdAt
          item {
            id
          }
          location {
            id
          }
          quantities(names: ["available"]) {
            name
            quantity
          }
          updatedAt
        }
      }
    }`,
    {
      variables: {
          "inventoryItemId": "gid://shopify/InventoryItem/43729076",
          "locationId": "gid://shopify/Location/346779380",
          "idempotencyKey": "ae436231-e684-4622-8620-51c4f877ef11"
      },
    },
    );
    const json = await response.json();
    return json.data;
  }
  ```

  #### Ruby

  ```ruby
  session = ShopifyAPI::Auth::Session.new(
    shop: "your-development-store.myshopify.com",
    access_token: access_token
  )
  client = ShopifyAPI::Clients::Graphql::Admin.new(
    session: session
  )

  query = <<~QUERY
    mutation InventoryActivate($inventoryItemId: ID!, $locationId: ID!, $idempotencyKey: String!) {
      inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId) @idempotent(key: $idempotencyKey) {
        inventoryLevel {
          id
          canDeactivate
          createdAt
          item {
            id
          }
          location {
            id
          }
          quantities(names: ["available"]) {
            name
            quantity
          }
          updatedAt
        }
      }
    }
  QUERY

  variables = {
    "inventoryItemId": "gid://shopify/InventoryItem/43729076",
    "locationId": "gid://shopify/Location/346779380",
    "idempotencyKey": "ae436231-e684-4622-8620-51c4f877ef11"
  }

  response = client.query(query: query, variables: variables)
  ```

  #### Node.js

  ```javascript
  const client = new shopify.clients.Graphql({session});
  const data = await client.query({
    data: {
      "query": `mutation InventoryActivate($inventoryItemId: ID!, $locationId: ID!, $idempotencyKey: String!) {
        inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId) @idempotent(key: $idempotencyKey) {
          inventoryLevel {
            id
            canDeactivate
            createdAt
            item {
              id
            }
            location {
              id
            }
            quantities(names: ["available"]) {
              name
              quantity
            }
            updatedAt
          }
        }
      }`,
      "variables": {
          "inventoryItemId": "gid://shopify/InventoryItem/43729076",
          "locationId": "gid://shopify/Location/346779380",
          "idempotencyKey": "ae436231-e684-4622-8620-51c4f877ef11"
      },
    },
  });
  ```

  #### Shopify CLI

  ```bash
  shopify app execute \
  --query \
  'mutation InventoryActivate($inventoryItemId: ID!, $locationId: ID!, $idempotencyKey: String!) {
    inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId) @idempotent(key: $idempotencyKey) {
      inventoryLevel {
        id
        canDeactivate
        createdAt
        item {
          id
        }
        location {
          id
        }
        quantities(names: ["available"]) {
          name
          quantity
        }
        updatedAt
      }
    }
  }' \
  --variables \
  '{
    "inventoryItemId": "gid://shopify/InventoryItem/43729076",
    "locationId": "gid://shopify/Location/346779380",
    "idempotencyKey": "ae436231-e684-4622-8620-51c4f877ef11"
  }'
  ```

  #### Direct API Access

  ```javascript
  const response = await fetch('shopify:admin/api/2026-07/graphql.json', {
    method: 'POST',
    body: JSON.stringify({
      query: `
        mutation InventoryActivate($inventoryItemId: ID!, $locationId: ID!, $idempotencyKey: String!) {
          inventoryActivate(inventoryItemId: $inventoryItemId, locationId: $locationId) @idempotent(key: $idempotencyKey) {
            inventoryLevel {
              id
              canDeactivate
              createdAt
              item {
                id
              }
              location {
                id
              }
              quantities(names: ["available"]) {
                name
                quantity
              }
              updatedAt
            }
          }
        }
      `,
      variables: {
          "inventoryItemId": "gid://shopify/InventoryItem/43729076",
          "locationId": "gid://shopify/Location/346779380",
          "idempotencyKey": "ae436231-e684-4622-8620-51c4f877ef11"
      },
    }),
  });

  const { data } = await response.json();
  console.log(data);
  ```

  #### Response

  ```json
  {
    "inventoryActivate": {
      "inventoryLevel": {
        "id": "gid://shopify/InventoryLevel/523463154?inventory_item_id=43729076",
        "canDeactivate": true,
        "createdAt": "2026-04-13T20:50:01Z",
        "item": {
          "id": "gid://shopify/InventoryItem/43729076"
        },
        "location": {
          "id": "gid://shopify/Location/346779380"
        },
        "quantities": [
          {
            "name": "available",
            "quantity": 0
          }
        ],
        "updatedAt": "2026-04-13T20:50:01Z"
      }
    }
  }
  ```

* ### inventoryActivate reference
