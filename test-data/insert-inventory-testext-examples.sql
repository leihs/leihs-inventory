-- Extended parity examples: models, packages, software, options, lent/returned reservations.
-- Prefix: testext-<nn>-* on products, inventory_code, contract compact_id.
-- Targets pool 8bd16d45-056d-5590-bc7f-12849f034351 (same as insert-inventory-test-models.sql).

BEGIN;

DO $$
DECLARE
  v_pool    uuid;
  v_pool_b  uuid;
  v_room    uuid;
  v_user    uuid;
  v_contract uuid := uuid_generate_v4();

  -- Models
  m01  uuid := uuid_generate_v4();  -- plain model, in stock
  m02  uuid := uuid_generate_v4();  -- model, signed lent (open)
  m03  uuid := uuid_generate_v4();  -- model, retired + borrowable
  m04  uuid := uuid_generate_v4();  -- model, broken + incomplete
  m05  uuid := uuid_generate_v4();  -- software, in stock
  m06  uuid := uuid_generate_v4();  -- software, signed lent
  m07  uuid := uuid_generate_v4();  -- software, not borrowable
  m08  uuid := uuid_generate_v4();  -- software (license-style naming)
  m09  uuid := uuid_generate_v4();  -- package root, in stock + 2 children
  m10  uuid := uuid_generate_v4();  -- package child A
  m11  uuid := uuid_generate_v4();  -- package child B
  m12  uuid := uuid_generate_v4();  -- package root, lent (signed)
  m13  uuid := uuid_generate_v4();  -- child of lent package
  m14  uuid := uuid_generate_v4();  -- empty model (no items)
  m15  uuid := uuid_generate_v4();  -- owner split (optional)
  m16  uuid := uuid_generate_v4();  -- model, signed reservation returned (in stock)
  m17  uuid := uuid_generate_v4();  -- software, approved open (no contract)
  m18  uuid := uuid_generate_v4();  -- model, not borrowable (in stock)
  m19  uuid := uuid_generate_v4();  -- software, retired + borrowable
  m20  uuid := uuid_generate_v4();  -- package root, retired borrowable root (children active)

  i01  uuid := uuid_generate_v4();
  i02  uuid := uuid_generate_v4();
  i03  uuid := uuid_generate_v4();
  i04  uuid := uuid_generate_v4();
  i05  uuid := uuid_generate_v4();
  i06  uuid := uuid_generate_v4();
  i07  uuid := uuid_generate_v4();
  i08  uuid := uuid_generate_v4();
  i09  uuid := uuid_generate_v4();
  i10  uuid := uuid_generate_v4();
  i11  uuid := uuid_generate_v4();
  i12  uuid := uuid_generate_v4();
  i13  uuid := uuid_generate_v4();
  i15  uuid := uuid_generate_v4();
  i16  uuid := uuid_generate_v4();
  i17  uuid := uuid_generate_v4();
  i18  uuid := uuid_generate_v4();
  i19  uuid := uuid_generate_v4();
  i20  uuid := uuid_generate_v4();
  i21  uuid := uuid_generate_v4();

  r02  uuid := uuid_generate_v4();
  r06  uuid := uuid_generate_v4();
  r12  uuid := uuid_generate_v4();
  r16  uuid := uuid_generate_v4();
  r17  uuid := uuid_generate_v4();
BEGIN
  SELECT ip.id, r.id INTO v_pool, v_room
  FROM inventory_pools ip
  CROSS JOIN LATERAL (
    SELECT id FROM public.rooms ORDER BY id LIMIT 1
  ) r
  WHERE ip.id = '8bd16d45-056d-5590-bc7f-12849f034351'
  LIMIT 1;

  SELECT id INTO v_pool_b
  FROM inventory_pools
  WHERE id IS DISTINCT FROM v_pool
  ORDER BY id
  LIMIT 1;

  SELECT id INTO v_user FROM public.users ORDER BY created_at LIMIT 1;

  IF v_pool IS NULL OR v_room IS NULL THEN
    RAISE EXCEPTION 'Need at least one inventory_pool and one room';
  END IF;
  IF v_user IS NULL THEN
    RAISE EXCEPTION 'Need at least one user (for contracts/reservations)';
  END IF;

  INSERT INTO public.contracts (
    id, compact_id, note, created_at, updated_at, state, user_id, inventory_pool_id, purpose)
  VALUES (
    v_contract,
    'testext-001-contract-' || replace(uuid_generate_v4()::text, '-', ''),
    NULL, now(), now(), 'open', v_user, v_pool,
    'testext extended examples');

  INSERT INTO public.models (id, type, manufacturer, product, version, is_package, created_at, updated_at)
  VALUES
    (m01,  'Model',    'TestExtCo', 'testext-001-model-active',               'v1', false, now(), now()),
    (m02,  'Model',    'TestExtCo', 'testext-002-model-lent-signed',          'v1', false, now(), now()),
    (m03,  'Model',    'TestExtCo', 'testext-003-model-retired-borrowable',   'v1', false, now(), now()),
    (m04,  'Model',    'TestExtCo', 'testext-004-model-broken-incomplete',   'v1', false, now(), now()),
    (m05,  'Software', 'TestExtCo', 'testext-005-software-in-stock',          'v1', false, now(), now()),
    (m06,  'Software', 'TestExtCo', 'testext-006-software-lent-signed',        'v1', false, now(), now()),
    (m07,  'Software', 'TestExtCo', 'testext-007-software-not-borrowable',    'v1', false, now(), now()),
    (m08,  'Software', 'TestExtCo', 'testext-008-software-license-concurrent', 'site', false, now(), now()),
    (m09,  'Model',    'TestExtCo', 'testext-009-package-kit',                'kit', true, now(), now()),
    (m10,  'Model',    'TestExtCo', 'testext-010-package-child-lens',         'v1', false, now(), now()),
    (m11,  'Model',    'TestExtCo', 'testext-011-package-child-body',         'v1', false, now(), now()),
    (m12,  'Model',    'TestExtCo', 'testext-012-package-lent-root',          'v1', true, now(), now()),
    (m13,  'Model',    'TestExtCo', 'testext-013-package-lent-child',         'v1', false, now(), now()),
    (m14,  'Model',    'TestExtCo', 'testext-014-model-empty',                'v1', false, now(), now()),
    (m16,  'Model',    'TestExtCo', 'testext-016-model-lent-returned',        'v1', false, now(), now()),
    (m17,  'Software', 'TestExtCo', 'testext-017-software-approved-open',      'v1', false, now(), now()),
    (m18,  'Model',    'TestExtCo', 'testext-018-model-not-borrowable',       'v1', false, now(), now()),
    (m19,  'Software', 'TestExtCo', 'testext-019-software-retired-borrowable', 'v1', false, now(), now()),
    (m20,  'Model',    'TestExtCo', 'testext-020-package-retired-root',       'v1', true, now(), now());

  IF v_pool_b IS NOT NULL THEN
    INSERT INTO public.models (id, type, manufacturer, product, version, is_package, created_at, updated_at)
    VALUES
      (m15, 'Model', 'TestExtCo', 'testext-015-model-owner-split', 'v1', false, now(), now());
  END IF;

  INSERT INTO public.items (id, inventory_code, model_id, owner_id, inventory_pool_id, parent_id, room_id,
                            is_borrowable, is_broken, is_incomplete, retired, retired_reason, price, created_at, updated_at, shelf)
  VALUES
    (i01, 'testext-101-inv-model-active', m01, v_pool, v_pool, NULL, v_room,
     true, false, false, NULL, NULL, 10, now(), now(), 'XT-01'),
    (i02, 'testext-102-inv-model-lent', m02, v_pool, v_pool, NULL, v_room,
     true, false, false, NULL, NULL, 20, now(), now(), 'XT-02'),
    (i03, 'testext-103-inv-model-retired', m03, v_pool, v_pool, NULL, v_room,
     true, false, false, CURRENT_DATE, 'testext retired', 30, now(), now(), 'XT-03'),
    (i04, 'testext-104-inv-model-broken', m04, v_pool, v_pool, NULL, v_room,
     true, true, true, NULL, NULL, 40, now(), now(), 'XT-04'),
    (i05, 'testext-105-inv-software-stock', m05, v_pool, v_pool, NULL, v_room,
     true, false, false, NULL, NULL, 0, now(), now(), 'XT-05'),
    (i06, 'testext-106-inv-software-lent', m06, v_pool, v_pool, NULL, v_room,
     true, false, false, NULL, NULL, 99, now(), now(), 'XT-06'),
    (i07, 'testext-107-inv-software-nb', m07, v_pool, v_pool, NULL, v_room,
     false, false, false, NULL, NULL, 199, now(), now(), 'XT-07'),
    (i08, 'testext-108-inv-license-sw', m08, v_pool, v_pool, NULL, v_room,
     true, false, false, NULL, NULL, 250, now(), now(), 'XT-08'),
    (i09, 'testext-109-inv-pkg-kit-root', m09, v_pool, v_pool, NULL, v_room,
     true, false, false, NULL, NULL, 500, now(), now(), 'XT-PK'),
    (i10, 'testext-110-inv-pkg-child-lens', m10, v_pool, v_pool, i09, v_room,
     true, false, false, NULL, NULL, 100, now(), now(), 'XT-PL');
  INSERT INTO public.items (id, inventory_code, model_id, owner_id, inventory_pool_id, parent_id, room_id,
                            is_borrowable, is_broken, is_incomplete, retired, retired_reason, price, created_at, updated_at, shelf)
  VALUES
    (i11, 'testext-111-inv-pkg-child-body', m11, v_pool, v_pool, i09, v_room,
     true, false, false, NULL, NULL, 200, now(), now(), 'XT-PB'),
    (i12, 'testext-112-inv-pkg-lent-root', m12, v_pool, v_pool, NULL, v_room,
     true, false, false, NULL, NULL, 600, now(), now(), 'XT-PR'),
    (i13, 'testext-113-inv-pkg-lent-child', m13, v_pool, v_pool, i12, v_room,
     true, false, false, NULL, NULL, 150, now(), now(), 'XT-PC'),
    (i16, 'testext-116-inv-lent-returned', m16, v_pool, v_pool, NULL, v_room,
     true, false, false, NULL, NULL, 70, now(), now(), 'XT-16'),
    (i17, 'testext-117-inv-sw-approved', m17, v_pool, v_pool, NULL, v_room,
     true, false, false, NULL, NULL, 55, now(), now(), 'XT-17'),
    (i18, 'testext-118-inv-model-nb', m18, v_pool, v_pool, NULL, v_room,
     false, false, false, NULL, NULL, 15, now(), now(), 'XT-18'),
    (i19, 'testext-119-inv-sw-retired', m19, v_pool, v_pool, NULL, v_room,
     true, false, false, CURRENT_DATE, 'testext sw retired', 88, now(), now(), 'XT-19'),
    (i20, 'testext-120-inv-pkg-retired-root', m20, v_pool, v_pool, NULL, v_room,
     true, false, false, CURRENT_DATE, 'pkg root retired', 300, now(), now(), 'XT-20R');
  INSERT INTO public.items (id, inventory_code, model_id, owner_id, inventory_pool_id, parent_id, room_id,
                            is_borrowable, is_broken, is_incomplete, retired, retired_reason, price, created_at, updated_at, shelf)
  VALUES
    (i21, 'testext-121-inv-pkg-retired-child', m10, v_pool, v_pool, i20, v_room,
     true, false, false, NULL, NULL, 90, now(), now(), 'XT-21C');

  IF v_pool_b IS NOT NULL THEN
    INSERT INTO public.items (id, inventory_code, model_id, owner_id, inventory_pool_id, parent_id, room_id,
                              is_borrowable, is_broken, is_incomplete, retired, retired_reason, price, created_at, updated_at, shelf)
    VALUES
      (i15, 'testext-115-inv-owner-split', m15, v_pool, v_pool_b, NULL, v_room,
       true, false, false, NULL, NULL, 120, now(), now(), 'XT-15');
  END IF;

  -- Same contract → identical start_date (check_unique_start_date_for_same_contract_f).
  INSERT INTO public.reservations (
    id, contract_id, inventory_pool_id, user_id, type, status, item_id, model_id,
    quantity, start_date, end_date, returned_date, created_at, updated_at)
  VALUES
    (r02, v_contract, v_pool, v_user, 'ItemLine', 'signed', i02, m02,
     1, CURRENT_DATE - 10, CURRENT_DATE + 30, NULL, now(), now()),
    (r06, v_contract, v_pool, v_user, 'ItemLine', 'signed', i06, m06,
     1, CURRENT_DATE - 10, CURRENT_DATE + 30, NULL, now(), now()),
    (r12, v_contract, v_pool, v_user, 'ItemLine', 'signed', i12, m12,
     1, CURRENT_DATE - 10, CURRENT_DATE + 30, NULL, now(), now()),
    (r16, v_contract, v_pool, v_user, 'ItemLine', 'signed', i16, m16,
     1, CURRENT_DATE - 10, CURRENT_DATE + 30, CURRENT_DATE - 2, now(), now());

  INSERT INTO public.reservations (
    id, contract_id, inventory_pool_id, user_id, type, status, item_id, model_id,
    quantity, start_date, end_date, returned_date, created_at, updated_at)
  VALUES
    (r17, NULL, v_pool, v_user, 'ItemLine', 'approved', i17, m17,
     1, CURRENT_DATE - 10, CURRENT_DATE + 30, NULL, now(), now());

  INSERT INTO public.options (
    id, inventory_pool_id, inventory_code, manufacturer, product, version, price, created_at, updated_at)
  VALUES (
    uuid_generate_v4(), v_pool, 'testext-201-inv-option', 'TestExtCo', 'testext-201-option-addon', 'v1', 12.5, now(), now());
END $$;

COMMIT;
