# Manual SQL test data (`test-001-` prefix)

Scripts create **models**, **items**, **reservations**, **contracts**, and one **option** for comparing Legacy vs new inventory list behavior.

**Headline availability vs Retired filter:** If two browsers show different `in_stock` / `borrowable` for the same model (e.g. `3 | 3` vs `2 | 2` on `test-002-model-plain`), compare the `retired` query param. See [AVAILABILITY-COUNT-PARITY.md](AVAILABILITY-COUNT-PARITY.md).

## Requirements

- PostgreSQL with `uuid-ossp` (`uuid_generate_v4()`).
- At least one **`users`** row (for contract / reservation FKs).
- At least one **`inventory_pools`** row and one **`rooms`** row.

The insert script targets pool **`8bd16d45-056d-5590-bc7f-12849f034351`** and uses the first room by `id`. It picks a second pool when available for **`test-001-model-owner-split`** (owner pool ≠ responsible/inventory pool). If only one pool exists, that model is skipped.

## Usage

```bash
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f test-data/insert-inventory-test-models.sql
```

Extended examples (**`testext-<nn>-*`** prefix): models, packages, software, license-style software, option, signed loans, returned reservation, approved-without-contract, retired/broken/not-borrowable/owner-split — see script header in [`insert-inventory-testext-examples.sql`](insert-inventory-testext-examples.sql).

```bash
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f test-data/insert-inventory-testext-examples.sql
```

```bash
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f test-data/delete-inventory-testext-examples.sql
```

Multi-item grid (**`testcomplex-*`**): [`insert-inventory-testcomplex-examples.sql`](insert-inventory-testcomplex-examples.sql) — **35** list families `testcomplex-c01-mix` … `c35-mix`. **Rows c01–c09, c11–c19, …** (not divisible by 10): same `n_*` state counts as before, but each line uses **`model_id` = that `-mix` model** and either **`parent_id` NULL** (top-level) or **`parent_id` → root** of a companion package shell **`testcomplex-cNN-pkg-shell`**. About **`pkg_pct ∈ {15,20,…,55}`** (rotates by row) of lines **in each state bucket** are package members — see model **`version`** e.g. `ipkg35pct`. **c10,c20,c30** remain classic packages (root + `-pkg-child-a` / `-b`). One contract; option `testcomplex-201-option-addon`.

```bash
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f test-data/insert-inventory-testcomplex-examples.sql
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f test-data/delete-inventory-testcomplex-examples.sql
```

`delete-inventory-test-models.sql` also removes **`test-*`**, **`testext-*`**, and **`testcomplex-*`** in one run.

Remove test rows:

```bash
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f test-data/delete-inventory-test-models.sql
```

Purge **archived** rows from earlier import attempts (`test-arch-*`, `test-legacy-*`, `test-001-arch-*`). This temporarily disables the `items` delete trigger (required by Leihs DB):

```bash
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f test-data/purge-archived-test-rows.sql
```

## What gets created

### Models (inventory)

| Product | Version | Type | Package | Items / notes |
|---------|---------|------|---------|----------------|
| `test-001-model-plain` | retired-matrix | Model | no | Four items: active, retired+borrowable, retired+not borrowable, broken+incomplete. |
| `test-001-model-empty` | no-items | Model | no | **No items** (`with_items` / no matches). |
| `test-001-package-model` | pkg | Model | yes | Root item + two child models/items (`parent_id`). |
| `test-001-package-child-a` | lens | Model | no | Package member. |
| `test-001-package-child-b` | body | Model | no | Package member. |
| `test-001-model-res-open-signed` | v1 | Model | no | One item + **signed** reservation, **`returned_date` NULL** (checked out). |
| `test-001-model-res-returned-signed` | v1 | Model | no | One item + **signed** reservation **returned** (`returned_date` set). |
| `test-001-model-res-open-approved` | v1 | Model | no | One item + **approved** reservation, **no contract**, open (`returned_date` NULL). |
| `test-001-model-owner-split` | v1 | Model | no | One item: **`owner_id` first pool**, **`inventory_pool_id` second pool** (if 2+ pools). |
| `test-001-package-rented` | pkg | Model | yes | Root on contract; **signed open** reservation on root; one child item. |
| `test-001-package-rented-child` | piece | Model | no | Child of rented package. |
| `test-001-software-app` | v1 | Software | no | One borrowable item (`type` Software in list filters). |

### Option (`options` table)

| `inventory_code` | Product |
|--------------------|---------|
| `test-001-inv-option-01` | `test-001-option-plain` |

### Reservations / contract

- One **`contracts`** row: `compact_id` like **`test-001-contract-%`**, `purpose` `test-data reservations`.
- **Signed + contract:** open loan on `test-001-inv-res-signed-open`, returned loan on `test-001-inv-res-signed-returned`, open loan on **package** root `test-001-inv-pkg-rent-root`.
- **Approved, no contract:** open reservation on `test-001-inv-res-approved-open`.

### Inventory codes

All **`test-001-inv-*`** (globally unique `inventory_code`).

List **`in_stock`** subquery treats **any** reservation with `returned_date IS NULL` as blocking (not only `signed`), so **approved-open** should behave like **signed-open** for that count.

### `testext-*` grid (see `insert-inventory-testext-examples.sql`)

| # | `product` (prefix) | View `type` | Notes |
|---|---------------------|------------|--------|
| 001 | `testext-001-model-active` | Model | Borrowable, in stock |
| 002 | `testext-002-model-lent-signed` | Model | **Signed** open reservation (lent) |
| 003 | `testext-003-model-retired-borrowable` | Model | Retired date set, still borrowable |
| 004 | `testext-004-model-broken-incomplete` | Model | Broken + incomplete |
| 005–007 | `testext-005-software-*` … `007` | Software | In stock / lent / not borrowable |
| 008 | `testext-008-software-license-concurrent` | Software | License-style **Software** row (`License` is not a separate `inventory.type`) |
| 009–011 | `testext-009-package-kit` + child lenses/body | Package + Model | In-stock kit + 2 children (`testext-010` lens model is **reused** as child of `020` below) |
| 012–013 | `testext-012-package-lent-root` + child | Package + Model | Package **lent** (signed on root) |
| 014 | `testext-014-model-empty` | Model | No items |
| 015 | `testext-015-model-owner-split` | Model | If a second pool exists: `owner_id` pool A, `inventory_pool_id` pool B |
| 016 | `testext-016-model-lent-returned` | Model | Signed reservation **returned** (`returned_date` set) |
| 017 | `testext-017-software-approved-open` | Software | **Approved**, no contract |
| 018 | `testext-018-model-not-borrowable` | Model | `is_borrowable` false |
| 019 | `testext-019-software-retired-borrowable` | Software | Retired + borrowable |
| 020 | `testext-020-package-retired-root` | Package | Retired borrowable **root** + active child (`testext-121-inv-pkg-retired-child`) |
| 201 | `testext-201-option-addon` | Option | `options` row |

### `testcomplex-*` (see `insert-inventory-testcomplex-examples.sql`)

| Pattern | Meaning |
|---------|---------|
| `testcomplex-cNN-mix` | Primary row **NN** (01–35): Model or Software (odd/even), or **Package** if NN ∈ {10,20,30}. |
| `testcomplex-cNN-pkg-shell` | Minimal package holder (one root item `…-inv-shell-000`) for rows **not** 10/20/30; **`-mix`** lines attach here for the “in package” share. |
| `testcomplex-cNN-pkg-child-a` / `-b` | Only **c10, c20, c30** (classic kit layout). |
| Item codes | `testcomplex-cNN-inv-###` on **`-mix`** (some rows `parent_id` set). Shell root: `…-inv-shell-000`. |
| Counts | Same `n_*` buckets as before; **within each bucket**, `round(count * pkg_pct/100)` lines get `parent_id =` shell root (`pkg_pct` from `15 + (NN % 9)*5`). |
| `testcomplex-201-option-addon` | Option row |
