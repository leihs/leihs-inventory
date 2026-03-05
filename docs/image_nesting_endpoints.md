# Image Nesting Migration Notes

This document tracks the migration from flat image fields to a nested image object in Inventory API responses.

## Goal

Move from:

- `image_id`
- `content_type`
- `url`

to:

- `image: { id, content_type, url }`

## Implemented (`/list/`)

Endpoint:

- `GET /inventory/:pool_id/list/`

Backend changes:

- Response post-processing now converts flat image fields into `:image`.
- Top-level `:image_id`, `:content_type`, and `:url` are removed for this endpoint response.
- Schema updated to:
  - `(s/optional-key :image) (s/maybe {:id ... :content_type ... :url ...})`

Frontend changes (list page):

- `model_row` now reads image URL from `[:image :url]`.
- `package_row` now reads image URL from `[:image :url]`.

Tests adjusted:

- `spec/backend/api/models/models_image_thumb_spec.rb`
  - Assertions now expect `resp.body[0]["image"]` and `resp.body[0]["image"]["url"]`.

## Endpoints still using flat image shape

These responses still use flat image fields and are candidates for the same migration:

- `GET /inventory/:pool_id/models/`
- `GET /inventory/:pool_id/software/`
- `GET /inventory/:pool_id/templates/:template_id` (and corresponding `PUT` response payload)
- `GET /inventory/:pool_id/entitlement-groups/:entitlement_group_id`
  - also affects payload schemas reused by:
    - `POST /inventory/:pool_id/entitlement-groups/`
    - `PUT /inventory/:pool_id/entitlement-groups/:entitlement_group_id`

Additional endpoint (broader impact):

- `GET /inventory/:pool_id/items/`  
  Also returns flat image fields; migration here is likely higher risk due to wider consumer surface.

## Recommended migration order

1. `models/`
2. `software/`
3. `templates/:template_id`
4. `entitlement-groups/:entitlement_group_id` (+ reused schemas)
5. `items/` (last, highest blast radius)

## Notes

- Keep migration endpoint-scoped where possible to minimize regressions.
- If global helper behavior is changed, verify all callers of `model->enrich-with-image-attr`.
- Update Swagger/schema and backend specs together for each endpoint.
