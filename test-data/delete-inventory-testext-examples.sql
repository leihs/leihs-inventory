-- Remove rows created by insert-inventory-testext-examples.sql (FK-safe order).
-- Temporarily disables prevent_delete_on_items_t (Leihs blocks plain DELETE on items).

BEGIN;

ALTER TABLE public.items DISABLE TRIGGER prevent_delete_on_items_t;

DELETE FROM public.reservations
WHERE item_id IN (SELECT id FROM public.items WHERE inventory_code LIKE 'testext-%-inv-%');

DELETE FROM public.contracts WHERE compact_id LIKE 'testext-%-contract-%';

DELETE FROM public.options
WHERE inventory_code LIKE 'testext-%-inv-%' OR product LIKE 'testext-%';

DELETE FROM public.items
WHERE inventory_code LIKE 'testext-%-inv-%' AND parent_id IS NOT NULL;

DELETE FROM public.items WHERE inventory_code LIKE 'testext-%-inv-%';

DELETE FROM public.models WHERE product LIKE 'testext-%';

ALTER TABLE public.items ENABLE TRIGGER prevent_delete_on_items_t;

COMMIT;
