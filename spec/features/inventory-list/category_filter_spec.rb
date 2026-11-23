require "features_helper"
require_relative "../shared/common"

feature "Inventory list category filter", type: :feature do
  let!(:pool) { FactoryBot.create(:inventory_pool) }
  let!(:user) { FactoryBot.create(:user, language_locale: "en-GB") }

  before(:each) do
    FactoryBot.create(:access_right, inventory_pool: pool, user: user, role: :inventory_manager)
  end

  scenario "expands the path to a selected sub-category" do
    root = FactoryBot.create(:category, name: "Instruments")
    child = FactoryBot.create(:category, name: "Strings", parents: [root])
    grandchild = FactoryBot.create(:category, name: "Violins", parents: [child])

    login(user)
    visit "/inventory/#{pool.id}/list"

    click_on "category-filter-button"
    within "[data-test-id='category-filter-dropdown']" do
      click_on root.name
      click_on child.name
      find("[data-test-id='#{grandchild.id}']").click
    end

    expect(page).to have_button("category-filter-button", text: grandchild.name)

    click_on "category-filter-button"
    within "[data-test-id='category-filter-dropdown']" do
      expect(page).to have_content(grandchild.name)
      expect(page).to have_css("[data-test-id='#{grandchild.id}'][aria-checked='true']")
    end
  end

  scenario "sorts sub-categories by name" do
    parent = FactoryBot.create(:category, name: "Sounds")
    ["Zoom", "Ähre", "Bass"].each do |name|
      FactoryBot.create(:category, name: name, parents: [parent])
    end

    login(user)
    visit "/inventory/#{pool.id}/list"

    click_on "category-filter-button"
    dropdown = find("[data-test-id='category-filter-dropdown']")
    within dropdown do
      click_on parent.name
      expect(page).to have_content("Zoom")
    end
    expect(dropdown.text).to match(/Ähre.*Bass.*Zoom/m)
  end
end
