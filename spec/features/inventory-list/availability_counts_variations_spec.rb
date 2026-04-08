require "features_helper"
require_relative "../shared/common"

# Inventory list headline counts (in stock | rentable): models, packages, options, software;
# borrowable vs not, retired filter, broken/incomplete, non-borrowable+broken+incomplete,
# signed reservations on model / package parent / software license, broken+rented model line,
# Status submenu filters (Owned, In stock, Broken, Incomplete) vs headline + row item counts.
# See AVAILABILITY-COUNTS-VARIATIONS-COVERAGE.md.
feature "Inventory list availability counts (models, packages, options, software)", type: :feature do
  scenario "builds rows stepwise and checks availability numbers" do
    building = FactoryBot.create(:building, name: "AVCnt B", code: "AC1")
    room = FactoryBot.create(:room, name: "AVCnt R", building: building)
    pool = FactoryBot.create(:inventory_pool, shortname: "AC")
    pool_owner_other = FactoryBot.create(:inventory_pool, shortname: "AX")
    user = FactoryBot.create(:user, language_locale: "en-GB")
    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)

    token = "AVCnt-Parity"

    plain = FactoryBot.create(:leihs_model,
      product: "#{token} Plain",
      version: "p1")
    pkg = FactoryBot.create(:leihs_model,
      product: "#{token} Package",
      version: "p1",
      is_package: true)
    software = FactoryBot.create(:leihs_model,
      product: "#{token} Software",
      version: "s1",
      type: "Software")
    option = FactoryBot.create(:option,
      product: "#{token} Option",
      version: "o1",
      inventory_code: "#{token}-OPT",
      inventory_pool: pool,
      price: 55.00)

    login(user)

    refresh_list = lambda do |pool_id, search_term, retired: "false"|
      visit "/inventory/#{pool_id}/list?retired=#{retired}&page=1&size=50"
      find("input[name='search']").set(search_term)
      await_debounce
    end

    refresh_list.call(pool.id, token)

    expect(page).to have_css("table tbody tr", minimum: 1)

    verify_inventory_list_row(plain.reload, "0 | 0", [])
    verify_inventory_list_row(pkg.reload, "0 | 0", [])
    verify_inventory_list_row(software.reload, "0 | 0", [])
    verify_inventory_list_row(option.reload, "55 GBP", [], is_option: true)

    i_plain_1 = FactoryBot.create(:item,
      inventory_code: "#{pool.shortname}P01",
      owner_id: pool.id,
      inventory_pool_id: pool.id,
      leihs_model: plain,
      room: room,
      shelf: "S-P01",
      is_borrowable: true,
      retired: nil)

    refresh_list.call(pool.id, token)

    verify_inventory_list_row(plain.reload, "1 | 1", [
      {
        inventory_code: i_plain_1.inventory_code,
        building_name: building.name,
        building_code: building.code,
        shelf: i_plain_1.shelf,
        statuses: ["Available"]
      }
    ])

    i_plain_2 = FactoryBot.create(:item,
      inventory_code: "#{pool.shortname}P02",
      owner_id: pool.id,
      inventory_pool_id: pool.id,
      leihs_model: plain,
      room: room,
      shelf: "S-P02",
      is_borrowable: false,
      retired: nil)

    refresh_list.call(pool.id, token)

    verify_inventory_list_row(plain.reload, "1 | 1", [
      {
        inventory_code: i_plain_1.inventory_code,
        building_name: building.name,
        building_code: building.code,
        shelf: i_plain_1.shelf,
        statuses: ["Available"]
      },
      {
        inventory_code: i_plain_2.inventory_code,
        building_name: building.name,
        building_code: building.code,
        shelf: i_plain_2.shelf,
        statuses: ["Not borrowable"]
      }
    ])

    i_plain_3 = FactoryBot.create(:item,
      inventory_code: "#{pool.shortname}P03",
      owner_id: pool.id,
      inventory_pool_id: pool.id,
      leihs_model: plain,
      room: room,
      shelf: "S-P03",
      is_borrowable: true,
      retired: nil)

    refresh_list.call(pool.id, token)

    verify_inventory_list_row(plain.reload, "2 | 2", [
      {
        inventory_code: i_plain_1.inventory_code,
        building_name: building.name,
        building_code: building.code,
        shelf: i_plain_1.shelf,
        statuses: ["Available"]
      },
      {
        inventory_code: i_plain_2.inventory_code,
        building_name: building.name,
        building_code: building.code,
        shelf: i_plain_2.shelf,
        statuses: ["Not borrowable"]
      },
      {
        inventory_code: i_plain_3.inventory_code,
        building_name: building.name,
        building_code: building.code,
        shelf: i_plain_3.shelf,
        statuses: ["Available"]
      }
    ])

    i_plain_4 = FactoryBot.create(:item,
      inventory_code: "#{pool.shortname}P04",
      owner_id: pool.id,
      inventory_pool_id: pool.id,
      leihs_model: plain,
      room: room,
      shelf: "S-P04",
      is_borrowable: true,
      retired: Date.yesterday,
      retired_reason: "test retired")

    refresh_list.call(pool.id, token)

    verify_inventory_list_row(plain.reload, "2 | 2", [
      {
        inventory_code: i_plain_1.inventory_code,
        building_name: building.name,
        building_code: building.code,
        shelf: i_plain_1.shelf,
        statuses: ["Available"]
      },
      {
        inventory_code: i_plain_2.inventory_code,
        building_name: building.name,
        building_code: building.code,
        shelf: i_plain_2.shelf,
        statuses: ["Not borrowable"]
      },
      {
        inventory_code: i_plain_3.inventory_code,
        building_name: building.name,
        building_code: building.code,
        shelf: i_plain_3.shelf,
        statuses: ["Available"]
      }
    ])

    child_a = FactoryBot.create(:leihs_model,
      product: "#{token} PChild A",
      version: "c1")
    child_b = FactoryBot.create(:leihs_model,
      product: "#{token} PChild B",
      version: "c1")

    pkg_parent = FactoryBot.create(:item,
      inventory_code: "#{pool.shortname}PKG1",
      owner_id: pool.id,
      inventory_pool_id: pool.id,
      leihs_model: pkg,
      room: room,
      shelf: "S-PKG",
      is_borrowable: true,
      retired: nil)

    i_pkg_a = FactoryBot.create(:item,
      inventory_code: "#{pool.shortname}PKA1",
      parent_id: pkg_parent.id,
      leihs_model: child_a,
      owner_id: pool.id,
      inventory_pool_id: pool.id,
      room: room,
      shelf: "S-PKA")

    i_pkg_b = FactoryBot.create(:item,
      inventory_code: "#{pool.shortname}PKB1",
      parent_id: pkg_parent.id,
      leihs_model: child_b,
      owner_id: pool.id,
      inventory_pool_id: pool.id,
      room: room,
      shelf: "S-PKB")

    refresh_list.call(pool.id, token)

    verify_inventory_list_row(pkg.reload, "1 | 1", [
      {
        inventory_code: pkg_parent.inventory_code,
        building_name: building.name,
        building_code: building.code,
        shelf: pkg_parent.shelf,
        statuses: ["Available"],
        package_items: [
          {
            inventory_code: i_pkg_a.inventory_code,
            statuses: ["Available"]
          },
          {
            inventory_code: i_pkg_b.inventory_code,
            statuses: ["Available"]
          }
        ]
      }
    ],
      is_package: true)

    pkg_borrower = FactoryBot.create(:user)
    pkg_contract = Contract.create_with_disabled_triggers(
      SecureRandom.uuid,
      pkg_borrower.id,
      pool.id
    )
    pkg_reservation = FactoryBot.create(:reservation,
      status: :signed,
      leihs_model: pkg,
      item_id: pkg_parent.id,
      contract_id: pkg_contract.id,
      user_id: pkg_borrower.id,
      inventory_pool_id: pool.id)

    refresh_list.call(pool.id, token)

    verify_inventory_list_row(pkg.reload, "0 | 1", [
      {
        inventory_code: pkg_parent.inventory_code,
        reservation_user_name: "#{pkg_borrower.firstname} #{pkg_borrower.lastname}",
        reservation_end_date: pkg_reservation.end_date.strftime("%d.%m.%Y"),
        statuses: ["Rented"],
        package_items: [
          {
            inventory_code: i_pkg_a.inventory_code,
            statuses: ["Available"]
          },
          {
            inventory_code: i_pkg_b.inventory_code,
            statuses: ["Available"]
          }
        ]
      }
    ],
      is_package: true)

    verify_inventory_list_row(option.reload, "55 GBP", [], is_option: true)

    lic_props = {license_type: "free", operating_system: %w[windows]}

    lic_1 = FactoryBot.create(:item,
      inventory_code: "#{pool.shortname}L01",
      owner_id: pool.id,
      inventory_pool_id: pool.id,
      leihs_model: software,
      room: room,
      shelf: "S-L01",
      is_borrowable: true,
      retired: nil,
      properties: lic_props)

    refresh_list.call(pool.id, token)

    verify_inventory_list_row(software.reload, "1 | 1", [
      {
        inventory_code: lic_1.inventory_code,
        license_type_label: "Free",
        os_labels: ["Windows"],
        statuses: ["Available"]
      }
    ])

    lic_2 = FactoryBot.create(:item,
      inventory_code: "#{pool.shortname}L02",
      owner_id: pool_owner_other.id,
      inventory_pool_id: pool.id,
      leihs_model: software,
      room: room,
      shelf: "S-L02",
      is_borrowable: false,
      retired: nil,
      properties: lic_props)

    refresh_list.call(pool.id, token)

    verify_inventory_list_row(software.reload, "1 | 1", [
      {
        inventory_code: lic_1.inventory_code,
        license_type_label: "Free",
        os_labels: ["Windows"],
        statuses: ["Available"]
      },
      {
        inventory_code: lic_2.inventory_code,
        license_type_label: "Free",
        os_labels: ["Windows"],
        responsible_pool_name: pool.name,
        statuses: ["Not borrowable"]
      }
    ])

    lic_3 = FactoryBot.create(:item,
      inventory_code: "#{pool.shortname}L03",
      owner_id: pool.id,
      inventory_pool_id: pool.id,
      leihs_model: software,
      room: room,
      shelf: "S-L03",
      is_borrowable: true,
      retired: Date.yesterday,
      retired_reason: "license retired",
      properties: lic_props)

    refresh_list.call(pool.id, token)

    verify_inventory_list_row(software.reload, "1 | 1", [
      {
        inventory_code: lic_1.inventory_code,
        license_type_label: "Free",
        os_labels: ["Windows"],
        statuses: ["Available"]
      },
      {
        inventory_code: lic_2.inventory_code,
        license_type_label: "Free",
        os_labels: ["Windows"],
        responsible_pool_name: pool.name,
        statuses: ["Not borrowable"]
      }
    ])

    sw_borrower = FactoryBot.create(:user)
    sw_contract = Contract.create_with_disabled_triggers(
      SecureRandom.uuid,
      sw_borrower.id,
      pool.id
    )
    sw_reservation = FactoryBot.create(:reservation,
      status: :signed,
      leihs_model: software,
      item_id: lic_1.id,
      contract_id: sw_contract.id,
      user_id: sw_borrower.id,
      inventory_pool_id: pool.id)

    refresh_list.call(pool.id, token)

    verify_inventory_list_row(software.reload, "0 | 1", [
      {
        inventory_code: lic_1.inventory_code,
        reservation_user_name: "#{sw_borrower.firstname} #{sw_borrower.lastname}",
        reservation_end_date: sw_reservation.end_date.strftime("%d.%m.%Y"),
        statuses: ["Rented"]
      },
      {
        inventory_code: lic_2.inventory_code,
        license_type_label: "Free",
        os_labels: ["Windows"],
        responsible_pool_name: pool.name,
        statuses: ["Not borrowable"]
      }
    ])

    refresh_list.call(pool.id, token, retired: "true")

    verify_inventory_list_row(plain.reload, "1 | 1", [
      {
        inventory_code: i_plain_4.inventory_code,
        building_name: building.name,
        building_code: building.code,
        shelf: i_plain_4.shelf,
        statuses: ["Not borrowable", "Available", "Retired"]
      }
    ])

    verify_inventory_list_row(software.reload, "1 | 1", [
      {
        inventory_code: lic_3.inventory_code,
        license_type_label: "Free",
        os_labels: ["Windows"],
        statuses: ["Not borrowable", "Available", "Retired"]
      }
    ])
  end

  scenario "broken, incomplete, non-borrowable, retired, and rented lines" do
    building = FactoryBot.create(:building, name: "AVCnt SB", code: "SB1")
    room = FactoryBot.create(:room, name: "AVCnt SR", building: building)
    pool = FactoryBot.create(:inventory_pool, shortname: "SB")
    user = FactoryBot.create(:user, language_locale: "en-GB")
    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)

    st_token = "AVCnt-StMx"
    st_model = FactoryBot.create(:leihs_model,
      product: "#{st_token} Model",
      version: "v1")

    login(user)

    refresh_st = lambda do |pool_id, retired: "false"|
      visit "/inventory/#{pool_id}/list?retired=#{retired}&page=1&size=50"
      find("input[name='search']").set(st_token)
      await_debounce
    end

    refresh_st.call(pool.id)

    verify_inventory_list_row(st_model.reload, "0 | 0", [])

    st01 = FactoryBot.create(:item,
      inventory_code: "#{pool.shortname}ST01",
      owner_id: pool.id,
      inventory_pool_id: pool.id,
      leihs_model: st_model,
      room: room,
      shelf: "ST-01",
      is_borrowable: true,
      retired: nil,
      is_broken: false,
      is_incomplete: false)

    refresh_st.call(pool.id)

    rows = [
      {
        inventory_code: st01.inventory_code,
        building_name: building.name,
        building_code: building.code,
        shelf: st01.shelf,
        statuses: ["Available"]
      }
    ]
    verify_inventory_list_row(st_model.reload, "1 | 1", rows)

    st02 = FactoryBot.create(:item,
      inventory_code: "#{pool.shortname}ST02",
      owner_id: pool.id,
      inventory_pool_id: pool.id,
      leihs_model: st_model,
      room: room,
      shelf: "ST-02",
      is_borrowable: true,
      retired: nil,
      is_broken: true,
      is_incomplete: false)

    refresh_st.call(pool.id)

    rows << {
      inventory_code: st02.inventory_code,
      building_name: building.name,
      building_code: building.code,
      shelf: st02.shelf,
      statuses: ["Broken", "Available"]
    }
    verify_inventory_list_row(st_model.reload, "2 | 2", rows)

    st03 = FactoryBot.create(:item,
      inventory_code: "#{pool.shortname}ST03",
      owner_id: pool.id,
      inventory_pool_id: pool.id,
      leihs_model: st_model,
      room: room,
      shelf: "ST-03",
      is_borrowable: true,
      retired: nil,
      is_broken: false,
      is_incomplete: true)

    refresh_st.call(pool.id)

    rows << {
      inventory_code: st03.inventory_code,
      building_name: building.name,
      building_code: building.code,
      shelf: st03.shelf,
      statuses: ["Incomplete", "Available"]
    }
    verify_inventory_list_row(st_model.reload, "3 | 3", rows)

    st04 = FactoryBot.create(:item,
      inventory_code: "#{pool.shortname}ST04",
      owner_id: pool.id,
      inventory_pool_id: pool.id,
      leihs_model: st_model,
      room: room,
      shelf: "ST-04",
      is_borrowable: true,
      retired: nil,
      is_broken: true,
      is_incomplete: true)

    refresh_st.call(pool.id)

    rows << {
      inventory_code: st04.inventory_code,
      building_name: building.name,
      building_code: building.code,
      shelf: st04.shelf,
      statuses: ["Broken", "Available", "Incomplete"]
    }
    verify_inventory_list_row(st_model.reload, "4 | 4", rows)

    st05 = FactoryBot.create(:item,
      inventory_code: "#{pool.shortname}ST05",
      owner_id: pool.id,
      inventory_pool_id: pool.id,
      leihs_model: st_model,
      room: room,
      shelf: "ST-05",
      is_borrowable: false,
      retired: nil,
      is_broken: true,
      is_incomplete: true)

    refresh_st.call(pool.id)

    rows << {
      inventory_code: st05.inventory_code,
      building_name: building.name,
      building_code: building.code,
      shelf: st05.shelf,
      statuses: ["Broken", "Incomplete", "Not borrowable"]
    }
    verify_inventory_list_row(st_model.reload, "4 | 4", rows)

    st06 = FactoryBot.create(:item,
      inventory_code: "#{pool.shortname}ST06",
      owner_id: pool.id,
      inventory_pool_id: pool.id,
      leihs_model: st_model,
      room: room,
      shelf: "ST-06",
      is_borrowable: true,
      retired: Date.yesterday,
      retired_reason: "status-matrix retired",
      is_broken: false,
      is_incomplete: false)

    refresh_st.call(pool.id)

    verify_inventory_list_row(st_model.reload, "4 | 4", rows)

    borrower = FactoryBot.create(:user)
    contract = Contract.create_with_disabled_triggers(
      SecureRandom.uuid,
      borrower.id,
      pool.id
    )

    st07 = FactoryBot.create(:item,
      inventory_code: "#{pool.shortname}ST07",
      owner_id: pool.id,
      inventory_pool_id: pool.id,
      leihs_model: st_model,
      room: room,
      shelf: "ST-07",
      is_borrowable: true,
      retired: nil,
      is_broken: false,
      is_incomplete: false)

    reservation = FactoryBot.create(:reservation,
      status: :signed,
      leihs_model: st_model,
      item_id: st07.id,
      contract_id: contract.id,
      user_id: borrower.id,
      inventory_pool_id: pool.id)

    refresh_st.call(pool.id)

    rows << {
      inventory_code: st07.inventory_code,
      reservation_user_name: "#{borrower.firstname} #{borrower.lastname}",
      reservation_end_date: reservation.end_date.strftime("%d.%m.%Y"),
      statuses: ["Rented"]
    }
    verify_inventory_list_row(st_model.reload, "4 | 5", rows)

    st08 = FactoryBot.create(:item,
      inventory_code: "#{pool.shortname}ST08",
      owner_id: pool.id,
      inventory_pool_id: pool.id,
      leihs_model: st_model,
      room: room,
      shelf: "ST-08",
      is_borrowable: true,
      retired: nil,
      is_broken: true,
      is_incomplete: false)

    broken_borrower = FactoryBot.create(:user)
    broken_contract = Contract.create_with_disabled_triggers(
      SecureRandom.uuid,
      broken_borrower.id,
      pool.id
    )
    broken_reservation = FactoryBot.create(:reservation,
      status: :signed,
      leihs_model: st_model,
      item_id: st08.id,
      contract_id: broken_contract.id,
      user_id: broken_borrower.id,
      inventory_pool_id: pool.id)

    refresh_st.call(pool.id)

    rows << {
      inventory_code: st08.inventory_code,
      reservation_user_name: "#{broken_borrower.firstname} #{broken_borrower.lastname}",
      reservation_end_date: broken_reservation.end_date.strftime("%d.%m.%Y"),
      statuses: ["Broken", "Rented"]
    }
    verify_inventory_list_row(st_model.reload, "4 | 6", rows)

    st09 = FactoryBot.create(:item,
      inventory_code: "#{pool.shortname}ST09",
      owner_id: pool.id,
      inventory_pool_id: pool.id,
      leihs_model: st_model,
      room: room,
      shelf: "ST-09",
      is_borrowable: true,
      retired: nil,
      is_broken: false,
      is_incomplete: true)

    inc_borrower = FactoryBot.create(:user)
    inc_contract = Contract.create_with_disabled_triggers(
      SecureRandom.uuid,
      inc_borrower.id,
      pool.id
    )
    inc_reservation = FactoryBot.create(:reservation,
      status: :signed,
      leihs_model: st_model,
      item_id: st09.id,
      contract_id: inc_contract.id,
      user_id: inc_borrower.id,
      inventory_pool_id: pool.id)

    refresh_st.call(pool.id)

    rows << {
      inventory_code: st09.inventory_code,
      reservation_user_name: "#{inc_borrower.firstname} #{inc_borrower.lastname}",
      reservation_end_date: inc_reservation.end_date.strftime("%d.%m.%Y"),
      statuses: ["Incomplete", "Rented"]
    }
    verify_inventory_list_row(st_model.reload, "4 | 7", rows)

    refresh_st.call(pool.id, retired: "true")

    verify_inventory_list_row(st_model.reload, "1 | 1", [
      {
        inventory_code: st06.inventory_code,
        building_name: building.name,
        building_code: building.code,
        shelf: st06.shelf,
        statuses: ["Not borrowable", "Available", "Retired"]
      }
    ])
  end

  scenario "Status filters (Owned, In stock, Broken, Incomplete) keep headline availability" do
    building = FactoryBot.create(:building, name: "AVCnt FB", code: "FB1")
    room = FactoryBot.create(:room, name: "AVCnt FR", building: building)
    pool = FactoryBot.create(:inventory_pool, shortname: "FB")
    pool_other = FactoryBot.create(:inventory_pool, shortname: "FY")
    user = FactoryBot.create(:user, language_locale: "en-GB")
    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)

    token = "AVCnt-Filt"
    filt_model = FactoryBot.create(:leihs_model,
      product: "#{token} Model",
      version: "v1")

    i1 = FactoryBot.create(:item,
      inventory_code: "#{pool.shortname}F01",
      owner_id: pool.id,
      inventory_pool_id: pool.id,
      leihs_model: filt_model,
      room: room,
      shelf: "F-01",
      is_borrowable: true,
      retired: nil,
      is_broken: false,
      is_incomplete: false)

    i2 = FactoryBot.create(:item,
      inventory_code: "#{pool.shortname}F02",
      owner_id: pool.id,
      inventory_pool_id: pool.id,
      leihs_model: filt_model,
      room: room,
      shelf: "F-02",
      is_borrowable: true,
      retired: nil,
      is_broken: true,
      is_incomplete: false)

    i3 = FactoryBot.create(:item,
      inventory_code: "#{pool.shortname}F03",
      owner_id: pool.id,
      inventory_pool_id: pool.id,
      leihs_model: filt_model,
      room: room,
      shelf: "F-03",
      is_borrowable: true,
      retired: nil,
      is_broken: false,
      is_incomplete: true)

    i4 = FactoryBot.create(:item,
      inventory_code: "#{pool.shortname}F04",
      owner_id: pool.id,
      inventory_pool_id: pool.id,
      leihs_model: filt_model,
      room: room,
      shelf: "F-04",
      is_borrowable: true,
      retired: nil,
      is_broken: true,
      is_incomplete: true)

    login(user)

    go_filt = lambda do
      visit "/inventory/#{pool.id}/list?retired=false&page=1&size=50"
      find("input[name='search']").set(token)
      await_debounce
    end

    row_i1 = lambda do
      {
        inventory_code: i1.inventory_code,
        building_name: building.name,
        building_code: building.code,
        shelf: i1.shelf,
        statuses: ["Available"]
      }
    end

    row_i2 = lambda do
      {
        inventory_code: i2.inventory_code,
        building_name: building.name,
        building_code: building.code,
        shelf: i2.shelf,
        statuses: ["Broken", "Available"]
      }
    end

    row_i3 = lambda do
      {
        inventory_code: i3.inventory_code,
        building_name: building.name,
        building_code: building.code,
        shelf: i3.shelf,
        statuses: ["Incomplete", "Available"]
      }
    end

    row_i4 = lambda do
      {
        inventory_code: i4.inventory_code,
        building_name: building.name,
        building_code: building.code,
        shelf: i4.shelf,
        statuses: ["Broken", "Available", "Incomplete"]
      }
    end

    go_filt.call

    verify_inventory_list_row(filt_model.reload, "4 | 4", [
      row_i1.call,
      row_i2.call,
      row_i3.call,
      row_i4.call
    ])

    select_status_filter_submenu("Broken", "Yes")
    await_debounce

    verify_inventory_list_row(filt_model.reload, "4 | 4", [
      row_i2.call,
      row_i4.call
    ])

    go_filt.call

    select_status_filter_submenu("Incomplete", "Yes")
    await_debounce

    verify_inventory_list_row(filt_model.reload, "4 | 4", [
      row_i3.call,
      row_i4.call
    ])

    go_filt.call

    select_status_filter_submenu("Broken", "Yes")
    await_debounce
    select_status_filter_submenu("Incomplete", "Yes")
    await_debounce

    verify_inventory_list_row(filt_model.reload, "4 | 4", [
      row_i4.call
    ])

    i5 = FactoryBot.create(:item,
      inventory_code: "#{pool.shortname}F05",
      owner_id: pool_other.id,
      inventory_pool_id: pool.id,
      leihs_model: filt_model,
      room: room,
      shelf: "F-05",
      is_borrowable: true,
      retired: nil,
      is_broken: false,
      is_incomplete: false)

    go_filt.call

    verify_inventory_list_row(filt_model.reload, "5 | 5", [
      row_i1.call,
      row_i2.call,
      row_i3.call,
      row_i4.call,
      {
        inventory_code: i5.inventory_code,
        building_name: building.name,
        building_code: building.code,
        shelf: i5.shelf,
        statuses: ["Available"]
      }
    ])

    select_status_filter_submenu("Owned", "Yes")
    await_debounce

    verify_inventory_list_row(filt_model.reload, "5 | 5", [
      row_i1.call,
      row_i2.call,
      row_i3.call,
      row_i4.call
    ])

    renter = FactoryBot.create(:user)
    rent_contract = Contract.create_with_disabled_triggers(
      SecureRandom.uuid,
      renter.id,
      pool.id
    )
    rent_res = FactoryBot.create(:reservation,
      status: :signed,
      leihs_model: filt_model,
      item_id: i1.id,
      contract_id: rent_contract.id,
      user_id: renter.id,
      inventory_pool_id: pool.id)

    go_filt.call

    row_i1_rented = row_i1.call.merge(
      reservation_user_name: "#{renter.firstname} #{renter.lastname}",
      reservation_end_date: rent_res.end_date.strftime("%d.%m.%Y"),
      statuses: ["Rented"]
    )

    verify_inventory_list_row(filt_model.reload, "4 | 5", [
      row_i1_rented,
      row_i2.call,
      row_i3.call,
      row_i4.call,
      {
        inventory_code: i5.inventory_code,
        building_name: building.name,
        building_code: building.code,
        shelf: i5.shelf,
        statuses: ["Available"]
      }
    ])

    select_status_filter_submenu("In stock", "No")
    await_debounce

    within("tr", text: filt_model.reload.name) do
      expect(find('[data-test-id="items"]').text).to eq("1")
      expect(find('[data-test-id="availability"]').text).to eq("4 | 5")
    end
  end
end

def verify_inventory_list_row(model, availabilty, items = [], is_package: false, is_option: false)
  row = find("tr", text: model.name)

  within("tr", text: model.name) do
    if is_option
      expect(page).to have_selector('[data-test-id="items"]', visible: false)
      expect(page).to have_button("expand-button", visible: false)
      expect(find('[data-test-id="price"]').text).to eq(availabilty)
      return "option row correct"
    end

    if items.empty?
      expect(find('[data-test-id="items"]').text).to eq("0")
      expect(page).to have_button("expand-button", disabled: true)
      expect(find('[data-test-id="availability"]').text).to eq(availabilty)
      return "rows correct"
    end

    expect(page).to have_content(model.name)
    expect(page).to have_content(model.version)
    expect(find('[data-test-id="items"]').text).to eq(items.size.to_s)
    expect(find('[data-test-id="availability"]').text).to eq(availabilty)
    click_on "expand-button"

    following_rows = if is_package
      row.all(:xpath, "following-sibling::tr[@data-row='package']", wait: 30)
    else
      row.all(:xpath, "following-sibling::tr[@data-row='item']", wait: 30)
    end

    wait_until { following_rows.size == items.size }

    items.each_with_index do |details, index|
      expect(following_rows[index]).to have_content(details[:inventory_code])
      if details[:reservation_user_name]
        expect(following_rows[index]).to have_content(details[:reservation_user_name])
        expect(following_rows[index]).to have_content(details[:reservation_end_date])
        if details[:package_items]
          expect(following_rows[index].find('[data-test-id="items"]').text).to eq(details[:package_items].size.to_s)
        end
      elsif model.values[:type] == "Software"
        expect(following_rows[index]).to have_content(details[:license_type_label]) if details[:license_type_label]
        Array(details[:os_labels]).each do |os_label|
          expect(following_rows[index]).to have_content(os_label)
        end
        expect(following_rows[index]).to have_content(details[:responsible_pool_name]) if details[:responsible_pool_name]
      else
        expect(following_rows[index]).to have_content(details[:building_name])
        expect(following_rows[index]).to have_content(details[:building_code])
        expect(following_rows[index]).to have_content(details[:shelf])
      end

      if details[:statuses]
        status_texts = following_rows[index].all('[data-test-id="item-status"] span').map(&:text)
        expect(details[:statuses]).to include(*status_texts)
      end

      if details[:package_items]

        package_expand = following_rows[index].find('[data-test-id="expand-button"]')
        package_expand.click

        package_rows = []
        current_row = following_rows[index]
        while (next_row = current_row.first(:xpath, "following-sibling::tr[1]", minimum: 0, wait: 0))
          break unless next_row["data-row"] == "item"

          package_rows << next_row
          current_row = next_row
        end

        wait_until { package_rows.size == details[:package_items].size }
        expect(package_rows.size).to eq(details[:package_items].size)

        details[:package_items].each_with_index do |pkg_item, pkg_index|
          expect(package_rows[pkg_index]).to have_content(pkg_item[:inventory_code])
          expect(package_rows[pkg_index]).to have_content("is part of a package")

          if pkg_item[:statuses]
            status_texts = package_rows[pkg_index].all('[data-test-id="item-status"] span').map(&:text)
            expect(pkg_item[:statuses]).to include(*status_texts)
          end
        end

        package_expand.click
      end
    end
    click_on "expand-button"
    return "rows correct"
  end
end
