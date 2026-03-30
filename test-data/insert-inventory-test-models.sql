-- Insert test models/items/reservations for inventory list vs Legacy parity.
-- Prefix: each inserted row uses its own incremented prefix test-<nnn>-*.
-- Requires: uuid-ossp (uuid_generate_v4()), at least one user, pool, room.

BEGIN;

DO $$
DECLARE
  v_pool    uuid;
  v_pool_b  uuid;
  v_room    uuid;
  v_user    uuid;
  v_contract uuid := uuid_generate_v4();

  m_plain  uuid := uuid_generate_v4();
  m_empty  uuid := uuid_generate_v4();
  m_pkg    uuid := uuid_generate_v4();
  m_ca     uuid := uuid_generate_v4();
  m_cb     uuid := uuid_generate_v4();
  m_ro     uuid := uuid_generate_v4();  -- reservation: signed open
  m_rr     uuid := uuid_generate_v4();  -- reservation: signed returned
  m_ra     uuid := uuid_generate_v4();  -- reservation: approved open (no contract)
  m_owner  uuid := uuid_generate_v4();  -- owner pool != inventory_pool
  m_pr     uuid := uuid_generate_v4();  -- package rented (root)
  m_prc    uuid := uuid_generate_v4();  -- package rented child
  m_sw     uuid := uuid_generate_v4();  -- type Software

  i_pkg     uuid := uuid_generate_v4();
  i_ro      uuid := uuid_generate_v4();
  i_rr      uuid := uuid_generate_v4();
  i_ra      uuid := uuid_generate_v4();
  i_owner   uuid := uuid_generate_v4();
  i_pr_r    uuid := uuid_generate_v4();
  i_pr_c    uuid := uuid_generate_v4();
  i_sw      uuid := uuid_generate_v4();

  r_sig_open uuid := uuid_generate_v4();
  r_sig_ret  uuid := uuid_generate_v4();
  r_appr     uuid := uuid_generate_v4();
  r_pkg      uuid := uuid_generate_v4();
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
    'test-001-contract-' || replace(uuid_generate_v4()::text, '-', ''),
    NULL, now(), now(), 'open', v_user, v_pool,
    'test-data reservations');

  -- Models: plain, empty, package + children, reservation matrix, software, optional owner-split, rented package
  INSERT INTO public.models (id, type, manufacturer, product, version, is_package, created_at, updated_at)
  VALUES
    (m_plain, 'Model', 'TestCo', 'test-002-model-plain', 'retired-matrix', false, now(), now()),
    (m_empty, 'Model', 'TestCo', 'test-003-model-empty', 'no-items', false, now(), now()),
    (m_pkg,   'Model', 'TestCo', 'test-004-package-model', 'pkg', true, now(), now()),
    (m_ca,    'Model', 'TestCo', 'test-005-package-child-a', 'lens', false, now(), now()),
    (m_cb,    'Model', 'TestCo', 'test-006-package-child-b', 'body', false, now(), now()),
    (m_ro,    'Model', 'TestCo', 'test-007-model-res-open-signed', 'v1', false, now(), now()),
    (m_rr,    'Model', 'TestCo', 'test-008-model-res-returned-signed', 'v1', false, now(), now()),
    (m_ra,    'Model', 'TestCo', 'test-009-model-res-open-approved', 'v1', false, now(), now()),
    (m_pr,    'Model', 'TestCo', 'test-010-package-rented', 'pkg', true, now(), now()),
    (m_prc,   'Model', 'TestCo', 'test-011-package-rented-child', 'piece', false, now(), now()),
    (m_sw,    'Software', 'TestCo', 'test-012-software-app', 'v1', false, now(), now());

  IF v_pool_b IS NOT NULL THEN
    INSERT INTO public.models (id, type, manufacturer, product, version, is_package, created_at, updated_at)
    VALUES
      (m_owner, 'Model', 'TestCo', 'test-013-model-owner-split', 'v1', false, now(), now());
  END IF;

  INSERT INTO public.items (id, inventory_code, model_id, owner_id, inventory_pool_id, parent_id, room_id,
                            is_borrowable, is_broken, is_incomplete, retired, retired_reason, price, created_at, updated_at, shelf)
  VALUES
    (i_pkg, 'test-101-inv-pkg-root', m_pkg, v_pool, v_pool, NULL, v_room,
     true, false, false, NULL, NULL, 100, now(), now(), 'T-PKG'),
    (i_ro, 'test-102-inv-res-signed-open', m_ro, v_pool, v_pool, NULL, v_room,
     true, false, false, NULL, NULL, 50, now(), now(), 'T-RSO'),
    (i_rr, 'test-103-inv-res-signed-returned', m_rr, v_pool, v_pool, NULL, v_room,
     true, false, false, NULL, NULL, 51, now(), now(), 'T-RSR'),
    (i_ra, 'test-104-inv-res-approved-open', m_ra, v_pool, v_pool, NULL, v_room,
     true, false, false, NULL, NULL, 52, now(), now(), 'T-RAO'),
    (i_pr_r, 'test-105-inv-pkg-rent-root', m_pr, v_pool, v_pool, NULL, v_room,
     true, false, false, NULL, NULL, 200, now(), now(), 'T-PRENT'),
    (i_pr_c, 'test-106-inv-pkg-rent-child', m_prc, v_pool, v_pool, i_pr_r, v_room,
     true, false, false, NULL, NULL, 201, now(), now(), 'T-PRENT-C'),
    (i_sw, 'test-107-inv-software-1', m_sw, v_pool, v_pool, NULL, v_room,
     true, false, false, NULL, NULL, 0, now(), now(), 'T-SW');

  INSERT INTO public.items (id, inventory_code, model_id, owner_id, inventory_pool_id, parent_id, room_id,
                            is_borrowable, is_broken, is_incomplete, retired, retired_reason, price, created_at, updated_at, shelf)
  VALUES
    (uuid_generate_v4(), 'test-108-inv-plain-active', m_plain, v_pool, v_pool, NULL, v_room,
     true, false, false, NULL, NULL, 10, now(), now(), 'T-P1'),
    (uuid_generate_v4(), 'test-109-inv-plain-retired-borrowable', m_plain, v_pool, v_pool, NULL, v_room,
     true, false, false, CURRENT_DATE, 'test-109 retired', 11, now(), now(), 'T-P2'),
    (uuid_generate_v4(), 'test-110-inv-plain-retired-not-borrowable', m_plain, v_pool, v_pool, NULL, v_room,
     false, false, false, CURRENT_DATE, 'test-110 retired NB', 12, now(), now(), 'T-P3'),
    (uuid_generate_v4(), 'test-111-inv-plain-broken-incomplete', m_plain, v_pool, v_pool, NULL, v_room,
     true, true, true, NULL, NULL, 13, now(), now(), 'T-P4'),
    (uuid_generate_v4(), 'test-112-inv-pkg-child-a', m_ca, v_pool, v_pool, i_pkg, v_room,
     true, false, false, NULL, NULL, 20, now(), now(), 'T-PA'),
    (uuid_generate_v4(), 'test-113-inv-pkg-child-b', m_cb, v_pool, v_pool, i_pkg, v_room,
     true, false, false, NULL, NULL, 30, now(), now(), 'T-PB');

  IF v_pool_b IS NOT NULL THEN
    INSERT INTO public.items (id, inventory_code, model_id, owner_id, inventory_pool_id, parent_id, room_id,
                              is_borrowable, is_broken, is_incomplete, retired, retired_reason, price, created_at, updated_at, shelf)
    VALUES
      (i_owner, 'test-114-inv-owner-split', m_owner, v_pool, v_pool_b, NULL, v_room,
       true, false, false, NULL, NULL, 60, now(), now(), 'T-OWN');
  END IF;

  -- Same contract requires identical start_date (trigger check_unique_start_date_for_same_contract_f).
  INSERT INTO public.reservations (
    id, contract_id, inventory_pool_id, user_id, type, status, item_id, model_id,
    quantity, start_date, end_date, returned_date, created_at, updated_at)
  VALUES
    (r_sig_open, v_contract, v_pool, v_user, 'ItemLine', 'signed', i_ro, m_ro,
     1, CURRENT_DATE - 14, CURRENT_DATE + 30, NULL, now(), now()),
    (r_sig_ret, v_contract, v_pool, v_user, 'ItemLine', 'signed', i_rr, m_rr,
     1, CURRENT_DATE - 14, CURRENT_DATE + 30, CURRENT_DATE - 1, now(), now()),
    (r_pkg, v_contract, v_pool, v_user, 'ItemLine', 'signed', i_pr_r, m_pr,
     1, CURRENT_DATE - 14, CURRENT_DATE + 30, NULL, now(), now());

  INSERT INTO public.reservations (
    id, contract_id, inventory_pool_id, user_id, type, status, item_id, model_id,
    quantity, start_date, end_date, returned_date, created_at, updated_at)
  VALUES
    (r_appr, NULL, v_pool, v_user, 'ItemLine', 'approved', i_ra, m_ra,
     1, CURRENT_DATE - 14, CURRENT_DATE + 30, NULL, now(), now());

  INSERT INTO public.options (
    id, inventory_pool_id, inventory_code, manufacturer, product, version, price, created_at, updated_at)
  VALUES (
    uuid_generate_v4(), v_pool, 'test-201-inv-option-01', 'TestCo', 'test-201-option-plain', 'v1', 9.99, now(), now());
END $$;

COMMIT;
