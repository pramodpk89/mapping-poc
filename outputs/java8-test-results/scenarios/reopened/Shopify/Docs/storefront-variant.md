---
title: ProductVariant - Storefront API
description: >-
  A specific version of a

  [product](/docs/api/storefront/2026-07/objects/Product)

  available for sale, differentiated by options like size or color. For example,
  a

  small blue t-shirt and a large blue t-shirt are separate variants of the same

  product. For more information, see the docs on [Shopify's product
  model](https://shopify.dev/docs/apps/build/product-merchandising/products-and-collections).


  For products with quantity rules, variants enforce minimum, maximum, and
  increment constraints on purchases.


  Variants also support subscriptions and pre-orders through [selling plan
  allocations](/docs/api/storefront/2026-07/objects/SellingPlanAllocation)

  objects, bundle configurations through [product variant
  components](/docs/api/storefront/2026-07/objects/ProductVariantComponent)

  objects, and [shop pay installments
  pricing](/docs/api/storefront/2026-07/objects/ShopPayInstallmentsPricing)

  for flexible payment options.
api_version: 2026-07
source_url:
  html: 'https://shopify.dev/docs/api/storefront/latest/objects/ProductVariant'
  md: 'https://shopify.dev/docs/api/storefront/latest/objects/ProductVariant.md'
api_name: storefront
api_type: graphql
type: object
---

# Product​Variant

object

Requires `unauthenticated_read_product_listings` access scope.

A specific version of a [product](https://shopify.dev/docs/api/storefront/2026-07/objects/Product) available for sale, differentiated by options like size or color. For example, a small blue t-shirt and a large blue t-shirt are separate variants of the same product. For more information, see the docs on [Shopify's product model](https://shopify.dev/docs/apps/build/product-merchandising/products-and-collections).

For products with quantity rules, variants enforce minimum, maximum, and increment constraints on purchases.

Variants also support subscriptions and pre-orders through [selling plan allocations](https://shopify.dev/docs/api/storefront/2026-07/objects/SellingPlanAllocation) objects, bundle configurations through [product variant components](https://shopify.dev/docs/api/storefront/2026-07/objects/ProductVariantComponent) objects, and [shop pay installments pricing](https://shopify.dev/docs/api/storefront/2026-07/objects/ShopPayInstallmentsPricing) for flexible payment options.

## Fields

* available​For​Sale

  [Boolean!](https://shopify.dev/docs/api/storefront/latest/scalars/Boolean)

  non-null

  Indicates if the product variant is available for sale.

* barcode

  [String](https://shopify.dev/docs/api/storefront/latest/scalars/String)

  The barcode (for example, ISBN, UPC, or GTIN) associated with the variant.

* compare​At​Price

  [Money​V2](https://shopify.dev/docs/api/storefront/latest/objects/MoneyV2)

  The compare at price of the variant. This can be used to mark a variant as on sale, when `compareAtPrice` is higher than `price`.

* components

  [Product​Variant​Component​Connection!](https://shopify.dev/docs/api/storefront/latest/connections/ProductVariantComponentConnection)

  non-null

  List of bundles components included in the variant considering only fixed bundles.

  * after

    [String](https://shopify.dev/docs/api/storefront/latest/scalars/String)

    ### Arguments

    Returns the elements that come after the specified cursor.

  * before

    [String](https://shopify.dev/docs/api/storefront/latest/scalars/String)

    Returns the elements that come before the specified cursor.

  * first

    [Int](https://shopify.dev/docs/api/storefront/latest/scalars/Int)

    Returns up to the first `n` elements from the list.

  * last

    [Int](https://shopify.dev/docs/api/storefront/latest/scalars/Int)

    Returns up to the last `n` elements from the list.

  ***

* currently​Not​In​Stock

  [Boolean!](https://shopify.dev/docs/api/storefront/latest/scalars/Boolean)

  non-null

  Whether a product is out of stock but still available for purchase (used for backorders).

* grouped​By

  [Product​Variant​Connection!](https://shopify.dev/docs/api/storefront/latest/connections/ProductVariantConnection)

  non-null

  List of bundles that include this variant considering only fixed bundles.

  * after

    [String](https://shopify.dev/docs/api/storefront/latest/scalars/String)

    ### Arguments

    Returns the elements that come after the specified cursor.

  * before

    [String](https://shopify.dev/docs/api/storefront/latest/scalars/String)

    Returns the elements that come before the specified cursor.

  * first

    [Int](https://shopify.dev/docs/api/storefront/latest/scalars/Int)

    Returns up to the first `n` elements from the list.

  * last

    [Int](https://shopify.dev/docs/api/storefront/latest/scalars/Int)

    Returns up to the last `n` elements from the list.

  ***

* id

  [ID!](https://shopify.dev/docs/api/storefront/latest/scalars/ID)

  non-null

  A globally-unique ID.

* image

  [Image](https://shopify.dev/docs/api/storefront/latest/objects/Image)

  Image associated with the product variant. This field falls back to the product image if no image is available.

* metafield

  [Metafield](https://shopify.dev/docs/api/storefront/latest/objects/Metafield)

  Token access required

  A [custom field](https://shopify.dev/docs/apps/build/custom-data), including its `namespace` and `key`, that's associated with a Shopify resource for the purposes of adding and storing additional information.

  * key

    [String!](https://shopify.dev/docs/api/storefront/latest/scalars/String)

    required

    ### Arguments

    The identifier for the metafield.

  * namespace

    [String](https://shopify.dev/docs/api/storefront/latest/scalars/String)

    The container the metafield belongs to. If omitted, the app-reserved namespace will be used.

  ***

* metafields

  [\[Metafield\]!](https://shopify.dev/docs/api/storefront/latest/objects/Metafield)

  non-null Token access required

  A list of [custom fields](https://shopify.dev/docs/apps/build/custom-data) that a merchant associates with a Shopify resource.

  * identifiers

    [\[Has​Metafields​Identifier!\]!](https://shopify.dev/docs/api/storefront/latest/input-objects/HasMetafieldsIdentifier)

    required

    ### Arguments

    The list of metafields to retrieve by namespace and key.

    The input must not contain more than `250` values.

  ***

* price

  [Money​V2!](https://shopify.dev/docs/api/storefront/latest/objects/MoneyV2)

  non-null

  The product variant’s price.

* product

  [Product!](https://shopify.dev/docs/api/storefront/latest/objects/Product)

  non-null

  The product object that the product variant belongs to.

* quantity​Available

  [Int](https://shopify.dev/docs/api/storefront/latest/scalars/Int)

  Token access required

  The total sellable quantity of the variant for online sales channels.

* quantity​Price​Breaks

  [Quantity​Price​Break​Connection!](https://shopify.dev/docs/api/storefront/latest/connections/QuantityPriceBreakConnection)

  non-null

  A list of quantity breaks for the product variant.

  * after

    [String](https://shopify.dev/docs/api/storefront/latest/scalars/String)

    ### Arguments

    Returns the elements that come after the specified cursor.

  * before

    [String](https://shopify.dev/docs/api/storefront/latest/scalars/String)

    Returns the elements that come before the specified cursor.

  * first

    [Int](https://shopify.dev/docs/api/storefront/latest/scalars/Int)

    Returns up to the first `n` elements from the list.

  * last

    [Int](https://shopify.dev/docs/api/storefront/latest/scalars/Int)

    Returns up to the last `n` elements from the list.

  ***

* quantity​Rule

  [Quantity​Rule!](https://shopify.dev/docs/api/storefront/latest/objects/QuantityRule)

  non-null

  The quantity rule for the product variant in a given context.

* requires​Components

  [Boolean!](https://shopify.dev/docs/api/storefront/latest/scalars/Boolean)

  non-null

  Whether a product variant requires components. The default value is `false`. If `true`, then the product variant can only be purchased as a parent bundle with components.

* requires​Shipping

  [Boolean!](https://shopify.dev/docs/api/storefront/latest/scalars/Boolean)

  non-null

  Whether a customer needs to provide a shipping address when placing an order for the product variant.

* selected​Options

  [\[Selected​Option!\]!](https://shopify.dev/docs/api/storefront/latest/objects/SelectedOption)

  non-null

  List of product options applied to the variant.

* selling​Plan​Allocations

  [Selling​Plan​Allocation​Connection!](https://shopify.dev/docs/api/storefront/latest/connections/SellingPlanAllocationConnection)

  non-null

  Represents an association between a variant and a selling plan. Selling plan allocations describe which selling plans are available for each variant, and what their impact is on pricing.

  * after

    [String](https://shopify.dev/docs/api/storefront/latest/scalars/String)

    ### Arguments

    Returns the elements that come after the specified cursor.

  * before

    [String](https://shopify.dev/docs/api/storefront/latest/scalars/String)

    Returns the elements that come before the specified cursor.

  * first

    [Int](https://shopify.dev/docs/api/storefront/latest/scalars/Int)

    Returns up to the first `n` elements from the list.

  * last

    [Int](https://shopify.dev/docs/api/storefront/latest/scalars/Int)

    Returns up to the last `n` elements from the list.

  * reverse

    [Boolean](https://shopify.dev/docs/api/storefront/latest/scalars/Boolean)

    Default:false

    Reverse the order of the underlying list.

  ***

* shop​Pay​Installments​Pricing

  [Shop​Pay​Installments​Product​Variant​Pricing](https://shopify.dev/docs/api/storefront/latest/objects/ShopPayInstallmentsProductVariantPricing)

  Token access required

  The Shop Pay Installments pricing information for the product variant.

* sku

  [String](https://shopify.dev/docs/api/storefront/latest/scalars/String)

  The SKU (stock keeping unit) associated with the variant.

* store​Availability

  [Store​Availability​Connection!](https://shopify.dev/docs/api/storefront/latest/connections/StoreAvailabilityConnection)

  non-null

  The in-store pickup availability of this variant by location.

  * after

    [String](https://shopify.dev/docs/api/storefront/latest/scalars/String)

    ### Arguments

    Returns the elements that come after the specified cursor.

  * before

    [String](https://shopify.dev/docs/api/storefront/latest/scalars/String)

    Returns the elements that come before the specified cursor.

  * first

    [Int](https://shopify.dev/docs/api/storefront/latest/scalars/Int)

    Returns up to the first `n` elements from the list.

  * last

    [Int](https://shopify.dev/docs/api/storefront/latest/scalars/Int)

    Returns up to the last `n` elements from the list.

  * near

    [Geo​Coordinate​Input](https://shopify.dev/docs/api/storefront/latest/input-objects/GeoCoordinateInput)

    Used to sort results based on proximity to the provided location.

  * reverse

    [Boolean](https://shopify.dev/docs/api/storefront/latest/scalars/Boolean)

    Default:false

    Reverse the order of the underlying list.

  ***

* taxable

  [Boolean!](https://shopify.dev/docs/api/storefront/latest/scalars/Boolean)

  non-null

  Whether tax is charged when the product variant is sold.

* title

  [String!](https://shopify.dev/docs/api/storefront/latest/scalars/String)

  non-null

  The product variant’s title.

* unit​Price

  [Money​V2](https://shopify.dev/docs/api/storefront/latest/objects/MoneyV2)

  The unit price value for the variant based on the variant's measurement.

* unit​Price​Measurement

  [Unit​Price​Measurement](https://shopify.dev/docs/api/storefront/latest/objects/UnitPriceMeasurement)

  The unit price measurement for the variant.

* weight

  [Float](https://shopify.dev/docs/api/storefront/latest/scalars/Float)

  The weight of the product variant in the unit system specified with `weight_unit`.

* weight​Unit

  [Weight​Unit!](https://shopify.dev/docs/api/storefront/latest/enums/WeightUnit)

  non-null

  Unit of measurement for weight.

### Deprecated fields

* compare​At​Price​V2

  [Money​V2](https://shopify.dev/docs/api/storefront/latest/objects/MoneyV2)

  Deprecated

* price​V2

  [Money​V2!](https://shopify.dev/docs/api/storefront/latest/objects/MoneyV2)

  non-nullDeprecated

***

## Map

### Fields and connections with this object

* [OrderLineItem.variant](https://shopify.dev/docs/api/storefront/latest/objects/OrderLineItem#field-OrderLineItem.fields.variant)
* [Product.adjacentVariants](https://shopify.dev/docs/api/storefront/latest/objects/Product#field-Product.fields.adjacentVariants)
* [Product.selectedOrFirstAvailableVariant](https://shopify.dev/docs/api/storefront/latest/objects/Product#field-Product.fields.selectedOrFirstAvailableVariant)
* [Product.variantBySelectedOptions](https://shopify.dev/docs/api/storefront/latest/objects/Product#field-Product.fields.variantBySelectedOptions)
* [Product.variants](https://shopify.dev/docs/api/storefront/latest/objects/Product#field-Product.fields.variants)
* [ProductOptionValue.firstSelectableVariant](https://shopify.dev/docs/api/storefront/latest/objects/ProductOptionValue#field-ProductOptionValue.fields.firstSelectableVariant)
* [ProductVariant.groupedBy](https://shopify.dev/docs/api/storefront/latest/objects/ProductVariant#field-ProductVariant.fields.groupedBy)
* [ProductVariantComponent.productVariant](https://shopify.dev/docs/api/storefront/latest/objects/ProductVariantComponent#field-ProductVariantComponent.fields.productVariant)
* [ProductVariantConnection.nodes](https://shopify.dev/docs/api/storefront/latest/connections/ProductVariantConnection#returns-nodes)
* [ProductVariantEdge.node](https://shopify.dev/docs/api/storefront/latest/objects/ProductVariantEdge#field-ProductVariantEdge.fields.node)

### Possible type in

* [Merchandise](https://shopify.dev/docs/api/storefront/latest/unions/Merchandise)
* [Metafield​Parent​Resource](https://shopify.dev/docs/api/storefront/latest/unions/MetafieldParentResource)
* [Metafield​Reference](https://shopify.dev/docs/api/storefront/latest/unions/MetafieldReference)

***

## Interfaces

* * [Has​Metafields](https://shopify.dev/docs/api/storefront/latest/interfaces/HasMetafields)

    interface

  * [Node](https://shopify.dev/docs/api/storefront/latest/interfaces/Node)

    interface

***

## ProductVariant Implements

### Implements

* [Has​Metafields](https://shopify.dev/docs/api/storefront/latest/interfaces/HasMetafields)
* [Node](https://shopify.dev/docs/api/storefront/latest/interfaces/Node)
