-- Remove rows created by insert-inventory-testcomplex-examples.sql (FK-safe order).

BEGIN;

ALTER TABLE public.items DISABLE TRIGGER prevent_delete_on_items_t;

DELETE FROM public.reservations
WHERE item_id IN (SELECT id FROM public.items WHERE inventory_code LIKE 'testcomplex-%-inv-%');

DELETE FROM public.contracts WHERE compact_id LIKE 'testcomplex-%-contract-%';

DELETE FROM public.options
WHERE inventory_code LIKE 'testcomplex-%-inv-%' OR product LIKE 'testcomplex-%';

DELETE FROM public.items
WHERE inventory_code LIKE 'testcomplex-%-inv-%' AND parent_id IS NOT NULL;

DELETE FROM public.items WHERE inventory_code LIKE 'testcomplex-%-inv-%';

DELETE FROM public.models WHERE product LIKE 'testcomplex-%';

ALTER TABLE public.items ENABLE TRIGGER prevent_delete_on_items_t;

COMMIT;
