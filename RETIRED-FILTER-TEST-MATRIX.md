# Retired filter and list availability (`in_stock` | `borrowable`)

List rows expose `in_stock_quantity` and `borrowable_quantity` from `base-inventory-query`. Availability subqueries count borrowable top-level items (`is_borrowable` true, `parent_id` null), then:

- **`retired=true`**: `items.retired IS NOT NULL`
- **Any other `retired` value** (including omitted/`nil`/`false`): `items.retired IS NULL`

So headline totals match **non-retired** stock unless the list is filtered to **Retired**, even when the UI “Retired: All” leaves `retired` out of the URL.

Expanded item rows still use [`items-shared/item-query-params`](src/leihs/inventory/server/resources/pool/items/shared.clj): a retirement filter is applied only when `retired` is a boolean. With `retired` absent, rows can include retired lines while availability stays non-retired-only.

## Query parameter matrix

| UI / URL | `retired` coerced value | Effect on availability subqueries |
|----------|-------------------------|-----------------------------------|
| All (param omitted) | `nil` | Non-retired only (`items.retired IS NULL`) |
| Not retired | `false` | Non-retired only |
| Retired | `true` | Retired only (`items.retired IS NOT NULL`) |

Both subqueries still require `items.is_borrowable = true` and `items.parent_id IS NULL` (top-level items only).

## API coverage

`spec/backend/api/models/models_list_spec.rb` — example **`reflects retired=all vs retired=false vs retired=true in list quantities`**

Fixture: one non-retired borrowable item, one retired borrowable item.

- No `retired` param: non-retired only → `1 | 1`.
- `retired=false`: non-retired only → `1 | 1`.
- `retired=true`: retired only → `1 | 1`.

## Feature spec combinations that broke vs old behavior

Previously, availability columns **ignored** `retired` while the table still filtered rows. After wiring `retired` into `base-inventory-query`, expectations must match **filtered** aggregates.

| Scenario | File / context | Combination | Old (wrong) expectation | Correct expectation | Reason |
|----------|----------------|-------------|-------------------------|---------------------|--------|
| `filters work` | `main_spec.rb` — pool_1, `with_items`, **`retired=retired`** | `retired=true` on model row | model_2 **`3 \| 3`** | **`0 \| 0`** | Only retired item for model_2 is `item_model_2_2`, which is **`is_borrowable: false`**, so it is excluded from both quantity subqueries. |
| Same block + **`borrowable=not_borrowable`** | same | `retired=true` + not borrowable | model_2 **`3 \| 3`** | **`0 \| 0`** | Same counts as above; row filters narrow **which items** expand, but quantities still only count borrowable top-level items in the retired slice (here: none). |

### model_2 fixture reminder (`filters work`)

For pool_1 / model_2:

- `item_model_2_1` — not retired, borrowable  
- `item_model_2_2` — **retired**, **not borrowable** (only row visible when `retired=true`)  
- `item_model_2_3`, `item_model_2_4` — not retired, borrowable  

So with `retired=true`, the aggregate borrowable/in-stock counts for model_2 are **zero**, while the expanded row still correctly shows `item_model_2_2`.

### model_3 (`with_items`, `retired` not set)

`item_model_3_1` is **retired** and **borrowable**. With `retired` absent from the URL, the model row still appears (item `exists` clause does not filter by retirement), but availability counts **non-retired** lines only → **`0 | 0`**, while the expanded line still shows the retired item.

## Related edge cases to watch

- **Package children**: `parent_id` set → excluded from both numbers; child models often show **`0 | 0`** when all items are package members (`models_list_spec` package example).
