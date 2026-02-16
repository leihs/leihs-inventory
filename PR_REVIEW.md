# PR Review: Fix Allocations Section in Create/Update Model

## Bug Description

The **Allocations** section in the Create/Update Model form has three bugs:

1. **"Allocations (max ?)" always shows "max 0"** — the maximum item count is hardcoded to `"0"`.
2. **Validation indicator is broken** — the green/red bar always shows red for any allocation >= 1, because it validates against the hardcoded `"0"`.
3. **Validation indicator doesn't react to input changes** — even with a correct max, changing a quantity in the input field does not update the color indicator because the `allocations` state is not recalculated on input.

---

## Root Cause

1. The `items` prop (representing available item count) is **hardcoded to `"0"`** in `fields.cljs`, and no backend endpoint provides this value for the model CRUD form.
2. The `allocations` total is computed in a `use-effect` whose dependency array (`[fields get-values allocations]`) never triggers on quantity input changes — `fields` only changes on add/remove, `get-values` is a stable reference, and `allocations` is a circular self-dependency.

---

## Correct Definition of "max"

**max = total quantity of borrowable, not retired, top-level items** (i.e., `is_borrowable = true AND retired IS NULL AND parent_id IS NULL`).

This matches the existing `select-available` function used for `is_quantity_ok` validation in the entitlement group detail endpoint (`entitlement_group/query.clj:46-62`). Using the same definition ensures consistency between the model form and the entitlement groups view.

### Why each condition matters

| Condition | Reason |
|-----------|--------|
| `is_borrowable = true` | Only borrowable items can be lent out |
| `retired IS NULL` | Retired items are no longer in active inventory |
| `parent_id IS NULL` | Package children cannot be independently lent — they go with the package |

### Comparison of existing item count definitions in codebase

| Field | Conditions | Source | Used for |
|-------|-----------|--------|----------|
| `rentable` | `is_borrowable` | `list/queries.clj:14-20` | List view availability column |
| `available` | `is_borrowable` + `NOT retired` + `no parent` | `entitlement_group/query.clj:46-62` | `is_quantity_ok` validation |
| `in_stock` | `is_borrowable` + `no parent` + no active reservations | `list/queries.clj:22-35` | List view availability column |

**We use the `available` definition** for max allocations to be consistent with entitlement group validation.

---

## Files to Change

### 1. Backend: Add `fetch-rentable` to model GET response

**File:** `src/leihs/inventory/server/resources/pool/models/model/main.clj`

Add a `fetch-rentable` function matching the `select-available` logic from `entitlement_group/query.clj:46-62`:

```clojure
(defn fetch-rentable [tx pool-id model-id]
  (let [query (-> (sql/select [[[:count :items.id]] :rentable])
                  (sql/from :items)
                  (sql/where [:and
                              [:= :items.inventory_pool_id pool-id]
                              [:= :items.model_id model-id]
                              [:= :items.is_borrowable true]
                              [:is :items.retired nil]
                              [:is :items.parent_id nil]])
                  sql-format)]
    (or (:rentable (jdbc/execute-one! tx query)) 0)))
```

Then update `get-resource` to include it in the response:

```clojure
;; Add to the let bindings:
rentable (fetch-rentable tx pool-id model-id)

;; Add to the assoc block:
:rentable rentable
```

---

### 2. Frontend: Pass `rentable` to EntitlementAllocations component

**File:** `src/leihs/inventory/client/routes/pools/models/crud/components/fields.cljs`

**Changes:**
- Add `["react-router-dom" :refer [useLoaderData]]` to requires
- Add `{:keys [data]} (jc (useLoaderData))` to the `let` bindings
- Replace hardcoded `"0"` with `(str (or (:rentable data) 0))`

```clojure
(defui field [{:keys [control form block]}]
  (let [[t] (useTranslation)
        {:keys [data]} (jc (useLoaderData))]
    (cond
      ;; ...
      (-> block :component (= "entitlement-allocations"))
      ($ EntitlementAllocations {:control control
                                 :items (str (or (:rentable data) 0))
                                 :form form
                                 :props (:props block)})
      ;; ...
```

---

### 3. Frontend: Fix reactive validation on quantity change

**File:** `src/leihs/inventory/client/routes/pools/models/crud/components/entitlement_allocations.cljs`

**Problem:** `handle-quantity-change` only calls `set-value` to update the form, but nothing triggers the `use-effect` to recalculate the total allocations. The dependency array `[fields get-values allocations]` doesn't change on input.

**Fix:** Recalculate the total directly in `handle-quantity-change`, substituting the new value for the changed index (since `set-value` is async and the form hasn't updated yet):

```clojure
handle-quantity-change
(fn [index val]
  (set-value (str "entitlements." index ".quantity") val)
  (let [entitlements (vec (jc (get-values "entitlements")))
        new-total (reduce-kv
                   (fn [acc i item]
                     (+ acc (if (= i index)
                              (js/parseInt (or val "0"))
                              (js/parseInt (or (:quantity item) "0")))))
                   0
                   entitlements)]
    (set-allocations! new-total)))
```

---

### 4. Frontend: Simplify validation comparison (optional readability improvement)

**File:** `src/leihs/inventory/client/routes/pools/models/crud/components/entitlement_allocations.cljs`

**Before:**
```clojure
(if (> (+ (js/parseInt items) 1)
       (js/parseInt allocations))
    " bg-green-500"
    " bg-red-500")
```

**After (equivalent, more readable):**
```clojure
(if (<= (js/parseInt allocations)
        (js/parseInt items))
    " bg-green-500"
    " bg-red-500")
```

Note: This is **visual-only validation** (colored bar). There is no form-level validation preventing submission when allocations exceed available items.

---

## Data Flow (after fix)

```
Backend                          Frontend Loader              Frontend Component

GET /models/:model-id            models-crud-page             field
  ├─ model fields                  ├─ :data (model)             ├─ useLoaderData → :data
  ├─ entitlements                  │    └─ :rentable ──────────────→ :items prop
  ├─ rentable  ← NEW              ├─ :categories                └─ EntitlementAllocations
  └─ ...                           ├─ :entitlement-groups            ├─ label: "max {{amount}}"
                                   └─ :manufacturers                 ├─ validation: green/red
                                                                     └─ handle-quantity-change
                                                                          recalculates total → color
```

---

## Testing

1. Create a model with borrowable items in a pool
2. Open Edit Model → Allocations section
3. Verify "Allocations (max N)" shows the correct count (borrowable, not retired, top-level items)
4. Add entitlement group allocations exceeding N → red indicator appears immediately
5. Reduce allocations to <= N → green indicator appears immediately
6. Verify consistency: the max matches `available` shown in the Entitlement Groups detail view for the same model
7. Retire an item → max should decrease by 1
8. Mark an item as not borrowable → max should decrease by 1
