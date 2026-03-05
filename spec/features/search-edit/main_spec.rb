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
    expect(page).to have_button("Add search parameter")
    click_on "or-0-field-select-0"

    within find('[data-test-id="field-options"]') do
      click_on "Inventory Code"
    end

    # Enter the inventory code value
    await_debounce
    find("input[name='$or.0.$and.0.value']").set(inventory_code)
    await_debounce

    # Results table should appear with exactly this item
    expect(page).to have_content(inventory_code)
    expect(page).not_to have_content("No items found")

    # Wait for results to load
    expect(page).to have_css("tbody tr")

    # "Edit 0 items" button must be disabled before any selection
    expect(page).to have_button("edit-button", disabled: true)

    # Select the matching item via its row checkbox
    within find("tbody tr", text: inventory_code) do
      find('button[role="checkbox"]').click
    end

    # "Edit 1 item" button becomes enabled
    click_on "edit-button"

    # Dialog opens — add a field to edit
    expect(page).to have_content("Add field")

    within find('[id="edit-dialog-form"]') do
      # Choose "Status note" field in the dialog selector
      click_on "field-select-0"
    end
    within find('[data-test-id="field-options"]') do
      click_on "Status note"
    end

    # Fill in the new status note value
    find("textarea[name='update.0.value']").set(status_note_new)
    # Submit the dialog
    click_on "apply-button"
    expect(page).to have_content("1 Items was successfully updated")

    # Dialog closes after submission
    expect(page).not_to have_content("Edit 1 item")

    # Verify the change was persisted in the database
    expect(item.reload.status_note).to eq(status_note_new)
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
    expect(page).to have_button("Add search parameter")
    click_on "or-0-field-select-0"

    within find('[data-test-id="field-options"]') do
      click_on "Inventory Code"
    end
    await_debounce
    find("input[name='$or.0.$and.0.value']").set(inv_code_a)
    await_debounce

    expect(page).to have_content(inv_code_a)
    expect(page).not_to have_content(inv_code_b)

    # Add a second OR group
    click_on "Add OR"
    await_debounce

    # The second OR group's combobox is now second in the list
    click_on "or-1-field-select-0"

    within find('[data-test-id="field-options"]') do
      click_on "Inventory Code"
    end

    await_debounce
    find("input[name='$or.1.$and.0.value']").set(inv_code_b)
    await_debounce

    # Both items should now appear
    expect(page).to have_content(inv_code_a)
    expect(page).to have_content(inv_code_b)
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
    expect(page).to have_button("Add search parameter")
    click_on "or-0-field-select-0"

    within find('[data-test-id="field-options"]') do
      click_on "Inventory Code"
    end

    await_debounce
    find("input[name='$or.0.$and.0.value']").set(inventory_code)
    await_debounce

    expect(page).to have_content(inventory_code)

    # Select the item
    within find("tbody tr", text: inventory_code) do
      find('button[role="checkbox"]').click
    end

    expect(page).to have_button("edit-button", text: "Edit", disabled: false)

    # Modify the filter value — this should reset selection
    find("input[name='$or.0.$and.0.value']").set("")
    await_debounce

    # Selection is cleared, button reverts to disabled state with 0 count
    expect(page).to have_button("edit-button", disabled: true)
  end

  context "field visibility based on item ownership" do
    let(:prefix) { "OWN-#{Faker::Alphanumeric.alpha(number: 6).upcase}" }

    def filter_by_prefix(prefix)
      click_on "Add filters to start your search."
      expect(page).to have_button("Add search parameter")
      click_on "or-0-field-select-0"
      within find('[data-test-id="field-options"]') do
        click_on "Inventory Code"
      end
      await_debounce
      find("input[name='$or.0.$and.0.value']").set(prefix)
      await_debounce
      expect(page).to have_css("tbody tr")
    end

    scenario "shows all fields and no ownership warning when all selected items are owned by the pool" do
      FactoryBot.create(:item,
        inventory_code: "#{prefix}-1",
        leihs_model: model,
        inventory_pool: pool,
        owner: pool)

      login(user)
      visit "/inventory/#{pool.id}/search-edit"

      filter_by_prefix(prefix)
      await_debounce
      find("thead button[role='checkbox']").click
      click_on "edit-button"

      expect(page).to have_content("Add field")
      expect(page).not_to have_content("not all selected items are owned by this pool")

      within find('[id="edit-dialog-form"]') do
        click_on "field-select-0"
      end
      within find('[data-test-id="field-options"]') do
        expect(page).to have_content("Initial Price")
        expect(page).to have_content("Status note")
      end
    end

    scenario "hides owner-only fields and shows ownership warning when selected items include non-owned items" do
      other_pool = FactoryBot.create(:inventory_pool)

      FactoryBot.create(:item,
        inventory_code: "#{prefix}-1",
        leihs_model: model,
        inventory_pool: pool,
        owner: pool)

      FactoryBot.create(:item,
        inventory_code: "#{prefix}-2",
        leihs_model: model,
        inventory_pool: pool,
        owner: other_pool)

      login(user)
      visit "/inventory/#{pool.id}/search-edit"

      filter_by_prefix(prefix)
      await_debounce
      find("thead button[role='checkbox']").click
      click_on "edit-button"

      expect(page).to have_content("Add field")
      expect(page).to have_content("Not all selected items are owned by this pool, only fields that are editable for all selected items are shown in the field selection.")

      within find('[id="edit-dialog-form"]') do
        click_on "field-select-0"
      end
      within find('[data-test-id="field-options"]') do
        expect(page).not_to have_content("Initial Price")
        expect(page).to have_content("Status note")
      end
    end
  end
end
