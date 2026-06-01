require "features_helper"
require_relative "../shared/common"

feature "Inventory list read-only pool (group_manager)", type: :feature do
  let(:user) { FactoryBot.create(:user, language_locale: "en-GB") }
  let(:pool) { FactoryBot.create(:inventory_pool, shortname: "RO") }
  let(:search_token) { "RO-Timeline" }
  let(:model) do
    FactoryBot.create(:leihs_model, product: "ReadOnlyModel #{search_token}", version: "v1")
  end
  let(:package_model) do
    FactoryBot.create(:leihs_model,
      product: "ReadOnlyPackage #{search_token}",
      version: "v1",
      is_package: true)
  end
  let(:software_model) do
    FactoryBot.create(:leihs_model,
      product: "ReadOnlySoftware #{search_token}",
      version: "v1",
      type: "Software")
  end
  let(:option) do
    FactoryBot.create(:option,
      product: "ReadOnlyOption #{search_token}",
      version: "v1",
      inventory_code: "RO-OPT-#{search_token}",
      inventory_pool: pool,
      price: 10.00)
  end
  let(:building) { FactoryBot.create(:building, name: "RO Building", code: "RO1") }
  let(:room) { FactoryBot.create(:room, name: "RO Room", building: building) }

  before do
    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :group_manager)

    create_timeline_list_items(
      pool: pool,
      room: room,
      model: model,
      package_model: package_model,
      software_model: software_model
    )
  end

  scenario "hides edit UI and limits tabs to inventory list" do
    login(user)
    visit "/inventory/#{pool.id}/list?page=1&size=50&with_items=true&retired=false"

    expect(page).to have_content(pool.name, wait: 20)
    expect(page).to have_content("Inventory List")

    expect(page).not_to have_css('[data-test-id="add-inventory-dropdown"]')
    expect(page).not_to have_link(href: %r{search-edit})
    expect(page).not_to have_link(href: %r{scan-edit})
    expect(page).not_to have_link(href: %r{entitlement-groups})
    expect(page).not_to have_link(href: %r{templates})

    find("input[name='search']").set(search_token)
    await_debounce

    expect_group_manager_timeline_on_model_row(pool: pool, model: model)
    expect_group_manager_timeline_on_model_row(pool: pool, model: package_model)
    expect_group_manager_timeline_on_model_row(pool: pool, model: software_model)

    visit "/inventory/#{pool.id}/list?page=1&size=50&retired=false"
    find("input[name='search']").set(search_token)
    await_debounce

    expect_no_timeline_on_model_row(product_label: "#{option.product} #{option.version}")

    within find('[data-row="model"]', text: model.name, wait: 10) do
      click_on "expand-button"
    end

    within find('[data-row="item"]') do
      expect(page).not_to have_link(href: %r{\.\./items/})
      expect(page).not_to have_css('[data-test-id="edit-dropdown"]')
    end
  end
end
