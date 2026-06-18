require "features_helper"
require_relative "../shared/common"

feature "Inventory list manager timeline actions", type: :feature do
  let(:search_token) { "MgrTimeline" }
  let(:building) { FactoryBot.create(:building, name: "Mgr Building", code: "MB1") }
  let(:room) { FactoryBot.create(:room, name: "Mgr Room", building: building) }
  let(:pool) { FactoryBot.create(:inventory_pool, shortname: "MT") }
  let(:model) do
    FactoryBot.create(:leihs_model, product: "MgrModel #{search_token}", version: "v1")
  end
  let(:package_model) do
    FactoryBot.create(:leihs_model,
      product: "MgrPackage #{search_token}",
      version: "v1",
      is_package: true)
  end
  let(:software_model) do
    FactoryBot.create(:leihs_model,
      product: "MgrSoftware #{search_token}",
      version: "v1",
      type: "Software")
  end
  let(:option) do
    FactoryBot.create(:option,
      product: "MgrOption #{search_token}",
      version: "v1",
      inventory_code: "MT-OPT-#{search_token}",
      inventory_pool: pool,
      price: 10.00)
  end

  before do
    create_timeline_list_items(
      pool: pool,
      room: room,
      model: model,
      package_model: package_model,
      software_model: software_model
    )
  end

  %i[inventory_manager lending_manager].each do |role|
    scenario "shows timeline in edit dropdown for model, package, and software (#{role})" do
      user = FactoryBot.create(:user, language_locale: "en-GB")
      FactoryBot.create(:access_right,
        inventory_pool: pool,
        user: user,
        role: role)

      login(user)
      visit "/inventory/#{pool.id}/list?page=1&size=50&with_items=true&retired=false"

      expect(page).to have_content(pool.name, wait: 20)
      find("input[name='search']").set(search_token)
      await_debounce

      expect_manager_timeline_in_edit_dropdown(pool: pool, model: model)
      expect_manager_timeline_in_edit_dropdown(pool: pool, model: package_model)
      expect_manager_timeline_in_edit_dropdown(pool: pool, model: software_model)

      visit "/inventory/#{pool.id}/list?page=1&size=50&retired=false"
      find("input[name='search']").set(search_token)
      await_debounce

      expect_manager_option_row_without_timeline(
        product_label: "#{option.product} #{option.version}"
      )

      within find('[data-row="model"]', text: model.name) do
        expect(page).not_to have_link("Timeline")
      end
    end
  end

  def expect_manager_option_row_without_timeline(product_label:)
    expect(page).to have_css('[data-row="model"]', text: product_label)
    within find('[data-row="model"]', text: product_label) do
      expect(page).not_to have_link("Timeline")
      expect(page).to have_link("edit")
    end
  end

  def expect_manager_timeline_in_edit_dropdown(pool:, model:, product_label: nil)
    label = product_label || model.name
    expect(page).to have_css('[data-row="model"]', text: label)
    within find('[data-row="model"]', text: label) do
      click_on "edit-dropdown"
    end
    expect(page).to have_link("Timeline")
    timeline_link = find(:link, "Timeline")
    expect(timeline_link[:href]).to end_with(
      "/manage/#{pool.id}/models/#{model.id}/timeline"
    )
    expect(timeline_link[:target]).to eq("_blank")
    send_keys :escape
  end
end
