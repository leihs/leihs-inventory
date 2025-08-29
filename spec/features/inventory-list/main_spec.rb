require "spec_helper"
require_relative "../shared/common"

feature "Inventory Page", type: :feature do
  scenario "filters" do
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

    model_1 = FactoryBot.create(:leihs_model, product: "Model", version: "AA1")
    model_2 = FactoryBot.create(:leihs_model, product: "Model", version: "AA2 #{search_term}")
    model_3 = FactoryBot.create(:leihs_model, product: "Model", version: "AA3")
    model_4 = FactoryBot.create(:leihs_model, product: "Model", version: "AA4")
    model_5 = FactoryBot.create(:leihs_model, product: "Model", version: "AA5")
    model_6 = FactoryBot.create(:leihs_model, product: "Model", version: "AA6")
    model_7 = FactoryBot.create(:leihs_model, product: "Model", version: "AA7")
    model_8 = FactoryBot.create(:leihs_model, product: "Model", version: "AA8", is_package: true)
    model_9 = FactoryBot.create(:leihs_model, product: "Model", version: "AA9 #{search_term}")
    model_10 = FactoryBot.create(:leihs_model, product: "Model", version: "AA10")

    model_1.add_category(cat_1)
    model_2.add_category(cat_2)

    FactoryBot.create(:item,
      inventory_code: "#{pool_1.shortname}100",
      owner_id: pool_1.id,
      inventory_pool_id: pool_1.id,
      leihs_model: model_2,
      last_check: Date.today,
      is_borrowable: true,
      retired: nil)

    FactoryBot.create(:item,
      inventory_code: "#{pool_1.shortname}101",
      owner_id: pool_1.id,
      inventory_pool_id: pool_1.id,
      leihs_model: model_3,
      is_borrowable: true,
      retired: Date.yesterday,
      retired_reason: Faker::Lorem.sentence,
      note: search_term)

    FactoryBot.create(:item,
      inventory_code: "#{pool_1.shortname}102",
      owner_id: pool_1.id,
      inventory_pool_id: pool_1.id,
      leihs_model: model_4,
      is_borrowable: false,
      is_incomplete: true,
      retired: Date.yesterday,
      retired_reason: Faker::Lorem.sentence)

    FactoryBot.create(:item,
      inventory_code: "#{pool_1.shortname}103",
      owner_id: pool_1.id,
      inventory_pool_id: pool_2.id,
      retired: Date.yesterday,
      retired_reason: Faker::Lorem.sentence,
      leihs_model: model_5)

    package = FactoryBot.create(:item,
      inventory_code: "#{pool_1.shortname}106",
      owner_id: pool_1.id,
      inventory_pool_id: pool_1.id,
      leihs_model: model_8,
      is_borrowable: true,
      retired: nil)

    FactoryBot.create(:item,
      inventory_code: "#{pool_1.shortname}107",
      parent_id: package.id,
      leihs_model: model_9,
      owner_id: pool_1.id,
      inventory_pool_id: pool_1.id)

    FactoryBot.create(:item,
      inventory_code: "#{pool_1.shortname}108",
      parent_id: package.id,
      leihs_model: model_10,
      owner_id: pool_1.id,
      inventory_pool_id: pool_1.id)

    user_2 = FactoryBot.create(:user)
    contract = Contract.create_with_disabled_triggers(
      "a9bf950b-e91e-4a23-85e1-5fd8163d234a",
      user_2.id,
      pool_1.id
    )

    FactoryBot.create(:reservation,
      status: :signed,
      leihs_model: model_8,
      item_id: package.id,
      contract_id: contract.id,
      user_id: user_2.id,
      inventory_pool_id: pool_1.id)

    FactoryBot.create(:item,
      inventory_code: "#{pool_3.shortname}104",
      owner_id: pool_3.id,
      inventory_pool_id: pool_1.id,
      is_broken: true,
      leihs_model: model_6)

    FactoryBot.create(:item,
      inventory_code: "#{pool_4.shortname}105",
      owner_id: pool_4.id,
      inventory_pool_id: pool_4.id,
      leihs_model: model_7)

    login(user)

    visit "/inventory"
    find("nav button", text: "Inventory").click
    click_on pool_1.name
    expect(page).to have_content("Inventory List - #{pool_1.name}")
    uri = URI.parse(current_url)
    query_params = CGI.parse(uri.query)
    expect(query_params).to eq({"with_items" => ["true"], "retired" => ["false"], "page" => ["1"], "size" => ["50"]})
    expect(all("table tbody tr").count).to eq 5
    expect(all("table tbody tr")[0]).to have_content(model_10.version)
    expect(all("table tbody tr")[1]).to have_content(model_2.version)
    expect(all("table tbody tr")[2]).to have_content(model_6.version)
    expect(all("table tbody tr")[3]).to have_content(model_8.version)
    expect(all("table tbody tr")[4]).to have_content(model_9.version)

    # pool 1
    # with_items=true
    # retired=true
    visit "/inventory/#{pool_1.id}/list"
    select_value("with_items", "with_items")
    select_value("retired", "retired")

    expect(all("table tbody tr").count).to eq 3
    expect(all("table tbody tr")[0]).to have_content(model_3.version)
    expect(all("table tbody tr")[1]).to have_content(model_4.version)
    expect(all("table tbody tr")[2]).to have_content(model_5.version)

    # pool 1
    # with_items=true
    # retired=true
    # borrowable=false
    visit "/inventory/#{pool_1.id}/list"

    select_value("with_items", "with_items")
    select_value("retired", "retired")
    select_value("borrowable", "not_borrowable")

    expect(all("table tbody tr").count).to eq 1
    expect(first("table tbody tr")).to have_content(model_4.version)

    # pool 1
    # with_items=true
    visit "/inventory/#{pool_1.id}/list"
    select_value("with_items", "with_items")

    expect(all("table tbody tr").count).to eq 8
    expect(all("table tbody tr")[0]).to have_content(model_10.version)
    expect(all("table tbody tr")[1]).to have_content(model_2.version)
    expect(all("table tbody tr")[2]).to have_content(model_3.version)
    expect(all("table tbody tr")[3]).to have_content(model_4.version)
    expect(all("table tbody tr")[4]).to have_content(model_5.version)
    expect(all("table tbody tr")[5]).to have_content(model_6.version)
    expect(all("table tbody tr")[6]).to have_content(model_8.version)
    expect(all("table tbody tr")[7]).to have_content(model_9.version)

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

    select_value("with_items", "without_items")

    expect(page).to have_button("Status", disabled: true)

    expect(page).not_to have_content("Broken - Yes")
    expect(page).not_to have_content("In Stock - No")
    expect(page).not_to have_content("Owned - Yes")
    expect(page).not_to have_content("Incomplete - Yes")

    expect(all("table tbody tr").count).to eq 2
    expect(all("table tbody tr")[0]).to have_content(model_1.version)
    expect(all("table tbody tr")[1]).to have_content(model_7.version)

    # pool 1
    # page=1
    # size=20
    visit "/inventory/#{pool_1.id}/list"
    first(:link_or_button, "50").click
    click_on "20"

    expect(all("table tbody tr").count).to eq 11
    expect(all("table tbody tr")[0]).to have_content(model_1.version)
    expect(all("table tbody tr")[1]).to have_content(model_10.version)
    expect(all("table tbody tr")[2]).to have_content(model_2.version)
    expect(all("table tbody tr")[3]).to have_content(model_3.version)
    expect(all("table tbody tr")[4]).to have_content(model_4.version)
    expect(all("table tbody tr")[5]).to have_content(model_5.version)
    expect(all("table tbody tr")[6]).to have_content(model_6.version)
    expect(all("table tbody tr")[7]).to have_content(model_7.version)
    expect(all("table tbody tr")[8]).to have_content(model_8.version)
    expect(all("table tbody tr")[9]).to have_content(model_9.version)
    expect(all("table tbody tr")[10]).to have_content(option_1.version)

    visit "/inventory/#{pool_1.id}/list"
    find("input[name='search']").set(search_term)

    wait_until { all("table tbody tr").count == 4 }
    expect(all("table tbody tr")[0]).to have_content(model_2.version)
    expect(all("table tbody tr")[1]).to have_content(model_3.version)
    expect(all("table tbody tr")[2]).to have_content(model_8.version)
    expect(all("table tbody tr")[3]).to have_content(model_9.version)

    # pool 1
    # with_items=true
    # inventory_pool_id=pool_2.id
    visit "/inventory/#{pool_1.id}/list"
    select_value("with_items", "with_items")
    click_on "Inventory pool"
    find("[data-value='#{pool_2.id}']").click

    expect(all("table tbody tr").count).to eq 1
    expect(all("table tbody tr")[0]).to have_content(model_5.version)

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
    expect(page).to have_content("Owned - Yes")

    expect(all("table tbody tr").count).to eq 6
    expect(all("table tbody tr")[0]).to have_content(model_10.version)
    expect(all("table tbody tr")[1]).to have_content(model_2.version)
    expect(all("table tbody tr")[2]).to have_content(model_3.version)
    expect(all("table tbody tr")[3]).to have_content(model_5.version)
    expect(all("table tbody tr")[4]).to have_content(model_8.version)
    expect(all("table tbody tr")[5]).to have_content(model_9.version)

    # pool 1
    # with_items=true
    # ind_stock=false
    visit "/inventory/#{pool_1.id}/list"
    select_value("with_items", "with_items")
    click_on "Status"
    click_on "In stock"
    click_on "No"
    expect(page).to have_content("In stock - No")

    expect(all("table tbody tr").count).to eq 1
    expect(all("table tbody tr")[0]).to have_content(model_8.version)

    # pool 1
    # with_items=true
    # incomplete=true
    visit "/inventory/#{pool_1.id}/list"
    select_value("with_items", "with_items")
    click_on "Status"
    click_on "Incomplete"
    click_on "Yes"
    expect(page).to have_content("Incomplete - Yes")

    expect(all("table tbody tr").count).to eq 1
    expect(all("table tbody tr")[0]).to have_content(model_4.version)

    # pool 1
    # with_items=true
    # broken=true
    visit "/inventory/#{pool_1.id}/list"

    select_value("with_items", "with_items")
    click_on "Status"
    click_on "Broken"
    click_on "Yes"
    expect(page).to have_content("Broken - Yes")

    expect(all("table tbody tr").count).to eq 1
    expect(all("table tbody tr")[0]).to have_content(model_6.version)

    # pool 1
    # with_items=true
    # before_last_check=today
    visit "/inventory/#{pool_1.id}/list"
    select_value("with_items", "with_items")
    click_on "Inventory before"
    within ".rdp" do
      all(:button, Date.today.day.to_s).last.click
    end
    click_on "Inventory before"

    expect(page).to have_selector("table tbody tr", count: 1)
    expect(page).to have_selector("table tbody tr", count: 1)

    expect(all("table tbody tr").count).to eq 1
    # expect(all("table tbody tr").count).to eq 1
    expect(all("table tbody tr")[0]).to have_content(model_2.version)

    # pool 1
    # category_id=cat_1.id
    visit "/inventory/#{pool_1.id}/list"

    click_on "Categories"
    click_on cat_1.id
    expect(page).to have_content(cat_1.name.to_s)

    expect(all("table tbody tr").count).to eq 2
    expect(all("table tbody tr")[0]).to have_content(model_1.version)
    expect(all("table tbody tr")[1]).to have_content(model_2.version)

    # pool 1
    # type=package
    visit "/inventory/#{pool_1.id}/list"
    click_on "Inventory type"
    click_on "Package"
    expect(page).to have_content("Package")

    expect(all("table tbody tr").count).to eq 1
    expect(all("table tbody tr")[0]).to have_content(model_8.version)

    # type=model
    visit "/inventory/#{pool_1.id}/list"
    click_on "Inventory type"
    click_on "Model"
    expect(page).to have_content("Model")

    expect(all("table tbody tr").count).to eq 9
    expect(all("table tbody tr")[0]).to have_content(model_1.version)
    expect(all("table tbody tr")[1]).to have_content(model_10.version) # package child model
    expect(all("table tbody tr")[2]).to have_content(model_2.version)
    expect(all("table tbody tr")[3]).to have_content(model_3.version)
    expect(all("table tbody tr")[4]).to have_content(model_4.version)
    expect(all("table tbody tr")[5]).to have_content(model_5.version)
    expect(all("table tbody tr")[6]).to have_content(model_6.version)
    expect(all("table tbody tr")[7]).to have_content(model_7.version)
    expect(all("table tbody tr")[8]).to have_content(model_9.version)  # package child model

    # type=option
    visit "/inventory/#{pool_1.id}/list"

    # set some filters to check if they are ignored
    # before selecting option
    click_on "Categories"
    click_on cat_1.id
    find("html").click
    find("html").click

    click_on "Status"
    click_on "Broken"
    click_on "Yes"

    click_on "Status"
    click_on "In stock"
    click_on "No"

    click_on "Inventory before"
    within ".rdp" do
      all(:button, Date.today.day.to_s).last.click
    end
    click_on "Inventory before"

    select_value("with_items", "with_items")
    select_value("borrowable", "borrowable")
    select_value("retired", "retired")

    click_on "Inventory pool"
    find("[data-value='#{pool_2.id}']").click
    find("html").click
    find("html").click

    expect(page).to have_content(cat_1.name.to_s)
    expect(page).to have_content("Audio")
    expect(page).to have_content("Broken - Yes")
    expect(page).to have_content("In stock - No")
    expect(page).to have_content(pool_2.name)
    expect(page).to have_content(Date.today.day.to_s)

    click_on "Inventory type"
    click_on "Option"

    expect(page).to have_content("Option")

    expect(page).to have_button("Status", disabled: true)
    expect(page).to have_button("Inventory pool", disabled: true)
    expect(page).to have_button("Categories", disabled: true)
    expect(page).to have_button("Inventory before", disabled: true)
    expect(page).to have_button("retired", disabled: true)
    expect(page).to have_button("only models with items", disabled: true)
    expect(page).to have_button("borrowable", disabled: true)

    expect(all("table tbody tr").count).to eq 1
    expect(all("table tbody tr")[0]).to have_content(option_1.version)

    # pool 5
    # with_items=true
    visit "/inventory/#{pool_5.id}/list"
    select_value("with_items", "with_items")

    expect(all("table tbody tr").count).to eq 0
  end
end
