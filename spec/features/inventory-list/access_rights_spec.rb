require "features_helper"
require_relative "../shared/common"

feature "Access rights on inventory list", type: :feature do
  # Searches for a model row by product name and yields within it.
  def within_model_row(product, &block)
    find("input[name='search']").set(product)
    await_debounce
    within find('[data-row="model"]', text: product, wait: 10), &block
  end

  # Expands a model row and returns the first revealed item sub-row.
  def expand_model_row(product, item)
    find("input[name='search']").set(product)
    await_debounce
    within find('[data-row="model"]', text: product, wait: 10) do
      click_on "expand-button"
    end
    find('[data-row="item"]', text: item.inventory_code, wait: 10)
  end

  def assert_all_tabs_visible
    expect(page).to have_link("Inventory List")
    expect(page).to have_link("Advanced Search")
    expect(page).to have_link("Statistics")
    expect(page).to have_link("Entitlement Groups")
    expect(page).to have_link("Templates")
  end

  def assert_only_inventory_list_tab_visible
    expect(page).to have_link("Inventory List")
    expect(page).to have_no_link("Advanced Search")
    expect(page).to have_no_link("Statistics")
    expect(page).to have_no_link("Entitlement Groups")
    expect(page).to have_no_link("Templates")
  end

  %i[lending_manager inventory_manager].each do |role|
    scenario "#{role} sees all tabs, the Add Inventory button, and edit buttons on every row" do
      user = FactoryBot.create(:user, language_locale: "en-GB")
      pool = FactoryBot.create(:inventory_pool)
      room = FactoryBot.create(:room)

      FactoryBot.create(:access_right,
        inventory_pool: pool,
        user: user,
        role: role)

      model = FactoryBot.create(:leihs_model,
        product: "AR#{role}Model",
        version: "1.0")

      item = FactoryBot.create(:item,
        leihs_model: model,
        inventory_pool: pool,
        owner: pool,
        room: room)

      software_model = FactoryBot.create(:leihs_model,
        product: "AR#{role}Software",
        version: "1.0",
        type: "Software")

      license = FactoryBot.create(:item,
        leihs_model: software_model,
        inventory_pool: pool,
        owner: pool,
        room: room)

      pkg_model = FactoryBot.create(:leihs_model,
        product: "AR#{role}Package",
        version: "1.0",
        is_package: true)

      FactoryBot.create(:item,
        leihs_model: pkg_model,
        inventory_pool: pool,
        owner: pool,
        room: room)

      login(user)
      visit "/inventory/#{pool.id}/list?with_items=true&page=1&size=50&retired=false"

      # All tabs are visible
      assert_all_tabs_visible

      # Add Inventory button is present
      expect(page).to have_content("Add inventory")

      # Model row (type "Model") has an edit button
      within_model_row(model.product) do
        expect(page).to have_link("edit")
      end

      # Item sub-row has an edit button
      item_row = expand_model_row(model.product, item)
      within(item_row) do
        expect(page).to have_link("edit")
      end

      # Package model row (type "Package") has an edit button
      within_model_row(pkg_model.product) do
        expect(page).to have_link("edit")
      end

      # Software model row (type "Software") has an edit button
      within_model_row(software_model.product) do
        expect(page).to have_link("edit")
      end

      # License sub-row has an edit button
      license_row = expand_model_row(software_model.product, license)
      within(license_row) do
        expect(page).to have_link("edit")
      end
    end
  end

  scenario "group_manager sees only the Inventory List tab, no Add Inventory button, Timeline on model rows, and no edit buttons on item rows" do
    user = FactoryBot.create(:user, language_locale: "en-GB")
    pool = FactoryBot.create(:inventory_pool)
    room = FactoryBot.create(:room)

    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :group_manager)

    model = FactoryBot.create(:leihs_model,
      product: "ARGroupManagerModel",
      version: "1.0")

    item = FactoryBot.create(:item,
      leihs_model: model,
      inventory_pool: pool,
      owner: pool,
      room: room)

    software_model = FactoryBot.create(:leihs_model,
      product: "ARGroupManagerSoftware",
      version: "1.0",
      type: "Software")

    license = FactoryBot.create(:item,
      leihs_model: software_model,
      inventory_pool: pool,
      owner: pool,
      room: room)

    pkg_model = FactoryBot.create(:leihs_model,
      product: "ARGroupManagerPackage",
      version: "1.0",
      is_package: true)

    FactoryBot.create(:item,
      leihs_model: pkg_model,
      inventory_pool: pool,
      owner: pool,
      room: room)

    login(user)
    visit "/inventory/#{pool.id}/list?with_items=true&page=1&size=50&retired=false"

    # Only the Inventory List tab is visible
    assert_only_inventory_list_tab_visible

    # Add Inventory button is not present
    expect(page).to have_no_selector('[data-test-id="add-inventory-dropdown"]')

    # Model row (type "Model") has a Timeline button, no edit button
    within_model_row(model.product) do
      expect(page).to have_link("Timeline")
      expect(page).to have_no_link("edit")
    end

    # Item sub-row has no visible edit button
    item_row = expand_model_row(model.product, item)
    within(item_row) do
      expect(page).to have_no_link("edit")
    end

    # Package model row (type "Package") has a Timeline button, no edit button
    within_model_row(pkg_model.product) do
      expect(page).to have_link("Timeline")
      expect(page).to have_no_link("edit")
    end

    # Software model row (type "Software") has a Timeline button, no edit button
    within_model_row(software_model.product) do
      expect(page).to have_link("Timeline")
      expect(page).to have_no_link("edit")
    end

    # License sub-row has no visible edit button
    license_row = expand_model_row(software_model.product, license)
    within(license_row) do
      expect(page).to have_no_link("edit")
    end
  end
end
