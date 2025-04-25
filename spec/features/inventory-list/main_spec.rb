require "spec_helper"
require_relative "../shared/common"

feature "Inventory Page", type: :feature do
  scenario "default filter" do
    # |---------|--------|--------|--------|---------|------------|
    # | model   | item   | owner  | pool   | retired | borrowable |
    # |---------|--------|--------|--------|---------|------------|
    # | model_1 |        |        |        |         |            |
    # | model_2 | PA100  | pool_1 | pool_1 | false   | true       |
    # | model_3 | PA101  | pool_1 | pool_1 | true    | true       |
    # | model_4 | PA102  | pool_1 | pool_1 | true    | false      |
    # | model_5 | PA103  | pool_1 | pool_2 | true    | true       |
    # | model_6 | PC104  | pool_3 | pool_1 | false   | true       |
    # | model_7 | PD105  | pool_4 | pool_4 | false   | true       |
    # |---------|--------|--------|--------|---------|------------|

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

    model_1 = FactoryBot.create(:leihs_model, product: "Model", version: "ABC")
    model_2 = FactoryBot.create(:leihs_model, product: "Model", version: "DEF")
    model_3 = FactoryBot.create(:leihs_model, product: "Model", version: "GHI")
    model_4 = FactoryBot.create(:leihs_model, product: "Model", version: "JKL")
    model_5 = FactoryBot.create(:leihs_model, product: "Model", version: "MNO")
    model_6 = FactoryBot.create(:leihs_model, product: "Model", version: "PRS")
    model_7 = FactoryBot.create(:leihs_model, product: "Model", version: "TOV")

    FactoryBot.create(:item,
      inventory_code: "#{pool_1.shortname}100",
      owner_id: pool_1.id,
      inventory_pool_id: pool_1.id,
      leihs_model: model_2,
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
      retired: Date.yesterday,
      retired_reason: Faker::Lorem.sentence)

    FactoryBot.create(:item,
      inventory_code: "#{pool_1.shortname}103",
      owner_id: pool_1.id,
      inventory_pool_id: pool_2.id,
      retired: Date.yesterday,
      retired_reason: Faker::Lorem.sentence,
      leihs_model: model_5)

    FactoryBot.create(:item,
      inventory_code: "#{pool_3.shortname}104",
      owner_id: pool_3.id,
      inventory_pool_id: pool_1.id,
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
    expect(all("table tbody tr").count).to eq 2
    expect(all("table tbody tr")[0]).to have_content(model_2.version)
    expect(all("table tbody tr")[1]).to have_content(model_6.version)

    visit "/inventory/#{pool_1.id}/models?with_items=true&retired=true"
    expect(all("table tbody tr").count).to eq 3
    expect(all("table tbody tr")[0]).to have_content(model_3.version)
    expect(all("table tbody tr")[1]).to have_content(model_4.version)
    expect(all("table tbody tr")[2]).to have_content(model_5.version)

    visit "/inventory/#{pool_1.id}/models?with_items=true&retired=true&borrowable=false"
    expect(all("table tbody tr").count).to eq 1
    expect(first("table tbody tr")).to have_content(model_4.version)

    visit "/inventory/#{pool_1.id}/models?with_items=true"
    expect(all("table tbody tr").count).to eq 5
    expect(all("table tbody tr")[0]).to have_content(model_2.version)
    expect(all("table tbody tr")[1]).to have_content(model_3.version)
    expect(all("table tbody tr")[2]).to have_content(model_4.version)
    expect(all("table tbody tr")[3]).to have_content(model_5.version)
    expect(all("table tbody tr")[4]).to have_content(model_6.version)

    visit "/inventory/#{pool_1.id}/models?with_items=false"
    expect(all("table tbody tr").count).to eq 1
    expect(all("table tbody tr")[0]).to have_content(model_1.version)

    visit "/inventory/#{pool_1.id}/models?page=1&size=20"
    expect(all("table tbody tr").count).to eq 6
    expect(all("table tbody tr")[0]).to have_content(model_1.version)
    expect(all("table tbody tr")[1]).to have_content(model_2.version)
    expect(all("table tbody tr")[2]).to have_content(model_3.version)
    expect(all("table tbody tr")[3]).to have_content(model_4.version)
    expect(all("table tbody tr")[4]).to have_content(model_5.version)
    expect(all("table tbody tr")[5]).to have_content(model_6.version)

    visit "/inventory/#{pool_1.id}/models?search=#{model_1.version}"
    expect(all("table tbody tr").count).to eq 1
    expect(all("table tbody tr")[0]).to have_content(model_1.version)

    visit "/inventory/#{pool_1.id}/models?with_items=true&inventory_pool_id=#{pool_2.id}"
    expect(all("table tbody tr").count).to eq 1
    expect(all("table tbody tr")[0]).to have_content(model_5.version)

    visit "/inventory/#{pool_5.id}/models?with_items=true"
    expect(all("table tbody tr").count).to eq 0
  end
end
