# PR Review: Fix Allocations Section in Create/Update Model

## Bug Description

The **Allocations** section in the Create/Update Model form has two bugs:

1. **"Allocations (max ?)" always shows "max 0"** — the maximum rentable item count is hardcoded to `"0"`.
2. **Validation indicator is broken** — the green/red bar always shows red for any allocation >= 1, because it validates against the hardcoded `"0"`.

---

## Root Cause

The `items` prop (representing `rentable` count) is **hardcoded to `"0"`** in `fields.cljs`, and no backend endpoint currently provides this value for the model CRUD form.

---

## Files to Change

### 1. Backend: Add `rentable` to model GET response

**File:** `src/leihs/inventory/server/resources/pool/models/model/main.clj`

Add a `fetch-rentable` function following the pattern from `entitlement_group/query.clj:46-62` (`select-available`), but using the simpler `rentable` logic from `list/queries.clj:14-20`:

```clojure
(defn fetch-rentable [tx pool-id model-id]
  (let [query (-> (sql/select [[[:count :items.id]] :rentable])
                  (sql/from :items)
                  (sql/where [:and
                              [:= :items.inventory_pool_id pool-id]
                              [:= :items.model_id model-id]
                              [:= :items.is_borrowable true]])
                  sql-format)]
    (or (:rentable (jdbc/execute-one! tx query)) 0)))
```

Then update `get-resource` (line 109) to include it in the response:

```clojure
;; Add to the let bindings (after line 127):
rentable (fetch-rentable tx pool-id model-id)

;; Add to the assoc block (around line 130):
:rentable rentable
```

**Reference — existing `rentable` logic (list endpoint):**
- `src/leihs/inventory/server/resources/pool/list/queries.clj` lines 14-20

**Reference — existing `available` logic (entitlement group detail):**
- `src/leihs/inventory/server/resources/pool/entitlement_groups/entitlement_group/query.clj` lines 46-62

| Field | Conditions | Used In |
|-------|-----------|---------|
| `rentable` | `is_borrowable = true` | list endpoint |
| `available` | `retired IS NULL`, `is_borrowable = true`, `parent_id IS NULL` | entitlement group detail |

Use `rentable` (broader count, matching list view behavior).

---

### 2. Frontend: Pass `rentable` to EntitlementAllocations component

**File:** `src/leihs/inventory/client/routes/pools/models/crud/components/fields.cljs`

**Current (line 34-38):**
```clojure
(-> block :component (= "entitlement-allocations"))
($ EntitlementAllocations {:control control
                           :items "0"          ;; <-- hardcoded
                           :form form
                           :props (:props block)})
```

**Fix:** Replace `"0"` with the actual `rentable` value from loader data. The `field` component signature (`{:keys [control form block]}` at line 27) does not currently receive model data, so either:

- **Option A:** Add a `data` prop and pass it down from the parent, or
- **Option B:** Call `useLoaderData` inside the `field` component to access `:data`

Example using Option B:
```clojure
(defui field [{:keys [control form block]}]
  (let [[t] (useTranslation)
        {:keys [data]} (useLoaderData)]    ;; add useLoaderData
    (cond
      ;; ...
      (-> block :component (= "entitlement-allocations"))
      ($ EntitlementAllocations {:control control
                                 :items (str (or (:rentable data) 0))
                                 :form form
                                 :props (:props block)})
      ;; ...
```

Note: `useLoaderData` is already imported in the entitlement allocations component but would need to be added to the `fields.cljs` requires:
```clojure
["react-router-dom" :refer [useLoaderData]]
```

---

### 3. Frontend: Validation logic review (optional)

**File:** `src/leihs/inventory/client/routes/pools/models/crud/components/entitlement_allocations.cljs`

**Current validation (lines 122-126):**
```clojure
(if (> (+ (js/parseInt items) 1)
       (js/parseInt allocations))
    " bg-green-500"
    " bg-red-500")
```

This checks `items + 1 > allocations`, meaning green when `allocations <= items`. This is functionally correct once `items` has the real `rentable` value. The `+ 1` compensates for `>` instead of `>=`.

Consider simplifying to be more readable:
```clojure
(if (<= (js/parseInt allocations)
        (js/parseInt items))
    " bg-green-500"
    " bg-red-500")
```

Also note: this is **visual-only validation** (colored bar). There is no form-level validation preventing submission when allocations exceed rentable items. Consider adding Zod/form-level validation if strict enforcement is desired.

---

## Data Flow (after fix)

```
Backend                          Frontend Loader              Frontend Component

GET /models/:model-id            models-crud-page             field
  ├─ model fields                  ├─ :data (model)             ├─ useLoaderData → :data
  ├─ entitlements                  │    └─ :rentable ──────────────→ :items prop
  ├─ rentable  ← NEW              ├─ :categories                └─ EntitlementAllocations
  └─ ...                           ├─ :entitlement-groups            ├─ label: "max {{amount}}"
                                   └─ :manufacturers                 └─ validation: green/red
```

---

## Testing

1. Create a model with borrowable items in a pool
2. Open Edit Model → Allocations section
3. Verify "Allocations (max N)" shows the correct `rentable` count
4. Add entitlement group allocations exceeding `N` → red indicator
5. Reduce allocations to <= `N` → green indicator
6. Verify the list view's `rentable` column matches the max shown in the form
