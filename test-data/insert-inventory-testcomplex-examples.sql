-- testcomplex-c01 … c35: non-package rows use a **percent split** on the **same** model (`-mix`):
-- about **(100 − pkg_pct)%** of lines are top-level (`parent_id` NULL), **pkg_pct%** are package members
-- (`parent_id` → root item on a small `-pkg-shell` model). Split is **per state bucket** (avail, lent, …).
-- c10, c20, c30 stay full packages (root + child models a/b). Prefix: testcomplex-. Pool 8bd16d45-056d-5590-bc7f-12849f034351

BEGIN;

DO $$
DECLARE
  v_pool     uuid;
  v_room     uuid;
  v_user     uuid;
  v_contract uuid := uuid_generate_v4();

  grid_idx    int;
  serial      int;
  k           int;

  m_id        uuid;
  m_shell     uuid;
  m_ch1       uuid;
  m_ch2       uuid;

  i_id        uuid;
  n_avail     int;
  n_lent      int;
  n_ret       int;
  n_retired   int;
  n_nb        int;
  n_broken    int;
  n_appr      int;
  n_pkg_slot  int;

  model_type  text;
  is_pkg      boolean;
  i_root      uuid;

  pkg_pct     int;       -- 15..55 by row: % of lines that are “in a package”
  p_in_pkg    numeric;   -- pkg_pct / 100.0
BEGIN
  SELECT ip.id, r.id INTO v_pool, v_room
  FROM inventory_pools ip
  CROSS JOIN LATERAL (SELECT id FROM public.rooms ORDER BY id LIMIT 1) r
  WHERE ip.id = '8bd16d45-056d-5590-bc7f-12849f034351'
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
    'testcomplex-001-contract-' || replace(uuid_generate_v4()::text, '-', ''),
    NULL, now(), now(), 'open', v_user, v_pool,
    'testcomplex grid c01-c35');

  INSERT INTO public.options (
    id, inventory_pool_id, inventory_code, manufacturer, product, version, price, created_at, updated_at)
  VALUES (
    uuid_generate_v4(), v_pool, 'testcomplex-201-inv-option', 'TestComplex', 'testcomplex-201-option-addon', 'v1', 9.99, now(), now());

  FOR grid_idx IN 1..35 LOOP
    IF grid_idx % 10 = 0 THEN
      is_pkg     := true;
      model_type := 'Model';
    ELSE
      is_pkg     := false;
      model_type := CASE WHEN grid_idx % 2 = 0 THEN 'Software' ELSE 'Model' END;
    END IF;

    IF NOT is_pkg THEN
      pkg_pct  := 15 + (grid_idx % 9) * 5;   -- 15, 20, …, 55
      p_in_pkg := pkg_pct / 100.0;
    END IF;

    m_id := uuid_generate_v4();
    INSERT INTO public.models (id, type, manufacturer, product, version, is_package, created_at, updated_at)
    VALUES (
      m_id, model_type, 'TestComplex',
      'testcomplex-c' || to_char(grid_idx, 'FM00') || '-mix',
      CASE WHEN is_pkg THEN 'grid-' || grid_idx::text
           ELSE 'ipkg' || pkg_pct::text || 'pct' END,
      is_pkg, now(), now());

    IF is_pkg THEN
      m_ch1 := uuid_generate_v4();
      m_ch2 := uuid_generate_v4();
      INSERT INTO public.models (id, type, manufacturer, product, version, is_package, created_at, updated_at)
      VALUES
        (m_ch1, 'Model', 'TestComplex',
         'testcomplex-c' || to_char(grid_idx, 'FM00') || '-pkg-child-a', 'a', false, now(), now()),
        (m_ch2, 'Model', 'TestComplex',
         'testcomplex-c' || to_char(grid_idx, 'FM00') || '-pkg-child-b', 'b', false, now(), now());

      i_root := uuid_generate_v4();
      INSERT INTO public.items (id, inventory_code, model_id, owner_id, inventory_pool_id, parent_id, room_id,
                               is_borrowable, is_broken, is_incomplete, retired, retired_reason, price, created_at, updated_at, shelf)
      VALUES (
        i_root,
        'testcomplex-c' || to_char(grid_idx, 'FM00') || '-inv-' || '001',
        m_id, v_pool, v_pool, NULL, v_room,
        true, false, false, NULL, NULL, 100 + grid_idx, now(), now(), 'TC-P' || grid_idx::text);

      serial := 2;
      FOR k IN 1..2 LOOP
        i_id := uuid_generate_v4();
        INSERT INTO public.items (id, inventory_code, model_id, owner_id, inventory_pool_id, parent_id, room_id,
                                 is_borrowable, is_broken, is_incomplete, retired, retired_reason, price, created_at, updated_at, shelf)
        VALUES (
          i_id,
          'testcomplex-c' || to_char(grid_idx, 'FM00') || '-inv-' || lpad(serial::text, 3, '0'),
          m_ch1, v_pool, v_pool, i_root, v_room,
          true, false, false, NULL, NULL, 10 * k, now(), now(), 'TC-PA' || grid_idx::text || '-' || k::text);
        serial := serial + 1;
      END LOOP;

      FOR k IN 1..2 LOOP
        i_id := uuid_generate_v4();
        INSERT INTO public.items (id, inventory_code, model_id, owner_id, inventory_pool_id, parent_id, room_id,
                                 is_borrowable, is_broken, is_incomplete, retired, retired_reason, price, created_at, updated_at, shelf)
        VALUES (
          i_id,
          'testcomplex-c' || to_char(grid_idx, 'FM00') || '-inv-' || lpad(serial::text, 3, '0'),
          m_ch2, v_pool, v_pool, i_root, v_room,
          true, false, false, NULL, NULL, 20 * k, now(), now(), 'TC-PB' || grid_idx::text || '-L' || k::text);
        INSERT INTO public.reservations (
          id, contract_id, inventory_pool_id, user_id, type, status, item_id, model_id,
          quantity, start_date, end_date, returned_date, created_at, updated_at)
        VALUES (
          uuid_generate_v4(), v_contract, v_pool, v_user, 'ItemLine', 'signed', i_id, m_ch2,
          1, CURRENT_DATE - 5, CURRENT_DATE + 60, NULL, now(), now());
        serial := serial + 1;
      END LOOP;

      i_id := uuid_generate_v4();
      INSERT INTO public.items (id, inventory_code, model_id, owner_id, inventory_pool_id, parent_id, room_id,
                               is_borrowable, is_broken, is_incomplete, retired, retired_reason, price, created_at, updated_at, shelf)
      VALUES (
        i_id,
        'testcomplex-c' || to_char(grid_idx, 'FM00') || '-inv-' || lpad(serial::text, 3, '0'),
        m_ch1, v_pool, v_pool, i_root, v_room,
        true, false, false, NULL, NULL, 11, now(), now(), 'TC-PR' || grid_idx::text);
      INSERT INTO public.reservations (
        id, contract_id, inventory_pool_id, user_id, type, status, item_id, model_id,
        quantity, start_date, end_date, returned_date, created_at, updated_at)
      VALUES (
        uuid_generate_v4(), v_contract, v_pool, v_user, 'ItemLine', 'signed', i_id, m_ch1,
        1, CURRENT_DATE - 5, CURRENT_DATE + 60, CURRENT_DATE - 1, now(), now());

    ELSE
      m_shell := uuid_generate_v4();
      INSERT INTO public.models (id, type, manufacturer, product, version, is_package, created_at, updated_at)
      VALUES (
        m_shell, 'Model', 'TestComplex',
        'testcomplex-c' || to_char(grid_idx, 'FM00') || '-pkg-shell',
        'shell', true, now(), now());

      i_root := uuid_generate_v4();
      INSERT INTO public.items (id, inventory_code, model_id, owner_id, inventory_pool_id, parent_id, room_id,
                               is_borrowable, is_broken, is_incomplete, retired, retired_reason, price, created_at, updated_at, shelf)
      VALUES (
        i_root,
        'testcomplex-c' || to_char(grid_idx, 'FM00') || '-inv-shell-000',
        m_shell, v_pool, v_pool, NULL, v_room,
        true, false, false, NULL, NULL, 1000 + grid_idx, now(), now(), 'TC-SH' || grid_idx::text);

      n_avail   := 3 + (grid_idx % 4);
      n_lent    := 2 + (grid_idx % 3);
      n_ret     := 2;
      n_retired := 2 + (grid_idx % 2);
      n_nb      := 1 + (grid_idx % 2);
      n_broken  := 2;
      n_appr    := 1 + (grid_idx % 2);

      serial := 1;

      n_pkg_slot := least(n_avail, greatest(0, round(n_avail * p_in_pkg)::int));
      FOR k IN 1..n_avail LOOP
        i_id := uuid_generate_v4();
        INSERT INTO public.items (id, inventory_code, model_id, owner_id, inventory_pool_id, parent_id, room_id,
                                 is_borrowable, is_broken, is_incomplete, retired, retired_reason, price, created_at, updated_at, shelf)
        VALUES (
          i_id,
          'testcomplex-c' || to_char(grid_idx, 'FM00') || '-inv-' || lpad(serial::text, 3, '0'),
          m_id, v_pool, v_pool, CASE WHEN k <= n_pkg_slot THEN i_root ELSE NULL END, v_room,
          true, false, false, NULL, NULL, serial::numeric, now(), now(),
          'TC-' || grid_idx::text || '-A' || k::text);
        serial := serial + 1;
      END LOOP;

      n_pkg_slot := least(n_lent, greatest(0, round(n_lent * p_in_pkg)::int));
      FOR k IN 1..n_lent LOOP
        i_id := uuid_generate_v4();
        INSERT INTO public.items (id, inventory_code, model_id, owner_id, inventory_pool_id, parent_id, room_id,
                                 is_borrowable, is_broken, is_incomplete, retired, retired_reason, price, created_at, updated_at, shelf)
        VALUES (
          i_id,
          'testcomplex-c' || to_char(grid_idx, 'FM00') || '-inv-' || lpad(serial::text, 3, '0'),
          m_id, v_pool, v_pool, CASE WHEN k <= n_pkg_slot THEN i_root ELSE NULL END, v_room,
          true, false, false, NULL, NULL, serial::numeric, now(), now(),
          'TC-' || grid_idx::text || '-L' || k::text);
        INSERT INTO public.reservations (
          id, contract_id, inventory_pool_id, user_id, type, status, item_id, model_id,
          quantity, start_date, end_date, returned_date, created_at, updated_at)
        VALUES (
          uuid_generate_v4(), v_contract, v_pool, v_user, 'ItemLine', 'signed', i_id, m_id,
          1, CURRENT_DATE - 5, CURRENT_DATE + 60, NULL, now(), now());
        serial := serial + 1;
      END LOOP;

      n_pkg_slot := least(n_ret, greatest(0, round(n_ret * p_in_pkg)::int));
      FOR k IN 1..n_ret LOOP
        i_id := uuid_generate_v4();
        INSERT INTO public.items (id, inventory_code, model_id, owner_id, inventory_pool_id, parent_id, room_id,
                                 is_borrowable, is_broken, is_incomplete, retired, retired_reason, price, created_at, updated_at, shelf)
        VALUES (
          i_id,
          'testcomplex-c' || to_char(grid_idx, 'FM00') || '-inv-' || lpad(serial::text, 3, '0'),
          m_id, v_pool, v_pool, CASE WHEN k <= n_pkg_slot THEN i_root ELSE NULL END, v_room,
          true, false, false, NULL, NULL, serial::numeric, now(), now(),
          'TC-' || grid_idx::text || '-R' || k::text);
        INSERT INTO public.reservations (
          id, contract_id, inventory_pool_id, user_id, type, status, item_id, model_id,
          quantity, start_date, end_date, returned_date, created_at, updated_at)
        VALUES (
          uuid_generate_v4(), v_contract, v_pool, v_user, 'ItemLine', 'signed', i_id, m_id,
          1, CURRENT_DATE - 5, CURRENT_DATE + 60, CURRENT_DATE - k, now(), now());
        serial := serial + 1;
      END LOOP;

      n_pkg_slot := least(n_retired, greatest(0, round(n_retired * p_in_pkg)::int));
      FOR k IN 1..n_retired LOOP
        i_id := uuid_generate_v4();
        INSERT INTO public.items (id, inventory_code, model_id, owner_id, inventory_pool_id, parent_id, room_id,
                                 is_borrowable, is_broken, is_incomplete, retired, retired_reason, price, created_at, updated_at, shelf)
        VALUES (
          i_id,
          'testcomplex-c' || to_char(grid_idx, 'FM00') || '-inv-' || lpad(serial::text, 3, '0'),
          m_id, v_pool, v_pool, CASE WHEN k <= n_pkg_slot THEN i_root ELSE NULL END, v_room,
          true, false, false, CURRENT_DATE, 'testcomplex grid ' || grid_idx::text, serial::numeric, now(), now(),
          'TC-' || grid_idx::text || '-T' || k::text);
        serial := serial + 1;
      END LOOP;

      n_pkg_slot := least(n_nb, greatest(0, round(n_nb * p_in_pkg)::int));
      FOR k IN 1..n_nb LOOP
        i_id := uuid_generate_v4();
        INSERT INTO public.items (id, inventory_code, model_id, owner_id, inventory_pool_id, parent_id, room_id,
                                 is_borrowable, is_broken, is_incomplete, retired, retired_reason, price, created_at, updated_at, shelf)
        VALUES (
          i_id,
          'testcomplex-c' || to_char(grid_idx, 'FM00') || '-inv-' || lpad(serial::text, 3, '0'),
          m_id, v_pool, v_pool, CASE WHEN k <= n_pkg_slot THEN i_root ELSE NULL END, v_room,
          false, false, false, NULL, NULL, serial::numeric, now(), now(),
          'TC-' || grid_idx::text || '-N' || k::text);
        serial := serial + 1;
      END LOOP;

      n_pkg_slot := least(n_broken, greatest(0, round(n_broken * p_in_pkg)::int));
      FOR k IN 1..n_broken LOOP
        i_id := uuid_generate_v4();
        INSERT INTO public.items (id, inventory_code, model_id, owner_id, inventory_pool_id, parent_id, room_id,
                                 is_borrowable, is_broken, is_incomplete, retired, retired_reason, price, created_at, updated_at, shelf)
        VALUES (
          i_id,
          'testcomplex-c' || to_char(grid_idx, 'FM00') || '-inv-' || lpad(serial::text, 3, '0'),
          m_id, v_pool, v_pool, CASE WHEN k <= n_pkg_slot THEN i_root ELSE NULL END, v_room,
          true, true, true, NULL, NULL, serial::numeric, now(), now(),
          'TC-' || grid_idx::text || '-B' || k::text);
        serial := serial + 1;
      END LOOP;

      n_pkg_slot := least(n_appr, greatest(0, round(n_appr * p_in_pkg)::int));
      FOR k IN 1..n_appr LOOP
        i_id := uuid_generate_v4();
        INSERT INTO public.items (id, inventory_code, model_id, owner_id, inventory_pool_id, parent_id, room_id,
                                 is_borrowable, is_broken, is_incomplete, retired, retired_reason, price, created_at, updated_at, shelf)
        VALUES (
          i_id,
          'testcomplex-c' || to_char(grid_idx, 'FM00') || '-inv-' || lpad(serial::text, 3, '0'),
          m_id, v_pool, v_pool, CASE WHEN k <= n_pkg_slot THEN i_root ELSE NULL END, v_room,
          true, false, false, NULL, NULL, serial::numeric, now(), now(),
          'TC-' || grid_idx::text || '-Q' || k::text);
        INSERT INTO public.reservations (
          id, contract_id, inventory_pool_id, user_id, type, status, item_id, model_id,
          quantity, start_date, end_date, returned_date, created_at, updated_at)
        VALUES (
          uuid_generate_v4(), NULL, v_pool, v_user, 'ItemLine', 'approved', i_id, m_id,
          1, CURRENT_DATE - 5, CURRENT_DATE + 60, NULL, now(), now());
        serial := serial + 1;
      END LOOP;

    END IF;
  END LOOP;
END $$;

COMMIT;
