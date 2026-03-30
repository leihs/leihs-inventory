-- Remove superseded test rows from earlier runs:
--   * items whose code looks archived (test-arch / test-legacy / test-001-arch), or
--   * items whose model was renamed to test-arch-* (inventory_code may be normal, e.g. P-IAS…).
-- Deletes reservations first, then items (package children via recursive doomed set), then models.
-- Temporarily disables `prevent_delete_on_items_t`.

BEGIN;

ALTER TABLE public.items DISABLE TRIGGER prevent_delete_on_items_t;

DELETE FROM public.reservations
WHERE item_id IN (
  WITH RECURSIVE doomed AS (
    SELECT i.id
    FROM public.items i
    LEFT JOIN public.models m ON m.id = i.model_id
    WHERE m.product LIKE 'test-arch-%'
       OR m.product LIKE 'test-legacy-%'
       OR m.product LIKE 'test-001-arch-%'
       OR i.inventory_code LIKE 'test-arch-%'
       OR i.inventory_code LIKE 'test-legacy-%'
       OR i.inventory_code LIKE 'test-001-arch-%'
    UNION
    SELECT i2.id
    FROM public.items i2
    JOIN doomed d ON i2.parent_id = d.id
  )
  SELECT id FROM doomed);

DELETE FROM public.items
WHERE id IN (
  WITH RECURSIVE doomed AS (
    SELECT i.id
    FROM public.items i
    LEFT JOIN public.models m ON m.id = i.model_id
    WHERE m.product LIKE 'test-arch-%'
       OR m.product LIKE 'test-legacy-%'
       OR m.product LIKE 'test-001-arch-%'
       OR i.inventory_code LIKE 'test-arch-%'
       OR i.inventory_code LIKE 'test-legacy-%'
       OR i.inventory_code LIKE 'test-001-arch-%'
    UNION
    SELECT i2.id
    FROM public.items i2
    JOIN doomed d ON i2.parent_id = d.id
  )
  SELECT id FROM doomed);

DELETE FROM public.models
WHERE product LIKE 'test-arch-%'
   OR product LIKE 'test-legacy-%'
   OR product LIKE 'test-001-arch-%';

ALTER TABLE public.items ENABLE TRIGGER prevent_delete_on_items_t;

COMMIT;
