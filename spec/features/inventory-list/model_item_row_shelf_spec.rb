require "features_helper"
require_relative "../shared/common"

feature "Model item row location on inventory list", type: :feature do
  let(:user) { FactoryBot.create(:user, language_locale: "en-GB") }
  let(:pool) { FactoryBot.create(:inventory_pool, shortname: "MS") }
  let(:owner_pool) do
    FactoryBot.create(:inventory_pool,
      name: "ModelItemRow-Owner-Pool",
      shortname: "MO")
  end
  let(:building) { FactoryBot.create(:building, name: "ItemTest-Building", code: "STB1") }
  let(:room) { FactoryBot.create(:room, building: building, name: "ItemTest-Room") }
  let(:model) do
    FactoryBot.create(:leihs_model,
      product: "Model-Shelf-Row-Product",
      version: "1.0")
  end
  let(:fixture_shelf) { "ItemTest-Shelf" }

  before do
    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)
  end

  def open_expanded_model_row
    login(user)
    visit "/inventory/#{pool.id}/list"
    select_value("with_items", "with_items")
    find("input[name='search']").set(model.product)
    await_debounce
    within find('[data-row="model"]', text: model.product, wait: 10) do
      click_on "expand-button"
    end
  end

  scenario "shows building, shelf label, and shelf value when shelf is set" do
    item = FactoryBot.create(:item,
      inventory_code: "MSHELF-WITH",
      leihs_model: model,
      inventory_pool: pool,
      owner: pool,
      room: room,
      shelf: fixture_shelf)

    open_expanded_model_row

    within find('[data-row="item"]', text: item.inventory_code, wait: 10) do
      expect(page).not_to have_content(pool.name)
      expect(page).to have_content(building.name)
      expect(page).to have_content(building.code)
      expect(page).to have_content("Shelf")
      expect(page).to have_content(fixture_shelf)
    end
  end

  scenario "shows room when shelf is blank" do
    item = FactoryBot.create(:item,
      inventory_code: "MSHELF-WITHOUT",
      leihs_model: model,
      inventory_pool: pool,
      owner: pool,
      room: room,
      shelf: nil)

    open_expanded_model_row

    within find('[data-row="item"]', text: item.inventory_code, wait: 10) do
      expect(page).not_to have_content(pool.name)
      expect(page).to have_content(building.name)
      expect(page).to have_content(building.code)
      expect(page).to have_content("Room")
      expect(page).to have_content(room.name)
      expect(page).not_to have_content(fixture_shelf)
    end
  end

  scenario "shows responsible pool name when owner differs from responsible department" do
    item = FactoryBot.create(:item,
      inventory_code: "MSHELF-CUSTODY",
      leihs_model: model,
      inventory_pool: pool,
      owner: owner_pool,
      room: room,
      shelf: fixture_shelf)

    open_expanded_model_row

    within find('[data-row="item"]', text: item.inventory_code, wait: 10) do
      expect(page).to have_content(pool.name)
      expect(page).to have_content(building.name)
      expect(page).to have_content(building.code)
      expect(page).to have_content("Shelf")
      expect(page).to have_content(fixture_shelf)
    end
  end
end
