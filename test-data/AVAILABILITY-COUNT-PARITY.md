# Availability counts and the Retired filter (`test-002-model-plain`)

## Behavior

List **availability** (`borrowable_quantity` / `in_stock_quantity`) comes from `base-inventory-query` in [`src/leihs/inventory/server/resources/pool/list/queries.clj`](../src/leihs/inventory/server/resources/pool/list/queries.clj). Both subqueries count pool items with `parent_id` null and `is_borrowable` true, then filter by retirement:

- **`retired=true`**: only rows where `items.retired IS NOT NULL`
- **Otherwise** (`retired` absent, `false`, or any non-true value): only rows where `items.retired IS NULL`

So headline numbers **exclude retired lines** unless the list is explicitly filtered to **Retired**. That matches the default list URL (`retired=false`) and avoids `3 | 3` on `test-002-model-plain` when the UI shows **All** but the URL omits `retired` (aggregates still count only non-retired borrowable items → **2 | 2**).

**Note:** When `retired` is absent, [`items-shared/item-query-params`](../../src/leihs/inventory/server/resources/pool/items/shared.clj) does **not** add a retirement predicate to the expanded item rows, so you can still see retired lines under a model while availability shows non-retired-only totals.

Default new-inventory list URL sets `retired=false` in [`src/leihs/inventory/client/routes.cljs`](../src/leihs/inventory/client/routes.cljs) (`list/?with_items=true&retired=false&page=1&size=50`).

## Scenario table

| `retired` in URL | Availability subqueries | `test-002-model-plain` (2 active + 1 retired borrowable) |
|------------------|-------------------------|------------------------------------------------------------|
| Absent | Non-retired only | **2 \| 2** |
| `false` | Non-retired only | **2 \| 2** |
| `true` | Retired only | Counts only retired borrowable top-level lines |

## See also

- [`RETIRED-FILTER-TEST-MATRIX.md`](../RETIRED-FILTER-TEST-MATRIX.md) — retired matrix for API/feature expectations.
- [`README.md`](README.md) — test-data SQL scripts.
