https://test.leihs.zhdk.ch/inventory/8bd16d45-056d-5590-bc7f-12849f034351/items/?model_id=39fffa8c-fd32-4d3a-94df-7b79eb579638&fields=id%2Cis_package%2Cis_borrowable%2Cis_broken%2Cretired%2Cis_incomplete%2Cprice%2Cinventory_code%2Cshelf%2Cbuilding_code%2Cpackage_items%2Cbuilding_name%2Creservation_end_date%2Cshelf%2Cinventory_pool_name%2Cuser_name%2Creservation_user_name%2Curl%2Creservation_contract_id&retired=false


Here parent_id (db::items.parent_id) is missing which indicates if "is part of a package" is displayed

response:

[
    {
        "is_package": false,
        "inventory_code": "INV47565",
        "is_borrowable": true,
        "retired": null,
        "reservation_contract_id": null,
        "reservation_end_date": null,
        "shelf": "09.A.06",
        "building_code": "TONI",
        "is_broken": false,
        "inventory_pool_name": "Ausleihe Toni-Areal",
        "package_items": 0,
        "id": "b15e93d4-68ae-4d5d-9b4f-de7517a4286c",
        "building_name": "Toni-Areal",
        "url": "/inventory/8bd16d45-056d-5590-bc7f-12849f034351/models/39fffa8c-fd32-4d3a-94df-7b79eb579638/images/0bc05cc7-0bf9-4798-b776-2168f005bc74",
        "is_incomplete": false,
        "user_name": null,
        "price": 284.00,
        "reservation_user_name": null
    },
    {
        "is_package": false,
        "inventory_code": "INV47566",
        "is_borrowable": true,
        "retired": null,
        "reservation_contract_id": "67518929-debf-42ac-ad0d-df233f98bec2",
        "reservation_end_date": "2026-04-30T00:00:00Z",
        "shelf": "09.A.06",
        "building_code": "TONI",
        "is_broken": false,
        "inventory_pool_name": "Ausleihe Toni-Areal",
        "package_items": 0,
        "id": "d2442109-de3e-4e79-95bb-dffa58b30838",
        "building_name": "Toni-Areal",
        "url": "/inventory/8bd16d45-056d-5590-bc7f-12849f034351/models/39fffa8c-fd32-4d3a-94df-7b79eb579638/images/0bc05cc7-0bf9-4798-b776-2168f005bc74",
        "is_incomplete": false,
        "user_name": null,
        "price": 284.00,
        "reservation_user_name": "Jan Schweizer"
    },
    {
        "is_package": false,
        "inventory_code": "INV47567",
        "is_borrowable": true,
        "retired": null,
        "reservation_contract_id": "858e2b70-3b21-4e8f-a49b-4af0252b5798",
        "reservation_end_date": "2026-04-09T00:00:00Z",
        "shelf": "09.A.06",
        "building_code": "TONI",
        "is_broken": false,
        "inventory_pool_name": "Ausleihe Toni-Areal",
        "package_items": 0,
        "id": "3a051046-c264-4445-9cd0-478d194d852e",
        "building_name": "Toni-Areal",
        "url": "/inventory/8bd16d45-056d-5590-bc7f-12849f034351/models/39fffa8c-fd32-4d3a-94df-7b79eb579638/images/0bc05cc7-0bf9-4798-b776-2168f005bc74",
        "is_incomplete": false,
        "user_name": null,
        "price": 284.00,
        "reservation_user_name": "Gioia Clavijo Rodriguez"
    },
    {
        "is_package": false,
        "inventory_code": "INV47568",
        "is_borrowable": true,
        "retired": null,
        "reservation_contract_id": null,
        "reservation_end_date": null,
        "shelf": "09.A.06",
        "building_code": "TONI",
        "is_broken": false,
        "inventory_pool_name": "Ausleihe Toni-Areal",
        "package_items": 0,
        "id": "e40aab4e-3f88-45a8-8643-b688beff9883",
        "building_name": "Toni-Areal",
        "url": "/inventory/8bd16d45-056d-5590-bc7f-12849f034351/models/39fffa8c-fd32-4d3a-94df-7b79eb579638/images/0bc05cc7-0bf9-4798-b776-2168f005bc74",
        "is_incomplete": false,
        "user_name": null,
        "price": 284.00,
        "reservation_user_name": null
    },
    {
        "is_package": false,
        "inventory_code": "INV50943",
        "is_borrowable": true,
        "retired": null,
        "reservation_contract_id": "a47c223c-30e5-40f2-a42c-2542681b50ce",
        "reservation_end_date": "2026-04-02T00:00:00Z",
        "shelf": "09.A.06",
        "building_code": "TONI",
        "is_broken": false,
        "inventory_pool_name": "Ausleihe Toni-Areal",
        "package_items": 0,
        "id": "81d81433-ddc7-41ad-9437-c5a8edcdae00",
        "building_name": "Toni-Areal",
        "url": "/inventory/8bd16d45-056d-5590-bc7f-12849f034351/models/39fffa8c-fd32-4d3a-94df-7b79eb579638/images/0bc05cc7-0bf9-4798-b776-2168f005bc74",
        "is_incomplete": false,
        "user_name": null,
        "price": 284.00,
        "reservation_user_name": "Nadine Reichmuth"
    }
]

## Review Result

### Conclusion

The old package-item display logic already existed before commit `f71b97bc6a1540fc158329db828e23564441b844`.

### What Was Actually Missing

- `parent_id` was not requested in `model_row.cljs` item fetch fields.
- Without `parent_id`, model-expanded item rows could not detect package membership.
- As a result, `ItemInfo` did not reliably render the "package item" indicator for those rows.

### Why The Fix Works

- `model_row.cljs` now includes `parent_id` in requested fields.
- `item_row.cljs` derives `package-item?` from either explicit `isPackageItem` or `(:parent_id item)`.
- `item_info.cljs` preserves old fallback behavior and keeps location-based rendering when data exists.