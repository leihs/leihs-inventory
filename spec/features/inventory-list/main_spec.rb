require "spec_helper"
require_relative "../shared/common"

feature "Inventory Page", type: :feature do
  scenario "default filter" do
    # |---------|--------|--------|--------|---------|------------|----------|------------|--------|------------|
    # | model   | item   | owner  | pool   | retired | borrowable | in_stock | incomplete | broken | last_check |
    # |---------|--------|--------|--------|---------|------------|----------|------------|--------|------------|
    # | model_1 |        |        |        |         |            |          |            |        |            |
    # | model_2 | PA100  | pool_1 | pool_1 | false   | true       | true     | false      | false  | 2024-12-31 |
    # | model_3 | PA101  | pool_1 | pool_1 | true    | true       | true     | false      | false  |            |
    # | model_4 | PA102  | pool_1 | pool_1 | true    | false      | true     | true       | false  |            |
    # | model_5 | PA103  | pool_1 | pool_2 | true    | true       | true     | false      | false  |            |
    # | model_6 | PC104  | pool_3 | pool_1 | false   | true       | true     | false      | true   |            |
    # | model_7 | PD105  | pool_4 | pool_4 | false   | true       | true     | false      | false  |            |
    # | model_8 | PA106  | pool_1 | pool_1 | false   | true       | false    | false      | false  |            |
    # |---------|--------|--------|--------|---------|------------|----------|------------|--------|------------|

    pool_1 = FactoryBot.create(:inventory_pool, shortname: "PA")
    pool_2 = FactoryBot.create(:inventory_pool, shortname: "PB")
    pool_3 = FactoryBot.create(:inventory_pool, shortname: "PC")
    pool_4 = FactoryBot.create(:inventory_pool, shortname: "PD")
    pool_5 = FactoryBot.create(:inventory_pool, shortname: "PE")

    user = FactoryBot.create(:user)
    [pool_1, pool_2].each do |pool|
      FactoryBot.create(:access_right,
        inventory_pool: pool,
        user: user,
        role: :inventory_manager)
    end

    model_1 = FactoryBot.create(:leihs_model, product: "Model", version: "AA1")
    model_2 = FactoryBot.create(:leihs_model, product: "Model", version: "AA2")
    model_3 = FactoryBot.create(:leihs_model, product: "Model", version: "AA3")
    model_4 = FactoryBot.create(:leihs_model, product: "Model", version: "AA4")
    model_5 = FactoryBot.create(:leihs_model, product: "Model", version: "AA5")
    model_6 = FactoryBot.create(:leihs_model, product: "Model", version: "AA6")
    model_7 = FactoryBot.create(:leihs_model, product: "Model", version: "AA7")
    model_8 = FactoryBot.create(:leihs_model, product: "Model", version: "AA8")

    FactoryBot.create(:item,
      inventory_code: "#{pool_1.shortname}100",
      owner_id: pool_1.id,
      inventory_pool_id: pool_1.id,
      leihs_model: model_2,
      last_check: Date.new(2024, 12, 31),
      is_borrowable: true,
      retired: nil)

    FactoryBot.create(:item,
      inventory_code: "#{pool_1.shortname}101",
      owner_id: pool_1.id,
      inventory_pool_id: pool_1.id,
      leihs_model: model_3,
      is_borrowable: true,
      retired: Date.yesterday,
      retired_reason: Faker::Lorem.sentence)

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

    item = FactoryBot.create(:item,
      inventory_code: "#{pool_1.shortname}106",
      owner_id: pool_1.id,
      inventory_pool_id: pool_1.id,
      leihs_model: model_8,
      is_borrowable: true,
      retired: nil)

    user_2 = FactoryBot.create(:user)
    contract = Contract.create_with_disabled_triggers(
      "a9bf950b-e91e-4a23-85e1-5fd8163d234a",
      user_2.id,
      pool_1.id
    )

    FactoryBot.create(:reservation,
      status: :signed,
      leihs_model: model_8,
      item_id: item.id,
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

    find("nav button", text: "Inventar").click
    click_on pool_1.name
    expect(page).to have_content("Inventarliste - #{pool_1.name}")
    uri = URI.parse(current_url)
    query_params = CGI.parse(uri.query)
    expect(query_params).to eq({"with_items" => ["true"], "retired" => ["false"], "page" => ["1"], "size" => ["20"]})
    expect(all("table tbody tr").count).to eq 3
    expect(all("table tbody tr")[0]).to have_content(model_2.version)
    expect(all("table tbody tr")[1]).to have_content(model_6.version)
    expect(all("table tbody tr")[2]).to have_content(model_8.version)

    visit "/inventory/#{pool_1.id}/models?with_items=true&retired=true"
    expect(all("table tbody tr").count).to eq 3
    expect(all("table tbody tr")[0]).to have_content(model_3.version)
    expect(all("table tbody tr")[1]).to have_content(model_4.version)
    expect(all("table tbody tr")[2]).to have_content(model_5.version)

    visit "/inventory/#{pool_1.id}/models?with_items=true&retired=true&borrowable=false"
    expect(all("table tbody tr").count).to eq 1
    expect(first("table tbody tr")).to have_content(model_4.version)

    visit "/inventory/#{pool_1.id}/models?with_items=true"
    expect(all("table tbody tr").count).to eq 6
    expect(all("table tbody tr")[0]).to have_content(model_2.version)
    expect(all("table tbody tr")[1]).to have_content(model_3.version)
    expect(all("table tbody tr")[2]).to have_content(model_4.version)
    expect(all("table tbody tr")[3]).to have_content(model_5.version)
    expect(all("table tbody tr")[4]).to have_content(model_6.version)
    expect(all("table tbody tr")[5]).to have_content(model_8.version)

    visit "/inventory/#{pool_1.id}/models?with_items=false"
    expect(all("table tbody tr").count).to eq 1
    expect(all("table tbody tr")[0]).to have_content(model_1.version)

    visit "/inventory/#{pool_1.id}/models?page=1&size=20"
    expect(all("table tbody tr").count).to eq 7
    expect(all("table tbody tr")[0]).to have_content(model_1.version)
    expect(all("table tbody tr")[1]).to have_content(model_2.version)
    expect(all("table tbody tr")[2]).to have_content(model_3.version)
    expect(all("table tbody tr")[3]).to have_content(model_4.version)
    expect(all("table tbody tr")[4]).to have_content(model_5.version)
    expect(all("table tbody tr")[5]).to have_content(model_6.version)
    expect(all("table tbody tr")[6]).to have_content(model_8.version)

    visit "/inventory/#{pool_1.id}/models?search=#{model_1.version}"
    expect(all("table tbody tr").count).to eq 1
    expect(all("table tbody tr")[0]).to have_content(model_1.version)

    visit "/inventory/#{pool_1.id}/models?with_items=true&inventory_pool_id=#{pool_2.id}"
    expect(all("table tbody tr").count).to eq 1
    expect(all("table tbody tr")[0]).to have_content(model_5.version)

    visit "/inventory/#{pool_1.id}/models?with_items=true&owned=true&borrowable=true"
    expect(all("table tbody tr").count).to eq 4
    expect(all("table tbody tr")[0]).to have_content(model_2.version)
    expect(all("table tbody tr")[1]).to have_content(model_3.version)
    expect(all("table tbody tr")[2]).to have_content(model_5.version)
    expect(all("table tbody tr")[3]).to have_content(model_8.version)

    visit "/inventory/#{pool_1.id}/models?with_items=true&in_stock=false"
    expect(all("table tbody tr").count).to eq 1
    expect(all("table tbody tr")[0]).to have_content(model_8.version)

    visit "/inventory/#{pool_1.id}/models?with_items=true&incomplete=true"
    expect(all("table tbody tr").count).to eq 1
    expect(all("table tbody tr")[0]).to have_content(model_4.version)

    visit "/inventory/#{pool_1.id}/models?with_items=true&broken=true"
    expect(all("table tbody tr").count).to eq 1
    expect(all("table tbody tr")[0]).to have_content(model_6.version)

    visit "/inventory/#{pool_1.id}/models?with_items=true&before_last_check=2024-12-31"
    expect(all("table tbody tr").count).to eq 1
    expect(all("table tbody tr")[0]).to have_content(model_2.version)

    visit "/inventory/#{pool_5.id}/models?with_items=true"
    expect(all("table tbody tr").count).to eq 0
  end
end
