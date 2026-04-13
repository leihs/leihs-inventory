require "features_helper"
require_relative "../shared/common"

feature "Search & Edit", type: :feature do
  let(:user) { FactoryBot.create(:user, language_locale: "en-GB") }
  let(:pool) { FactoryBot.create(:inventory_pool) }
  let!(:model) { FactoryBot.create(:leihs_model) }

  before(:each) do
    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)
  end

  scenario "shows add filter prompt when no filter is active" do
    login(user)
    visit "/inventory/#{pool.id}/search-edit"

    expect(page).to have_button("Add filters to start your search.")
    expect(page).not_to have_css("table")
  end

  scenario "filters items by inventory_code and bulk-edits status_note" do
    inventory_code = "TESTCODE-#{Faker::Alphanumeric.alpha(number: 6).upcase}"
    status_note_new = Faker::Lorem.sentence

    item = FactoryBot.create(:item,
      inventory_code: inventory_code,
      leihs_model: model,
      inventory_pool: pool,
      owner: pool)

    # Create a second item to confirm filtering is precise
    FactoryBot.create(:item,
      inventory_code: "OTHER-#{Faker::Alphanumeric.alpha(number: 6).upcase}",
      leihs_model: model,
      inventory_pool: pool,
      owner: pool)

    login(user)
    visit "/inventory/#{pool.id}/search-edit"

    # Add the first filter group
    click_on "Add filters to start your search."

    # Wait for the filter row to appear, then choose the "Inventory Code" field
    expect(page).to have_button("Add AND")
    click_on "field-select-0"

    within find('[data-test-id="field-options-0"]') do
      click_on "Inventory Code"
    end

    # Enter the inventory code value
    await_debounce
    find("input[name='$or.0.$and.0.value']", wait: 10).set(inventory_code)
    await_debounce

    # Results table should appear with exactly this item
    expect(page).to have_content(inventory_code, wait: 10)
    expect(page).not_to have_content("No items found")

    # Select the matching item via its row checkbox
    within find("tbody tr", text: inventory_code) do
      find('button[role="checkbox"]').click
    end

    # "Edit 1 item" button becomes enabled
    click_on "Edit 1 item"

    # Dialog opens — add a field to edit
    expect(page).to have_content("Edit 1 item", wait: 10)
    click_on "Add field"

    # Choose "Status note" field in the dialog selector
    click_on "field-select-0"

    within find('[data-test-id="field-options"]') do
      click_on "Status note"
    end

    # Fill in the new status note value
    find("textarea[name='update.0.value']", wait: 10).set(status_note_new)

    # Submit the dialog
    click_on "Apply to 1 item"

    # Dialog closes after submission
    expect(page).not_to have_content("Edit 1 item", wait: 10)

    # Verify the change was persisted in the database
    expect(item.reload.status_note).to eq(status_note_new)
  end

  scenario "'Edit N items' button is disabled when no items are selected" do
    FactoryBot.create(:item,
      inventory_code: "ITEM-#{Faker::Alphanumeric.alpha(number: 6).upcase}",
      leihs_model: model,
      inventory_pool: pool,
      owner: pool)

    login(user)
    visit "/inventory/#{pool.id}/search-edit"

    # Add a filter to trigger the results table
    click_on "Add filters to start your search."
    expect(page).to have_button("Add AND")
    first('[role="combobox"]').click
    find("button", text: "Inventory Code", wait: 10).click
    await_debounce

    # Wait for results to load
    expect(page).to have_css("tbody tr", wait: 10)

    # "Edit 0 items" button must be disabled before any selection
    expect(page).to have_button("Edit 0 items", disabled: true)
  end

  scenario "supports multiple OR groups" do
    inv_code_a = "ALPHA-#{Faker::Alphanumeric.alpha(number: 6).upcase}"
    inv_code_b = "BETA-#{Faker::Alphanumeric.alpha(number: 6).upcase}"

    FactoryBot.create(:item,
      inventory_code: inv_code_a,
      leihs_model: model,
      inventory_pool: pool,
      owner: pool)

    FactoryBot.create(:item,
      inventory_code: inv_code_b,
      leihs_model: model,
      inventory_pool: pool,
      owner: pool)

    login(user)
    visit "/inventory/#{pool.id}/search-edit"

    # Add first OR group and filter by inv_code_a
    click_on "Add filters to start your search."
    expect(page).to have_button("Add AND")
    first('[role="combobox"]').click
    find("button", text: "Inventory Code", wait: 10).click
    await_debounce
    find("input[name='$or.0.$and.0.value']", wait: 10).set(inv_code_a)
    await_debounce

    expect(page).to have_content(inv_code_a, wait: 10)
    expect(page).not_to have_content(inv_code_b)

    # Add a second OR group
    click_on "Add OR"
    await_debounce

    # The second OR group's combobox is now second in the list
    all('[role="combobox"]').last.click
    find("button", text: "Inventory Code", wait: 10).click
    await_debounce
    find("input[name='$or.1.$and.0.value']", wait: 10).set(inv_code_b)
    await_debounce

    # Both items should now appear
    expect(page).to have_content(inv_code_a, wait: 10)
    expect(page).to have_content(inv_code_b, wait: 10)
  end

  scenario "clears item selection when the filter changes" do
    inventory_code = "SEL-#{Faker::Alphanumeric.alpha(number: 6).upcase}"

    FactoryBot.create(:item,
      inventory_code: inventory_code,
      leihs_model: model,
      inventory_pool: pool,
      owner: pool)

    login(user)
    visit "/inventory/#{pool.id}/search-edit"

    # Add filter and wait for results
    click_on "Add filters to start your search."
    expect(page).to have_button("Add AND")
    first('[role="combobox"]').click
    find("button", text: "Inventory Code", wait: 10).click
    await_debounce
    find("input[name='$or.0.$and.0.value']", wait: 10).set(inventory_code)
    await_debounce

    expect(page).to have_content(inventory_code, wait: 10)

    # Select the item
    within find("tbody tr", text: inventory_code) do
      find('button[role="checkbox"]').click
    end

    expect(page).to have_button("Edit 1 item")

    # Modify the filter value — this should reset selection
    find("input[name='$or.0.$and.0.value']").set("")
    await_debounce

    # Selection is cleared, button reverts to disabled state with 0 count
    expect(page).to have_button("Edit 0 items", disabled: true, wait: 10)
  end
end
