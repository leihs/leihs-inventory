require "spec_helper"
require_relative "../shared/common"

feature "Inventory Page", type: :feature do
  scenario "default filter" do
    pool_1 = FactoryBot.create(:inventory_pool)
    pool_2 = FactoryBot.create(:inventory_pool)
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

    FactoryBot.create(:item,
      owner_id: pool_1.id,
      inventory_pool_id: pool_1.id,
      leihs_model: model_2,
      retired: nil)

    FactoryBot.create(:item,
      owner_id: pool_1.id,
      inventory_pool_id: pool_1.id,
      leihs_model: model_3,
      retired: Date.yesterday,
      retired_reason: Faker::Lorem.sentence)

    login(user)

    find("nav button", text: "Inventar").click
    click_on pool_1.name
    expect(page).to have_content("Inventarliste - #{pool_1.name}")
    uri = URI.parse(current_url)
    query_params = CGI.parse(uri.query)
    expect(query_params).to eq({"with_items" => ["true"], "retired" => ["false"], "page" => ["1"], "size" => ["20"]})
    expect(all("table tbody tr").count).to eq 1
    expect(first("table tbody tr")).to have_content(model_2.product)

    visit "/inventory/#{pool_1.id}/models?with_items=true&retired=true"
    expect(all("table tbody tr").count).to eq 1
    expect(first("table tbody tr")).to have_content(model_3.product)

    visit "/inventory/#{pool_1.id}/models?with_items=true"
    expect(all("table tbody tr").count).to eq 2
    expect(all("table tbody tr")[0]).to have_content(model_2.product)
    expect(all("table tbody tr")[1]).to have_content(model_3.product)

    visit "/inventory/#{pool_1.id}/models?with_items=false"
    expect(all("table tbody tr").count).to eq 1
    expect(all("table tbody tr")[0]).to have_content(model_1.product)

    visit "/inventory/#{pool_1.id}/models?page=1&size=20"
    expect(all("table tbody tr").count).to eq 3
    expect(all("table tbody tr")[0]).to have_content(model_1.product)
    expect(all("table tbody tr")[1]).to have_content(model_2.product)
    expect(all("table tbody tr")[2]).to have_content(model_3.product)

    visit "/inventory/#{pool_1.id}/models?search=#{model_1.version}"
    expect(all("table tbody tr").count).to eq 1
    expect(all("table tbody tr")[0]).to have_content(model_1.product)

    visit "/inventory/#{pool_2.id}/models?with_items=true"
    expect(all("table tbody tr").count).to eq 0
  end
end
