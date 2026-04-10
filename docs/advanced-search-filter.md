# `filter_q` Opportunities for Items Endpoint

## Scope

This document describes all supported `filter_q` options for:

- `GET /inventory/:pool_id/items/`

`filter_q` is an EDN map (URL-encoded in query params), using Mongo-like operators (`:$eq`, `:$gte`, `:$lte`, `:$ilike`, `:$and`, `:$or`).

---

## 1) Filterable Fields

### A) Built-in item fields (always available)

- UUID-like: `:id`, `:inventory_pool_id`, `:owner_id`, `:supplier_id`, `:model_id`, `:room_id`, `:parent_id`, `:building_id`
- Date: `:last_check`, `:invoice_date`, `:retired` (special handling, see below)
- Numeric: `:price`
- Boolean: `:is_inventory_relevant`, `:is_broken`, `:is_borrowable`, `:is_incomplete`, `:needs_permission`
- Text: `:inventory_code`, `:insurance_number`, `:item_version`, `:responsible`, `:serial_number`, `:shelf`, `:user_name`, `:retired_reason`, `:status_note`, `:note`, `:invoice_number`, `:name`

### B) Dynamic fields from `/fields?target_type=item`

Additionally allowed:

- each field id returned by `/inventory/:pool_id/fields/?target_type=item`
- and prefixed property variant: `:properties_<field_id>`

Example: if `/fields` contains `reference`, then both are valid:

- `:reference`
- `:properties_reference`

---

## 2) Supported Operators

- Equality:
  - shorthand scalar: `{:inventory_code "ITZ21122"}` (same as `:$eq`) — **not valid for boolean fields**
  - explicit: `{:inventory_code {:$eq "ITZ21122"}}`
  - booleans **require** explicit form: `{:is_borrowable {:$eq false}}` — scalar `{:is_borrowable false}` returns 400
- Range:
  - `:$gte`, `:$lte` (numeric and date use-cases)
- Text search:
  - `:$ilike` (case-insensitive partial match, wrapped as `%value%`)
- Logical:
  - `:$and`, `:$or` (nesting supported)
- Null checks:
  - `{:field nil}` or `{:field {:$eq nil}}`
- Multiple top-level fields in one map are treated as implicit `AND`.

---

## 3) Type Behavior Notes

- **Boolean fields** (`:is_borrowable`, `:is_broken`, `:is_incomplete`, `:is_inventory_relevant`, `:needs_permission`):
  - must use explicit operator: `{:is_borrowable {:$eq false}}`
  - scalar shorthand `{:is_borrowable false}` is rejected with 400
- **Date fields** (`:invoice_date`, `:last_check`):
  - scalar / `:$eq` means full-day match
  - `:$gte` and `:$lte` are inclusive day boundaries
- **`retired` is special**:
  - only boolean predicates allowed:
    - `{:retired true}` -> `IS NOT NULL`
    - `{:retired false}` -> `IS NULL`
    - `{:retired {:$eq true|false}}` also valid
  - date/text operators on `:retired` are rejected (400)
- **`building_id`** is resolved via `rooms.building_id` internally

---

## 4) Valid `filter_q` Examples

- Exact inventory code:
  - `{:inventory_code "ITZ21122"}`
- Partial text:
  - `{:inventory_code {:$ilike "ITZ211"}}`
- UUID match:
  - `{:supplier_id {:$eq "d20338d8-1182-5ea6-91da-ea96c3a0a76a"}}`
- Numeric range:
  - `{:$and [{:price {:$gte 50}} {:price {:$lte 100}}]}`
- Date range:
  - `{:$and [{:invoice_date {:$gte "2013-09-19"}} {:invoice_date {:$lte "2013-09-20"}}]}`
- Boolean:
  - `{:is_borrowable {:$eq false}}`
  - `{:is_broken {:$eq true}}`
- Retired state:
  - `{:retired false}`
- Dynamic property field:
  - `{:properties_reference {:$eq "invoice"}}`
  - `{:properties_mac_address {:$ilike "00:1B"}}`
- Nested logic:
  - `{:$or [{:$and [{:is_borrowable {:$eq false}} {:price {:$gte 100}}]} {:inventory_code {:$eq "ITZ21124"}}]}`

---

## 5) Invalid / Unsupported Cases

- Non-EDN `filter_q` -> 400
- Unknown field keys -> 400
- Unknown `properties_*` keys (not configured in `/fields`) -> 400
- Unsupported operators (e.g. `:$gt`, `:$lt`, `:$ne`, `:$exists`) -> 400
- Non-boolean predicates on `:retired` -> 400
- Scalar boolean shorthand on boolean fields (e.g. `{:is_borrowable false}`) -> 400; use `{:is_borrowable {:$eq false}}` instead

---

## 6) URL Usage

`filter_q` must be URL-encoded in the request query string.

Example (conceptual):

- Raw EDN: `{:inventory_code {:$ilike "WF-A"}}`
- Query: `...?filter_q=<url-encoded-edn>`
