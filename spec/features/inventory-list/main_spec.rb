require "spec_helper"
require_relative "../shared/common"

feature "Inventory Page", type: :feature do
  scenario "shortcuts work" do
    user = FactoryBot.create(:user, language_locale: "en-GB")
    pool = FactoryBot.create(:inventory_pool, shortname: "PA")

    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)

    login(user)

    visit "/inventory"
    expect(page).to have_content "Inventory"
    find("nav button", text: "Inventory").click
    click_on pool.name
    expect(page).to have_content(pool.name)

    page.driver.browser.action.key_down(:shift)
      .key_down(:alt)
      .send_keys("f")
      .key_up(:alt)
      .key_up(:shift)
      .perform

    # Check if the input field with name "search" is focused
    expect(page.evaluate_script("document.activeElement.name")).to eq("search")

    # Select "retired" in the dropdown
    select_value("retired", "retired")
    expect(page).to have_content("retired")

    # Trigger Shift + Alt + R
    page.driver.browser.action.key_down(:shift)
      .key_down(:alt)
      .send_keys("r")
      .key_up(:alt)
      .key_up(:shift)
      .perform

    # Check if the query params still include "retired"
    expect(page.current_url).to_not include("retired")

    page.driver.browser.action.key_down(:shift)
      .key_down(:alt)
      .send_keys("n")
      .key_up(:alt)
      .key_up(:shift)
      .perform

    expect(page).to have_selector('[data-test-id="add-inventory-dropdown"]')
  end

  scenario "filters work" do
    # |----------  |---------|--------|--------|--------|---------|------------|----------|------------|--------|------------|
    # | model      | package | item   | owner  | pool   | retired | borrowable | in_stock | incomplete | broken | last_check |
    # |----------  |---------|--------|--------|--------|---------|------------|----------|------------|--------|------------|
    # | model_1    | false   |        |        |        |         |            |          |            |        |            |
    # | model_2    | false   | PA100  | pool_1 | pool_1 | false   | true       | true     | false      | false  | 2024-12-31 |
    # | model_3    | false   | PA101  | pool_1 | pool_1 | true    | true       | true     | false      | false  |            |
    # | model_4    | false   | PA102  | pool_1 | pool_1 | true    | false      | true     | true       | false  |            |
    # | model_5    | false   | PA103  | pool_1 | pool_2 | true    | true       | true     | false      | false  |            |
    # | model_6    | false   | PC104  | pool_3 | pool_1 | false   | true       | true     | false      | true   |            |
    # | model_7    | false   | PD105  | pool_4 | pool_4 | false   | true       | true     | false      | false  |            |
    # |            |
    # |-PACKAGE--  |---------|--------|--------|--------|---------|------------|----------|------------|--------|------------|
    # | model_8    | true    | PA106  | pool_1 | pool_1 | false   | true       | false    | false      | false  |            |
    # |   model_9  | false   | PA107  | pool_1 | pool_1 | false   | true       | false    | false      | false  |            |
    # |   model_10 | false   | PA108  | pool_1 | pool_1 | false   | true       | false    | false      | false  |            |
    # |----------  |---------|--------|--------|--------|---------|------------|----------|------------|--------|------------|
    #
    # |---------|--------|--------|
    # | model   | cat_1  | cat_2  |
    # |---------|--------|--------|
    # | model_1 | Audio  |        |
    # |---------|--------|--------|
    # | model_2 | Audio  | Mic    |
    # |---------|--------|--------|
    #
    # |----------|----------|---------|
    # | option   | inv_code | pool    |
    # |----------|----------|---------|
    # | option_1 | OP01     | pool_1  |
    # | option_2 | OP02     | pool_2  |
    # |----------|----------|---------|

    cat_1 = FactoryBot.create(:category, name: "Audio")
    cat_2 = FactoryBot.create(:category, name: "Mic")
    cat_1.add_child(cat_2)

    building = FactoryBot.create(:building,
      name: "Building", code: "B1")

    room = FactoryBot.create(:room,
      name: "R1", description: "Room 1", building: building)

    pool_1 = FactoryBot.create(:inventory_pool, shortname: "PA")
    pool_2 = FactoryBot.create(:inventory_pool, shortname: "PB")
    pool_3 = FactoryBot.create(:inventory_pool, shortname: "PC")
    pool_4 = FactoryBot.create(:inventory_pool, shortname: "PD")
    pool_5 = FactoryBot.create(:inventory_pool, shortname: "PE")

    # user = FactoryBot.create(:user)
    user = FactoryBot.create(:user, language_locale: "en-GB")

    [pool_1, pool_2, pool_5].each do |pool|
      FactoryBot.create(:access_right,
        inventory_pool: pool,
        user: user,
        role: :inventory_manager)
    end

    option_1 = FactoryBot.create(:option,
      product: "Option",
      version: "OP01",
      inventory_code: "OP01",
      inventory_pool: pool_1)
    FactoryBot.create(:option,
      product: "Option",
      version: "OP02",
      inventory_code: "OP02",
      inventory_pool: pool_2)

    search_term = "XYZ"

    model_1 = FactoryBot.create(:leihs_model, product: "Model_1", version: "AA1")
    model_2 = FactoryBot.create(:leihs_model, product: "Model_2", version: "AA2 #{search_term}")
    model_3 = FactoryBot.create(:leihs_model, product: "Model_3", version: "AA3")
    model_4 = FactoryBot.create(:leihs_model, product: "Model_4", version: "AA4")
    model_5 = FactoryBot.create(:leihs_model, product: "Model_5", version: "AA5")
    model_6 = FactoryBot.create(:leihs_model, product: "Model_6", version: "AA6")
    model_7 = FactoryBot.create(:leihs_model, product: "Model_7", version: "AA7")
    model_8 = FactoryBot.create(:leihs_model, product: "Model_8", version: "AA8", is_package: true)
    model_9 = FactoryBot.create(:leihs_model, product: "Model_9", version: "AA9 #{search_term}")
    model_10 = FactoryBot.create(:leihs_model, product: "Model_10", version: "AA10")

    model_1.add_category(cat_1)
    model_2.add_category(cat_2)

    item_model_2_1 = FactoryBot.create(:item,
      inventory_code: "#{pool_1.shortname}100",
      owner_id: pool_1.id,
      inventory_pool_id: pool_1.id,
      leihs_model: model_2,
      room: room,
      shelf: "S21",
      last_check: Date.today,
      is_borrowable: true,
      retired: nil)

    item_model_2_2 = FactoryBot.create(:item,
      inventory_code: "#{pool_1.shortname}110",
      owner_id: pool_1.id,
      inventory_pool_id: pool_1.id,
      leihs_model: model_2,
      last_check: Date.today,
      is_borrowable: false,
      room: room,
      shelf: "S22",
      retired: Date.yesterday,
      retired_reason: Faker::Lorem.sentence)

    item_model_2_3 = FactoryBot.create(:item,
      inventory_code: "#{pool_1.shortname}120",
      owner_id: pool_2.id,
      inventory_pool_id: pool_1.id,
      leihs_model: model_2,
      room: room,
      shelf: "S23",
      last_check: Date.today,
      is_borrowable: true,
      retired: nil)

    item_model_2_4 = FactoryBot.create(:item,
      inventory_code: "#{pool_1.shortname}130",
      owner_id: pool_2.id,
      inventory_pool_id: pool_1.id,
      leihs_model: model_2,
      last_check: Date.today,
      room: room,
      shelf: "S24",
      is_borrowable: true,
      is_incomplete: true,
      is_broken: true,
      retired: nil)

    item_model_3_1 = FactoryBot.create(:item,
      inventory_code: "#{pool_1.shortname}101",
      owner_id: pool_1.id,
      inventory_pool_id: pool_1.id,
      leihs_model: model_3,
      is_borrowable: true,
      room: room,
      shelf: "S31",
      retired: Date.yesterday,
      retired_reason: Faker::Lorem.sentence,
      note: search_term)

    item_model_4_1 = FactoryBot.create(:item,
      inventory_code: "#{pool_1.shortname}102",
      owner_id: pool_1.id,
      inventory_pool_id: pool_1.id,
      leihs_model: model_4,
      is_borrowable: false,
      is_incomplete: true,
      room: room,
      shelf: "S41",
      retired: Date.yesterday,
      retired_reason: Faker::Lorem.sentence)

    item_model_5_1 = FactoryBot.create(:item,
      inventory_code: "#{pool_1.shortname}103",
      owner_id: pool_1.id,
      inventory_pool_id: pool_2.id,
      shelf: "S51",
      room: room,
      retired: Date.yesterday,
      retired_reason: Faker::Lorem.sentence,
      leihs_model: model_5)

    package = FactoryBot.create(:item,
      inventory_code: "#{pool_1.shortname}106",
      owner_id: pool_1.id,
      inventory_pool_id: pool_1.id,
      leihs_model: model_8,
      is_borrowable: true,
      room: room,
      retired: nil)

    item_model_9_1 = FactoryBot.create(:item,
      inventory_code: "#{pool_1.shortname}107",
      parent_id: package.id,
      leihs_model: model_9,
      owner_id: pool_1.id,
      shelf: "S91",
      room: room,
      inventory_pool_id: pool_1.id)

    item_model_10_1 = FactoryBot.create(:item,
      inventory_code: "#{pool_1.shortname}108",
      parent_id: package.id,
      leihs_model: model_10,
      owner_id: pool_1.id,
      shelf: "S101",
      inventory_pool_id: pool_1.id,
      room: room)

    user_2 = FactoryBot.create(:user)
    contract = Contract.create_with_disabled_triggers(
      "a9bf950b-e91e-4a23-85e1-5fd8163d234a",
      user_2.id,
      pool_1.id
    )

    reservation = FactoryBot.create(:reservation,
      status: :signed,
      leihs_model: model_8,
      item_id: package.id,
      contract_id: contract.id,
      user_id: user_2.id,
      inventory_pool_id: pool_1.id)

    item_model_6_1 = FactoryBot.create(:item,
      inventory_code: "#{pool_3.shortname}104",
      owner_id: pool_3.id,
      inventory_pool_id: pool_1.id,
      is_broken: true,
      shelf: "S61",
      room: room,
      leihs_model: model_6)

    FactoryBot.create(:item,
      inventory_code: "#{pool_4.shortname}105",
      owner_id: pool_4.id,
      inventory_pool_id: pool_4.id,
      leihs_model: model_7)

    login(user)

    visit "/inventory"
    expect(page).to have_content "Inventory"
    find("nav button", text: "Inventory").click
    click_on pool_1.name

    expect(page).to have_css('nav[aria-label="breadcrumb"]', text: pool_1.name)
    expect(page).to have_css('nav[aria-label="breadcrumb"]', text: "Inventory List")

    uri = URI.parse(current_url)
    query_params = CGI.parse(uri.query)
    expect(query_params).to eq({"with_items" => ["true"], "retired" => ["false"], "page" => ["1"], "size" => ["50"]})
    expect(all("table tbody tr").count).to eq 5

    verify_row_details(
      model_10,
      "0 | 1",
      [
        {
          inventory_code: item_model_10_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_10_1.shelf,
          statuses: ["Available"]
        }
      ]
    )

    verify_row_details(
      model_2,
      "3 | 3",
      [
        {
          inventory_code: item_model_2_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_2_1.shelf,
          statuses: ["Available"]
        },
        {
          inventory_code: item_model_2_3.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_2_3.shelf,
          statuses: ["Available"]
        },
        {
          inventory_code: item_model_2_4.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_2_4.shelf,
          statuses: ["Broken", "Available", "Incomplete"]
        }
      ]
    )

    verify_row_details(
      model_6,
      "1 | 1",
      [
        {
          inventory_code: item_model_6_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_6_1.shelf,
          statuses: ["Broken", "Available"]
        }
      ]
    )

    verify_row_details(
      model_8,
      "0 | 1",
      [
        {
          inventory_code: package.inventory_code,
          reservation_user_name: "#{user_2.firstname} #{user_2.lastname}",
          reservation_end_date: reservation.end_date.strftime("%d.%m.%Y"),
          statuses: ["Rented"],
          package_items: [
            {
              model_name: model_9.product,
              inventory_code: item_model_9_1.inventory_code,
              statuses: ["Available"]
            },
            {
              model_name: model_10.product,
              inventory_code: item_model_10_1.inventory_code,
              statuses: ["Available"]
            }
          ]
        }
      ],
      is_package: true
    )

    verify_row_details(
      model_9,
      "0 | 1",
      [
        {
          inventory_code: item_model_9_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_9_1.shelf,
          statuses: ["Available"]
        }
      ]
    )

    # pool 1
    # with_items=true
    # retired=true
    visit "/inventory/#{pool_1.id}/list"
    select_value("with_items", "with_items")
    select_value("retired", "retired")

    expect(all("table tbody tr").count).to eq 4

    verify_row_details(
      model_2,
      "3 | 3",
      [
        {
          inventory_code: item_model_2_2.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_2_2.shelf,
          statuses: ["Not borrowable"]
        }
      ]
    )

    verify_row_details(
      model_3,
      "1 | 1",
      [
        {
          inventory_code: item_model_3_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_3_1.shelf,
          statuses: ["Not borrowable", "Available"]
        }
      ]
    )

    verify_row_details(
      model_4,
      "0 | 0",
      [
        {
          inventory_code: item_model_4_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_4_1.shelf,
          statuses: ["Not borrowable", "Incomplete"]
        }
      ]
    )

    verify_row_details(
      model_5,
      "0 | 0",
      [
        {
          inventory_code: item_model_5_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_5_1.shelf,
          statuses: ["Not borrowable", "Available"]
        }
      ]
    )

    # pool 1
    # with_items=true
    # retired=true
    # borrowable=false
    visit "/inventory/#{pool_1.id}/list"

    select_value("with_items", "with_items")
    select_value("retired", "retired")
    select_value("borrowable", "not_borrowable")

    expect(all("table tbody tr").count).to eq 2

    verify_row_details(
      model_2,
      "3 | 3",
      [
        {
          inventory_code: item_model_2_2.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_2_2.shelf,
          statuses: ["Not borrowable"]
        }
      ]
    )

    verify_row_details(
      model_4,
      "0 | 0",
      [
        {
          inventory_code: item_model_4_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_4_1.shelf,
          statuses: ["Not borrowable", "Incomplete"]
        }
      ]
    )

    # pool 1
    # with_items=true
    visit "/inventory/#{pool_1.id}/list"
    select_value("with_items", "with_items")

    expect(all("table tbody tr").count).to eq 8

    verify_row_details(
      model_10,
      "0 | 1",
      [
        {
          inventory_code: item_model_10_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_10_1.shelf,
          statuses: ["Available"]
        }
      ]
    )

    expect(all("table tbody tr")[1]).to have_content(model_2.version)
    verify_row_details(
      model_2,
      "3 | 3",
      [
        {
          inventory_code: item_model_2_2.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_2_2.shelf,
          statuses: ["Not borrowable"]
        },
        {
          inventory_code: item_model_2_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_2_1.shelf,
          statuses: ["Available"]
        },
        {
          inventory_code: item_model_2_3.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_2_3.shelf,
          statuses: ["Available"]
        },
        {
          inventory_code: item_model_2_4.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_2_4.shelf,
          statuses: ["Broken", "Available", "Incomplete"]
        }
      ]
    )

    verify_row_details(
      model_3,
      "1 | 1",
      [
        {
          inventory_code: item_model_3_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_3_1.shelf,
          statuses: ["Not borrowable", "Available"]
        }
      ]
    )

    verify_row_details(
      model_4,
      "0 | 0",
      [
        {
          inventory_code: item_model_4_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_4_1.shelf,
          statuses: ["Not borrowable", "Incomplete"]
        }
      ]
    )

    verify_row_details(
      model_5,
      "0 | 0",
      [
        {
          inventory_code: item_model_5_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_5_1.shelf,
          statuses: ["Not borrowable", "Available"]
        }
      ]
    )

    verify_row_details(
      model_6,
      "1 | 1",
      [
        {
          inventory_code: item_model_6_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_6_1.shelf,
          statuses: ["Broken", "Available"]
        }
      ]
    )

    verify_row_details(
      model_8,
      "0 | 1",
      [
        {
          inventory_code: package.inventory_code,
          reservation_user_name: "#{user_2.firstname} #{user_2.lastname}",
          reservation_end_date: reservation.end_date.strftime("%d.%m.%Y"),
          statuses: ["Rented"],
          package_items: [
            {
              model_name: model_9.product,
              inventory_code: item_model_9_1.inventory_code,
              statuses: ["Available"]
            },
            {
              model_name: model_10.product,
              inventory_code: item_model_10_1.inventory_code,
              statuses: ["Available"]
            }
          ]
        }
      ],
      is_package: true
    )

    verify_row_details(
      model_9,
      "0 | 1",
      [
        {
          inventory_code: item_model_9_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_9_1.shelf,
          statuses: ["Available"]
        }
      ]
    )

    # pool 1
    # with_items=false
    visit "/inventory/#{pool_1.id}/list"

    click_on "Status"
    click_on "Broken"
    click_on "Yes"

    click_on "Status"
    click_on "In stock"
    click_on "No"

    click_on "Status"
    click_on "Owned"
    click_on "Yes"

    click_on "Status"
    click_on "Incomplete"
    click_on "Yes"

    expect(page).to have_button("Status", text: "4")

    select_value("with_items", "without_items")

    expect(page).to have_button("Status", disabled: true)

    expect(all("table tbody tr").count).to eq 2

    verify_row_details(
      model_1,
      "0 | 0"
    )

    verify_row_details(
      model_7,
      "0 | 0"
    )

    # pool 1
    # page=1
    # size=20
    visit "/inventory/#{pool_1.id}/list"
    first(:link_or_button, "50").click
    click_on "20"

    expect(all("table tbody tr").count).to eq 11

    verify_row_details(
      model_1,
      "0 | 0"
    )

    verify_row_details(
      model_10,
      "0 | 1",
      [
        {
          inventory_code: item_model_10_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_10_1.shelf,
          statuses: ["Available"]
        }
      ]
    )

    verify_row_details(
      model_2,
      "3 | 3",
      [

        {
          inventory_code: item_model_2_2.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_2_2.shelf,
          statuses: ["Not borrowable"]
        },
        {
          inventory_code: item_model_2_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_2_1.shelf,
          statuses: ["Available"]
        },
        {
          inventory_code: item_model_2_3.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_2_3.shelf,
          statuses: ["Available"]
        },
        {
          inventory_code: item_model_2_4.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_2_4.shelf,
          statuses: ["Broken", "Available", "Incomplete"]
        }
      ]
    )

    verify_row_details(
      model_3,
      "1 | 1",
      [
        {
          inventory_code: item_model_3_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_3_1.shelf,
          statuses: ["Not borrowable", "Available"]
        }
      ]
    )

    verify_row_details(
      model_4,
      "0 | 0",
      [
        {
          inventory_code: item_model_4_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_4_1.shelf,
          statuses: ["Not borrowable", "Incomplete"]
        }
      ]
    )

    verify_row_details(
      model_5,
      "0 | 0",
      [
        {
          inventory_code: item_model_5_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_5_1.shelf,
          statuses: ["Not borrowable", "Available"]
        }
      ]
    )

    verify_row_details(
      model_6,
      "1 | 1",
      [
        {
          inventory_code: item_model_6_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_6_1.shelf,
          statuses: ["Broken", "Available"]
        }
      ]
    )

    verify_row_details(
      model_7,
      "0 | 0"
    )

    verify_row_details(
      model_8,
      "0 | 1",
      [
        {
          inventory_code: package.inventory_code,
          reservation_user_name: "#{user_2.firstname} #{user_2.lastname}",
          reservation_end_date: reservation.end_date.strftime("%d.%m.%Y"),
          statuses: ["Rented"],
          package_items: [
            {
              model_name: model_9.product,
              inventory_code: item_model_9_1.inventory_code,
              statuses: ["Available"]
            },
            {
              model_name: model_10.product,
              inventory_code: item_model_10_1.inventory_code,
              statuses: ["Available"]
            }
          ]
        }
      ],
      is_package: true
    )

    verify_row_details(
      model_9,
      "0 | 1",
      [
        {
          inventory_code: item_model_9_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_9_1.shelf,
          statuses: ["Available"]
        }
      ]
    )

    verify_row_details(
      option_1,
      "#{option_1.price.to_i} GBP",
      [],
      is_option: true
    )

    visit "/inventory/#{pool_1.id}/list"
    find("input[name='search']").set(search_term)

    wait_until { all("table tbody tr").count == 4 }

    verify_row_details(
      model_2,
      "3 | 3",
      [

        {
          inventory_code: item_model_2_2.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_2_2.shelf,
          statuses: ["Not borrowable"]
        },
        {
          inventory_code: item_model_2_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_2_1.shelf,
          statuses: ["Available"]
        },
        {
          inventory_code: item_model_2_3.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_2_3.shelf,
          statuses: ["Available"]
        },
        {
          inventory_code: item_model_2_4.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_2_4.shelf,
          statuses: ["Broken", "Available", "Incomplete"]
        }
      ]
    )

    verify_row_details(
      model_3,
      "1 | 1",
      [
        {
          inventory_code: item_model_3_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_3_1.shelf,
          statuses: ["Not borrowable", "Available"]
        }
      ]
    )

    verify_row_details(
      model_8,
      "0 | 1",
      [
        {
          inventory_code: package.inventory_code,
          reservation_user_name: "#{user_2.firstname} #{user_2.lastname}",
          reservation_end_date: reservation.end_date.strftime("%d.%m.%Y"),
          statuses: ["Rented"],
          package_items: [
            {
              model_name: model_9.product,
              inventory_code: item_model_9_1.inventory_code,
              statuses: ["Available"]
            },
            {
              model_name: model_10.product,
              inventory_code: item_model_10_1.inventory_code,
              statuses: ["Available"]
            }
          ]
        }
      ],
      is_package: true
    )

    verify_row_details(
      model_9,
      "0 | 1",
      [
        {
          inventory_code: item_model_9_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_9_1.shelf,
          statuses: ["Available"]
        }
      ]
    )

    # pool 1
    # with_items=true
    # inventory_pool_id=pool_2.id
    visit "/inventory/#{pool_1.id}/list"
    select_value("with_items", "with_items")
    click_on "Inventory pool"
    find("[data-value='#{pool_2.id}']").click

    expect(all("table tbody tr").count).to eq 2

    verify_row_details(
      model_2,
      "3 | 3",
      [
        {
          inventory_code: item_model_2_3.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_2_3.shelf,
          statuses: ["Available"]
        },
        {
          inventory_code: item_model_2_4.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_2_4.shelf,
          statuses: ["Broken", "Available", "Incomplete"]
        }
      ]
    )

    verify_row_details(
      model_5,
      "0 | 0",
      [
        {
          inventory_code: item_model_5_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_5_1.shelf,
          statuses: ["Not borrowable", "Available"]
        }
      ]
    )

    # pool 1
    # with_items=true
    # owned=true
    # borrowable=true
    visit "/inventory/#{pool_1.id}/list"
    select_value("with_items", "with_items")
    select_value("borrowable", "borrowable")
    click_on "Status"
    click_on "Owned"
    click_on "Yes"

    expect(page).to have_button("Status", text: "1")

    expect(all("table tbody tr").count).to eq 6

    verify_row_details(
      model_10,
      "0 | 1",
      [
        {
          inventory_code: item_model_10_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_10_1.shelf,
          statuses: ["Available"]
        }
      ]
    )

    verify_row_details(
      model_2,
      "3 | 3",
      [
        {
          inventory_code: item_model_2_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_2_1.shelf,
          statuses: ["Available"]
        }
      ]
    )

    verify_row_details(
      model_3,
      "1 | 1",
      [
        {
          inventory_code: item_model_3_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_3_1.shelf,
          statuses: ["Not borrowable", "Available"]
        }
      ]
    )

    verify_row_details(
      model_5,
      "0 | 0",
      [
        {
          inventory_code: item_model_5_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_5_1.shelf,
          statuses: ["Not borrowable", "Available"]
        }
      ]
    )

    verify_row_details(
      model_8,
      "0 | 1",
      [
        {
          inventory_code: package.inventory_code,
          reservation_user_name: "#{user_2.firstname} #{user_2.lastname}",
          reservation_end_date: reservation.end_date.strftime("%d.%m.%Y"),
          statuses: ["Rented"],
          package_items: [
            {
              model_name: model_9.product,
              inventory_code: item_model_9_1.inventory_code,
              statuses: ["Available"]
            },
            {
              model_name: model_10.product,
              inventory_code: item_model_10_1.inventory_code,
              statuses: ["Available"]
            }
          ]
        }
      ],
      is_package: true
    )

    verify_row_details(
      model_9,
      "0 | 1",
      [
        {
          inventory_code: item_model_9_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_9_1.shelf,
          statuses: ["Available"]
        }
      ]
    )

    # pool 1
    # with_items=true
    # ind_stock=false
    visit "/inventory/#{pool_1.id}/list"
    select_value("with_items", "with_items")
    click_on "Status"
    click_on "In stock"
    click_on "No"
    expect(page).to have_button("Status", text: "1")

    expect(all("table tbody tr").count).to eq 1

    verify_row_details(
      model_8,
      "0 | 1",
      [
        {
          inventory_code: package.inventory_code,
          reservation_user_name: "#{user_2.firstname} #{user_2.lastname}",
          reservation_end_date: reservation.end_date.strftime("%d.%m.%Y"),
          statuses: ["Rented"],
          package_items: [
            {
              model_name: model_9.product,
              inventory_code: item_model_9_1.inventory_code,
              statuses: ["Available"]
            },
            {
              model_name: model_10.product,
              inventory_code: item_model_10_1.inventory_code,
              statuses: ["Available"]
            }
          ]
        }
      ],
      is_package: true
    )

    # pool 1
    # with_items=true
    # incomplete=true
    visit "/inventory/#{pool_1.id}/list"
    select_value("with_items", "with_items")
    click_on "Status"
    click_on "Incomplete"
    click_on "Yes"
    expect(page).to have_button("Status", text: "1")

    expect(all("table tbody tr").count).to eq 2

    verify_row_details(
      model_2,
      "3 | 3",
      [
        {
          inventory_code: item_model_2_4.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_2_4.shelf,
          statuses: ["Available", "Broken", "Incomplete"]
        }
      ]
    )

    verify_row_details(
      model_4,
      "0 | 0",
      [
        {
          inventory_code: item_model_4_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_4_1.shelf,
          statuses: ["Incomplete", "Not borrowable"]
        }
      ]
    )

    # pool 1
    # with_items=true
    # broken=true
    visit "/inventory/#{pool_1.id}/list"

    select_value("with_items", "with_items")
    click_on "Status"
    click_on "Broken"
    click_on "Yes"
    expect(page).to have_button("Status", text: "1")

    expect(all("table tbody tr").count).to eq 2

    verify_row_details(
      model_2,
      "3 | 3",
      [
        {
          inventory_code: item_model_2_4.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_2_4.shelf,
          statuses: ["Available", "Broken", "Incomplete"]
        }
      ]
    )

    verify_row_details(
      model_6,
      "1 | 1",
      [
        {
          inventory_code: item_model_6_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_6_1.shelf,
          statuses: ["Available", "Broken"]
        }
      ]
    )

    # pool 1
    # with_items=true
    # before_last_check=today
    visit "/inventory/#{pool_1.id}/list"
    select_value("with_items", "with_items")
    click_on "before-last-check-filter-button"
    within("[data-test-id='before-last-check-calendar']") do
      all(:button, Date.today.day.to_s).last.click
    end

    expect(all("table tbody tr").count).to eq 1

    verify_row_details(
      model_2,
      "3 | 3",
      [
        {
          inventory_code: item_model_2_2.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_2_2.shelf,
          statuses: ["Not borrowable"]
        },
        {
          inventory_code: item_model_2_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_2_1.shelf,
          statuses: ["Available"]
        },
        {
          inventory_code: item_model_2_3.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_2_3.shelf,
          statuses: ["Available"]
        },
        {
          inventory_code: item_model_2_4.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_2_4.shelf,
          statuses: ["Available", "Broken", "Incomplete"]
        }
      ]
    )

    # pool 1
    # category_id=cat_1.id
    visit "/inventory/#{pool_1.id}/list"

    click_on "category-filter-button"
    click_on cat_1.id
    expect(page).to have_content(cat_1.name.to_s)

    expect(all("table tbody tr").count).to eq 2

    verify_row_details(
      model_2,
      "3 | 3",
      [
        {
          inventory_code: item_model_2_2.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_2_2.shelf,
          statuses: ["Not borrowable"]
        },
        {
          inventory_code: item_model_2_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_2_1.shelf,
          statuses: ["Available"]
        },
        {
          inventory_code: item_model_2_3.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_2_3.shelf,
          statuses: ["Available"]
        },
        {
          inventory_code: item_model_2_4.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_2_4.shelf,
          statuses: ["Available", "Broken", "Incomplete"]
        }
      ]
    )

    # pool 1
    # type=package
    visit "/inventory/#{pool_1.id}/list"
    click_on "Inventory type"
    click_on "Package"

    within(:button, "Inventory type") do
      expect(page).to have_text("P")
    end

    expect(all("table tbody tr").count).to eq 1
    verify_row_details(
      model_8,
      "0 | 1",
      [
        {
          inventory_code: package.inventory_code,
          reservation_user_name: "#{user_2.firstname} #{user_2.lastname}",
          reservation_end_date: reservation.end_date.strftime("%d.%m.%Y"),
          statuses: ["Rented"],
          package_items: [
            {
              model_name: model_9.product,
              inventory_code: item_model_9_1.inventory_code,
              statuses: ["Available"]
            },
            {
              model_name: model_10.product,
              inventory_code: item_model_10_1.inventory_code,
              statuses: ["Available"]
            }
          ]
        }
      ],
      is_package: true
    )

    # type=model
    visit "/inventory/#{pool_1.id}/list"
    click_on "Inventory type"
    within("[data-test-id='type-filter-dropdown']") do
      click_on "Model"
    end

    expect(all("table tbody tr").count).to eq 9

    verify_row_details(
      model_1,
      "0 | 0"
    )

    verify_row_details(
      model_10,
      "0 | 1",
      [
        {
          inventory_code: item_model_10_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_10_1.shelf,
          statuses: ["Available"]
        }
      ]
    )

    verify_row_details(
      model_2,
      "3 | 3",
      [

        {
          inventory_code: item_model_2_2.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_2_2.shelf,
          statuses: ["Not borrowable"]
        },
        {
          inventory_code: item_model_2_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_2_1.shelf,
          statuses: ["Available"]
        },
        {
          inventory_code: item_model_2_3.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_2_3.shelf,
          statuses: ["Available"]
        },
        {
          inventory_code: item_model_2_4.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_2_4.shelf,
          statuses: ["Broken", "Available", "Incomplete"]
        }
      ]
    )

    verify_row_details(
      model_3,
      "1 | 1",
      [
        {
          inventory_code: item_model_3_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_3_1.shelf,
          statuses: ["Not borrowable", "Available"]
        }
      ]
    )

    verify_row_details(
      model_4,
      "0 | 0",
      [
        {
          inventory_code: item_model_4_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_4_1.shelf,
          statuses: ["Not borrowable", "Incomplete"]
        }
      ]
    )

    verify_row_details(
      model_5,
      "0 | 0",
      [
        {
          inventory_code: item_model_5_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_5_1.shelf,
          statuses: ["Not borrowable", "Available"]
        }
      ]
    )

    verify_row_details(
      model_6,
      "1 | 1",
      [
        {
          inventory_code: item_model_6_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_6_1.shelf,
          statuses: ["Broken", "Available"]
        }
      ]
    )

    verify_row_details(
      model_7,
      "0 | 0"
    )

    verify_row_details(
      model_9,
      "0 | 1",
      [
        {
          inventory_code: item_model_9_1.inventory_code,
          building_name: building.name,
          building_code: building.code,
          shelf: item_model_9_1.shelf,
          statuses: ["Available"]
        }
      ]
    )

    # type=option
    visit "/inventory/#{pool_1.id}/list"

    # set some filters to check if they are ignored
    # before selecting option
    click_on "category-filter-button"
    click_on cat_1.id

    click_on "Status"
    click_on "Broken"
    click_on "Yes"

    click_on "Status"
    click_on "In stock"
    click_on "No"

    click_on "before-last-check-filter-button"
    within("[data-test-id='before-last-check-calendar']") do
      all(:button, Date.today.day.to_s).last.click
    end

    select_value("with_items", "with_items")
    select_value("borrowable", "borrowable")
    select_value("retired", "retired")

    click_on "Inventory pool"
    find("[data-value='#{pool_2.id}']").click

    expect(page).to have_content(cat_1.name.to_s)
    expect(page).to have_content("Audio")
    expect(page).to have_content(pool_2.name)
    expect(page).to have_content(Date.today.day.to_s)

    expect(page).to have_button("Status", text: "2")

    click_on "Inventory type"
    click_on "Option"

    within(:button, "Inventory type") do
      expect(page).to have_text("O")
    end

    expect(page).to have_button("Status", disabled: true)
    expect(page).to have_button(cat_1.name, disabled: true)
    expect(page).to have_button(pool_2.name, disabled: true)
    expect(page).to have_button(Date.today.day.to_s, disabled: true)
    expect(page).to have_button("retired", disabled: true)
    expect(page).to have_button("only models with items", disabled: true)
    expect(page).to have_button("borrowable", disabled: true)

    expect(all("table tbody tr").count).to eq 1

    verify_row_details(
      option_1,
      "#{option_1.price.to_i} GBP",
      [],
      is_option: true
    )

    # pool 5
    # with_items=true
    visit "/inventory/#{pool_5.id}/list"
    select_value("with_items", "with_items")

    expect(all("table tbody tr").count).to eq 0
  end
end

def verify_row_details(model, availabilty, items = [], is_package: false, is_option: false)
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

    expect(following_rows.size).to eq(items.size)

    items.each_with_index do |details, index|
      expect(following_rows[index]).to have_content(details[:inventory_code])
      if details[:reservation_user_name]
        expect(following_rows[index]).to have_content(details[:reservation_user_name])
        expect(following_rows[index]).to have_content(details[:reservation_end_date])
        expect(following_rows[index].find('[data-test-id="items"]').text).to eq(details[:package_items].size.to_s)
      else
        expect(following_rows[index]).to have_content(details[:building_name])
        expect(following_rows[index]).to have_content(details[:building_code])
        expect(following_rows[index]).to have_content(details[:shelf])
      end

      # Extract the status portion and validate it
      status_texts = following_rows[index].all('[data-test-id="item-status"] span').map(&:text)
      expect(details[:statuses]).to include(*status_texts)

      if details[:package_items]

        package_expand = following_rows[index].find('[data-test-id="expand-button"]')
        package_expand.click

        details[:package_items].each_with_index do |pkg_item, pkg_index|
          package_rows = following_rows[index].all(:xpath, "following-sibling::tr[@data-row='item']", wait: 30)
          expect(package_rows[pkg_index]).to have_content(pkg_item[:model_name])
          expect(package_rows[pkg_index]).to have_content(pkg_item[:inventory_code])
          expect(package_rows[pkg_index]).to have_content("is part of a package")
        end

        package_expand.click
      end
    end
    click_on "expand-button"
    return "rows correct"
  end
end
