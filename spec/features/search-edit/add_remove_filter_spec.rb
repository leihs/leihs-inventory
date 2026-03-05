require "features_helper"
require_relative "../shared/common"

feature "Search & Edit - Filters", type: :feature do
  let(:user) { FactoryBot.create(:user, language_locale: "en-GB") }
  let(:pool) { FactoryBot.create(:inventory_pool) }
  let!(:model) { FactoryBot.create(:leihs_model) }
  let!(:item) do
    FactoryBot.create(:item,
      inventory_code: "FILTER-#{Faker::Alphanumeric.alpha(number: 6).upcase}",
      leihs_model: model,
      inventory_pool: pool,
      owner: pool)
  end

  before(:each) do
    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)
  end

  def visit_search_edit
    login(user)
    visit "/inventory/#{pool.id}/search-edit"
  end

  def add_first_filter_group
    click_on "Add filters to start your search."
    expect(page).to have_button("Add search parameter")
  end

  def open_edit_dialog
    visit_search_edit
    add_first_filter_group
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

  context "search filter panel" do
    scenario "adding filters to start shows first filter row" do
      visit_search_edit
      add_first_filter_group

      expect(page).to have_css('[data-test-id="or-0-field-select-0"]')
    end

    scenario "can add a second AND condition within the same OR group" do
      visit_search_edit
      add_first_filter_group

      click_on "Add search parameter"
      await_debounce

      expect(page).to have_css('[data-test-id="or-0-field-select-1"]')
    end

    scenario "can remove an AND condition row" do
      visit_search_edit
      add_first_filter_group

      click_on "Add search parameter"
      await_debounce
      expect(page).to have_css('[data-test-id="or-0-field-select-1"]')

      click_on "remove-and-1"
      await_debounce

      expect(page).not_to have_css('[data-test-id="or-0-field-select-1"]')
      expect(page).to have_css('[data-test-id="or-0-field-select-0"]')
    end

    scenario "can add a second OR group" do
      visit_search_edit
      add_first_filter_group

      find('[data-test-id="add-or-filter-btn"]').click
      await_debounce

      expect(page).to have_css('[data-test-id="or-1-field-select-0"]')
    end

    scenario "can remove an OR group" do
      visit_search_edit
      add_first_filter_group

      find('[data-test-id="add-or-filter-btn"]').click
      await_debounce
      expect(page).to have_css('[data-test-id="or-1-field-select-0"]')

      find('[data-test-id="remove-or-btn-1"]').click
      await_debounce

      expect(page).not_to have_css('[data-test-id="or-1-field-select-0"]')
      expect(page).to have_css('[data-test-id="or-0-field-select-0"]')
    end
  end

  context "edit dialog" do
    scenario "shows first field row on open" do
      open_edit_dialog

      expect(page).to have_css('[data-test-id="field-select-0"]')
    end

    scenario "can add a second field row" do
      open_edit_dialog

      within find('[id="edit-dialog-form"]') do
        click_on "field-select-0"
      end
      within find('[data-test-id="field-options"]') do
        click_on "Status note"
      end

      find("textarea[name='update.0.value']")

      within find('[id="edit-dialog-form"]') do
        find('[data-test-id="field-select-0"]')
        click_on "Add field"
      end
      await_debounce

      expect(page).to have_css('[data-test-id="field-select-1"]')
    end

    scenario "can remove a field row" do
      open_edit_dialog

      within find('[id="edit-dialog-form"]') do
        click_on "field-select-0"
      end
      within find('[data-test-id="field-options"]') do
        click_on "Status note"
      end

      within find('[id="edit-dialog-form"]') do
        click_on "Add field"
      end
      await_debounce
      expect(page).to have_css('[data-test-id="field-select-1"]')

      # Remove the first row
      rows = all("#edit-dialog-form .grid.grid-cols-12")
      within rows[0] do
        find("button.col-span-1").click
      end
      await_debounce

      expect(page).not_to have_css('[data-test-id="field-select-1"]')
      expect(page).to have_css('[data-test-id="field-select-0"]')
    end
  end
end
