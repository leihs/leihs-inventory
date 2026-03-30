-- Remove rows created by insert-inventory-test-models.sql (FK-safe order).
-- Temporarily disables prevent_delete_on_items_t (Leihs blocks plain DELETE on items).

BEGIN;

ALTER TABLE public.items DISABLE TRIGGER prevent_delete_on_items_t;

DELETE FROM public.reservations
WHERE item_id IN (SELECT id FROM public.items WHERE inventory_code LIKE 'test-%-inv-%'
                   OR inventory_code LIKE 'testext-%-inv-%'
                   OR inventory_code LIKE 'testcomplex-%-inv-%');

DELETE FROM public.contracts
WHERE compact_id LIKE 'test-%-contract-%' OR compact_id LIKE 'testext-%-contract-%'
   OR compact_id LIKE 'testcomplex-%-contract-%';

DELETE FROM public.options
WHERE inventory_code LIKE 'test-%-inv-%' OR product LIKE 'test-%'
   OR inventory_code LIKE 'testext-%-inv-%' OR product LIKE 'testext-%'
   OR inventory_code LIKE 'testcomplex-%-inv-%' OR product LIKE 'testcomplex-%';

DELETE FROM public.items
WHERE (inventory_code LIKE 'test-%-inv-%' OR inventory_code LIKE 'testext-%-inv-%'
       OR inventory_code LIKE 'testcomplex-%-inv-%')
  AND parent_id IS NOT NULL;

DELETE FROM public.items
WHERE inventory_code LIKE 'test-%-inv-%' OR inventory_code LIKE 'testext-%-inv-%'
   OR inventory_code LIKE 'testcomplex-%-inv-%';

DELETE FROM public.models
WHERE product LIKE 'test-%' OR product LIKE 'testext-%' OR product LIKE 'testcomplex-%';

ALTER TABLE public.items ENABLE TRIGGER prevent_delete_on_items_t;

COMMIT;
