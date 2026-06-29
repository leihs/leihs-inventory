require "features_helper"
require_relative "../shared/common"

feature "Scan & Edit", type: :feature do
  let(:user) { FactoryBot.create(:user, language_locale: "en-GB") }
  let(:pool) { FactoryBot.create(:inventory_pool) }
  let(:other_pool) { FactoryBot.create(:inventory_pool) }
  let!(:model) { FactoryBot.create(:leihs_model) }

  let!(:item) do
    FactoryBot.create(:item,
      inventory_code: "SCAN-#{Faker::Alphanumeric.alpha(number: 6).upcase}",
      leihs_model: model,
      inventory_pool: pool,
      owner: pool)
  end

  # Item in a different pool — not owned by `pool`
  let!(:foreign_item) do
    FactoryBot.create(:item,
      inventory_code: "FOREIGN-#{Faker::Alphanumeric.alpha(number: 6).upcase}",
      leihs_model: model,
      inventory_pool: other_pool,
      owner: other_pool)
  end

  before(:each) do
    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)
  end

  def visit_scan_edit
    login(user)
    visit "/inventory/#{pool.id}/scan-edit"
  end

  def set_status_note(value)
    click_on "field-select-0"
    within find('[data-test-id="field-options"]') do
      find("button", text: "Status note").click
    end
    find("textarea[name='update.0.value']").set(value)
  end

  scenario "selecting an item from the autocomplete and clicking Apply shows a success toast" do
    visit_scan_edit

    set_status_note(Faker::Lorem.sentence)

    # Open the autocomplete and search for the item
    find('[data-test-id="item"]').click
    find('[data-test-id="item-input"]').set(item.inventory_code[0..3])
    await_debounce

    within find('[role="listbox"]') do
      find("button", text: item.inventory_code).click
    end

    # find('button[form="patch-item-form"]', text: "Apply").click
    # workaround to mitigate timing capybara and react timing issue for disabled fields, the above would not work
    page.execute_script("document.querySelector('button[form=\"patch-item-form\"]').click()")

    expect(find('[data-test-id="item"]')).to be_disabled
    expect(find('[data-test-id="barcode-input"]')).to be_disabled

    expect(page).to have_text("Item has been successfully updated")
  end

  scenario "scanning a barcode and pressing Enter shows a success toast" do
    visit_scan_edit

    set_status_note(Faker::Lorem.sentence)

    find('[data-test-id="barcode-input"]').set(item.inventory_code)
    find('[data-test-id="barcode-input"]').send_keys(:return)

    expect(find('[data-test-id="item"]')).to be_disabled
    expect(find('[data-test-id="barcode-input"]')).to be_disabled

    expect(page).to have_text("Item has been successfully updated")
  end

  scenario "an item from a different pool does not appear in the autocomplete dropdown" do
    visit_scan_edit

    find('[data-test-id="item"]').click
    find('[data-test-id="item-input"]').set(foreign_item.inventory_code[0..5])
    await_debounce

    within find('[role="listbox"]') do
      expect(page).not_to have_text(foreign_item.inventory_code)
      expect(page).to have_text("No results found for this search term")
    end
  end

  scenario "scanning a barcode from a different pool shows an error toast" do
    visit_scan_edit

    set_status_note(Faker::Lorem.sentence)

    find('[data-test-id="barcode-input"]').set(foreign_item.inventory_code)
    find('[data-test-id="barcode-input"]').send_keys(:return)

    expect(page).to have_text("Item cannot be updated")
  end
end
