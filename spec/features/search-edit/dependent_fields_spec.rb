require "features_helper"
require_relative "../shared/common"

feature "Search & Edit - Dependent Fields", type: :feature do
  let(:user) { FactoryBot.create(:user, language_locale: "en-GB") }
  let(:pool) { FactoryBot.create(:inventory_pool) }
  let!(:model) { FactoryBot.create(:leihs_model) }

  before(:each) do
    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)
  end

  let(:building_a) { FactoryBot.create(:building, name: "Building A") }
  let(:building_b) { FactoryBot.create(:building, name: "Building B") }
  let!(:room_a) { FactoryBot.create(:room, name: "Room A", building: building_a) }
  let!(:room_b) { FactoryBot.create(:room, name: "Room B", building: building_b) }
  let!(:item) do
    FactoryBot.create(:item,
      leihs_model: model,
      inventory_pool: pool,
      owner: pool,
      room: room_a)
  end

  def open_filter_page
    login(user)
    visit "/inventory/#{pool.id}/search-edit"
    click_on "Add filters to start your search."
    expect(page).to have_button("Add search parameter")
  end

  def select_filter_field(and_index, label)
    click_on "or-0-field-select-#{and_index}"
    within find('[data-test-id="field-options"]') do
      click_on label
    end
  end

  def open_edit_dialog
    login(user)
    visit "/inventory/#{pool.id}/search-edit"
    click_on "Add filters to start your search."
    expect(page).to have_button("Add search parameter")
    click_on "or-0-field-select-0"
    within find('[data-test-id="field-options"]') do
      click_on "Inventory Code"
    end
    await_debounce
    find("input[name='$or.0.$and.0.value']").set(item.inventory_code)
    await_debounce
    expect(page).to have_css("tbody tr")
    within find("tbody tr", text: item.inventory_code) do
      find('button[role="checkbox"]').click
    end
    click_on "edit-button"
    expect(page).to have_content("Add field")
  end

  context "search filters" do
    scenario "retired=Yes auto-adds retired_reason field; reverting to No removes it" do
      open_filter_page
      select_filter_field(0, "Retirement")

      # Set retired to Yes → retired_reason textarea should appear
      first('[name="$or.0.$and.0.value"]').click
      within find('[data-test-id="$or.0.$and.0.value-options"]') do
        click_on "Yes"
      end
      await_debounce

      expect(page).to have_css("textarea[name='$or.0.$and.1.value']")

      # The trash button for the auto-added retired_reason row should be invisible
      rows = all(".grid.grid-cols-12")
      retired_reason_row = rows[1]
      expect(retired_reason_row).to have_css("button.invisible", visible: :all)

      # Revert to No → retired_reason should be removed
      first('[name="$or.0.$and.0.value"]').click
      within find('[data-test-id="$or.0.$and.0.value-options"]') do
        click_on "No"
      end
      await_debounce

      expect(page).not_to have_css("textarea[name='$or.0.$and.1.value']")
    end

    scenario "selecting a building auto-adds room_id; changing building resets room value" do
      open_filter_page
      select_filter_field(0, "Building")

      # Pick Building A → room_id field should appear
      find('button[data-test-id="$or.0.$and.0.value"]').click
      expect(page).to have_field(placeholder: "Enter search term")
      click_on building_a.name
      await_debounce

      expect(page).to have_css('button[data-test-id="$or.0.$and.1.value"]')

      # Pick Room A in the room autocomplete
      find('button[data-test-id="$or.0.$and.1.value"]').click
      expect(page).to have_field(placeholder: "Enter search term")
      click_on room_a.name
      await_debounce

      expect(find('button[data-test-id="$or.0.$and.1.value"]')).to have_text(room_a.name)

      # Change to Building B → room_id field still present but value is cleared
      find('button[data-test-id="$or.0.$and.0.value"]').click
      expect(page).to have_field(placeholder: "Enter search term")
      click_on building_b.name
      await_debounce

      expect(page).to have_css('button[data-test-id="$or.0.$and.1.value"]')
      expect(find('button[data-test-id="$or.0.$and.1.value"]')).not_to have_text(room_a.name)
    end

    scenario "deleting building_id row also removes auto-added room_id row" do
      open_filter_page
      select_filter_field(0, "Building")

      find('button[data-test-id="$or.0.$and.0.value"]').click
      expect(page).to have_field(placeholder: "Enter search term")
      click_on building_a.name
      await_debounce

      expect(page).to have_css('button[data-test-id="$or.0.$and.1.value"]')

      # Click trash on the building_id row (first row, visible trash button)
      click_on "remove-and-0"
      await_debounce

      expect(page).not_to have_css('button[data-test-id="$or.0.$and.0.value"]')
      expect(page).not_to have_css('button[data-test-id="$or.0.$and.1.value"]')
    end
  end

  context "edit dialog" do
    scenario "retired=Yes auto-adds retired_reason; reverting to No removes it" do
      open_edit_dialog

      within find('[id="edit-dialog-form"]') do
        click_on "field-select-0"
      end
      within find('[data-test-id="field-options"]') do
        click_on "Retirement"
      end

      # Set to Yes → retired_reason textarea should appear
      first('[name="update.0.value"]').click
      within find('[data-test-id="update.0.value-options"]') do
        click_on "Yes"
      end
      await_debounce

      expect(page).to have_css("textarea[name='update.1.value']")

      # The trash button on the retired_reason row should be invisible
      rows = all("#edit-dialog-form .grid.grid-cols-12")
      retired_reason_row = rows[1]
      expect(retired_reason_row).to have_css("button.invisible", visible: :all)

      # Revert to No → retired_reason disappears
      first('[name="update.0.value"]').click
      within find('[data-test-id="update.0.value-options"]') do
        click_on "No"
      end
      await_debounce

      expect(page).not_to have_css("textarea[name='update.1.value']")
    end

    scenario "selecting a building auto-adds room_id; changing building resets room value" do
      open_edit_dialog

      within find('[id="edit-dialog-form"]') do
        click_on "field-select-0"
      end
      within find('[data-test-id="field-options"]') do
        click_on "Building"
      end

      # Pick Building A → room_id should appear
      find('button[data-test-id="update.0.value"]').click
      expect(page).to have_field(placeholder: "Enter search term")
      click_on building_a.name
      await_debounce

      expect(page).to have_css('button[data-test-id="update.1.value"]')

      # Pick Room A
      find('button[data-test-id="update.1.value"]').click
      expect(page).to have_field(placeholder: "Enter search term")
      click_on room_a.name
      await_debounce

      expect(find('button[data-test-id="update.1.value"]')).to have_text(room_a.name)

      # Change to Building B → room field still present but value cleared
      find('button[data-test-id="update.0.value"]').click
      expect(page).to have_field(placeholder: "Enter search term")
      click_on building_b.name
      await_debounce

      expect(page).to have_css('button[data-test-id="update.1.value"]')
      expect(find('button[data-test-id="update.1.value"]')).not_to have_text(room_a.name)
    end

    scenario "deleting building_id row also removes auto-added room_id row" do
      open_edit_dialog

      within find('[id="edit-dialog-form"]') do
        click_on "field-select-0"
      end
      within find('[data-test-id="field-options"]') do
        click_on "Building"
      end

      find('button[data-test-id="update.0.value"]').click
      expect(page).to have_field(placeholder: "Enter search term")
      click_on building_a.name
      await_debounce

      expect(page).to have_css('button[data-test-id="update.1.value"]')

      # Click trash on the building_id row
      rows = all("#edit-dialog-form .grid.grid-cols-12")
      within rows[0] do
        find("button.col-span-1").click
      end
      await_debounce

      expect(page).not_to have_css('button[data-test-id="update.0.value"]')
      expect(page).not_to have_css('button[data-test-id="update.1.value"]')
    end
  end
end
