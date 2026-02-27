require "features_helper"
require_relative "../shared/common"

feature "Create package", type: :feature do
  let(:user) { FactoryBot.create(:user, language_locale: "en-GB") }
  let(:pool) { FactoryBot.create(:inventory_pool) }
  let!(:package_model) { FactoryBot.create(:package_model, product: "Test Package Model") }
  let!(:regular_model) { FactoryBot.create(:leihs_model, product: "Regular Item Model") }

  # Building and room for items
  let!(:building) { FactoryBot.create(:building, name: "Test Building") }
  let!(:room) { FactoryBot.create(:room, name: "Test Room", building: building) }

  # 2 items to add to the package
  let!(:item1) do
    FactoryBot.create(:item,
      inventory_code: "ITEM-001",
      leihs_model: regular_model,
      inventory_pool: pool,
      owner: pool,
      room: room)
  end

  let!(:item2) do
    FactoryBot.create(:item,
      inventory_code: "ITEM-002",
      leihs_model: regular_model,
      inventory_pool: pool,
      owner: pool,
      room: room)
  end

  let(:inventory_code) { "PKG-" + Faker::Barcode.ean }
  let(:name) { Faker::Device.model_name }
  let(:note) { Faker::Lorem.paragraph }
  let(:price) do
    base_price = Faker::Commerce.price(range: 100..5000).floor
    decimals = [*1..9].sample(2).join
    format("%.2f", "#{base_price}.#{decimals}".to_f)
  end
  let(:user_name) { Faker::Name.name }
  let(:reason_for_retirement) { Faker::Lorem.sentence }
  let(:typical_usage) { Faker::Lorem.sentence }
  let(:project_number) { Faker::Number.number(digits: 5).to_s }
  let(:status_note) { Faker::Lorem.sentence }
  let(:shelf) { Faker::Alphanumeric.alpha(number: 3).upcase + "-" + Faker::Number.number(digits: 2).to_s }

  before(:each) do
    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)
  end

  scenario "works" do
    login(user)
    visit "/inventory/#{pool.id}/list"
    click_on "Add inventory"
    click_on "New package"

    fill_in "Inventory Code", with: inventory_code

    click_on "model_id"
    expect(page).to have_field(placeholder: "Enter search term")
    fill_in "model_id-input", with: package_model.product
    expect(page).to have_content package_model.product
    click_on package_model.product

    # Select items (package-specific functionality)
    click_on "Select items"
    expect(page).to have_field("items-input", wait: 10)

    # Search for item 1 by inventory code
    fill_in "items-input", with: "ITEM-001"
    await_debounce
    expect(page).to have_content "ITEM-001"
    click_on "ITEM-001"

    # Search for item 2 by model name (auto-clears)
    click_on "Select items"
    expect(page).to have_field("items-input", wait: 10)
    fill_in "items-input", with: "Regular Item Model"
    await_debounce
    expect(page).to have_content "ITEM-002 - Regular Item Model"
    fill_in "items-input", with: "ITEM-002"
    await_debounce
    expect(page).to have_content "ITEM-002 - Regular Item Model"
    click_on "ITEM-002"

    expect(page).not_to have_content "Reason for Retirement"
    click_on "Retirement"
    expect(page).to have_content "Yes"
    click_on "Yes"

    fill_in "Reason for Retirement", with: reason_for_retirement

    click_on "is_broken-true"
    click_on "is_incomplete-true"
    click_on "is_borrowable-true"

    fill_in "Status note", with: status_note

    click_on "Relevant for inventory"
    expect(page).to have_content "No"
    click_on "No"

    click_on "Last Checked"

    yesterday = Date.today - 1
    click_calendar_day(yesterday)

    fill_in "Responsible person", with: user_name
    fill_in "User/Typical usage", with: typical_usage

    fill_in "Note", with: note

    expect(page).not_to have_content "Room"

    click_on "building_id"
    expect(page).to have_field(placeholder: "Enter search term")
    click_on "general building"

    expect(page).to have_content "Room"

    click_on "Room"
    expect(page).to have_field(placeholder: "Enter search term")
    click_on "general room"

    fill_in "Shelf", with: shelf

    fill_in "Initial Price", with: price

    click_on "Create"
    expect(page).to have_text "Package was successfully created"
    expect(page).to have_content "Inventory List"

    fill_in "search", with: package_model.product
    await_debounce

    within find('[data-row="model"]', text: package_model.product) do
      click_on "expand-button"
    end

    within find('[data-row="package"]', text: inventory_code) do
      click_on "edit"
    end

    assert_field "Inventory Code", inventory_code
    expect(find('button[data-test-id="model_id"]')).to have_text(package_model.product)

    # Verify both items are in the package
    expect(page).to have_content "ITEM-001"
    expect(page).to have_content "ITEM-002"

    expect(find('button[name="retired"]')).to have_text("Yes")
    assert_field "Reason for Retirement", reason_for_retirement

    assert_checked(find('button[data-test-id="is_broken-true"]'))
    assert_checked(find('button[data-test-id="is_incomplete-true"]'))
    assert_checked(find('button[data-test-id="is_borrowable-true"]'))

    assert_field "Status note", status_note

    expect(find('button[name="is_inventory_relevant"]')).to have_text("No")

    expect(find('button[data-test-id="owner_id"]')).to have_text(pool.name)
    expect(find('button[name="last_check"]')).to have_text(yesterday.strftime("%Y-%m-%d"))

    assert_field "Responsible person", user_name
    assert_field "User/Typical usage", typical_usage

    assert_field "Note", note

    expect(find('button[data-test-id="building_id"]')).to have_text("general building")
    expect(find('button[data-test-id="room_id"]')).to have_text("general room")

    assert_field "Shelf", shelf

    assert_field "Initial Price", price
  end

  scenario "fails with invalid mandatory fields" do
    login(user)
    visit "/inventory/#{pool.id}/list"
    click_on "Add inventory"
    click_on "New package"

    click_on "Create"

    expect(page).to have_text "Package could not be created because 3 fields are invalid"

    click_on "model_id"
    expect(page).to have_field(placeholder: "Enter search term")
    fill_in "model_id-input", with: package_model.product
    expect(page).to have_content package_model.product
    click_on package_model.product

    click_on "building_id"
    expect(page).to have_field(placeholder: "Enter search term")
    click_on "general building"

    click_on "Create"
    expect(page).to have_text "Package could not be created because 2 fields are invalid"

    click_on "Room"
    expect(page).to have_field(placeholder: "Enter search term")
    click_on "general room"

    click_on "Create"
    expect(page).to have_text "Package could not be created because one field is invalid"

    # Select one item
    click_on "Select items"
    expect(page).to have_field("items-input", wait: 10)
    fill_in "items-input", with: "ITEM-001"
    await_debounce
    expect(page).to have_content "ITEM-001"
    click_on "ITEM-001"

    click_on "Create"
    expect(page).to have_text "Package was successfully created"
  end

  scenario "fails with conflicting inventory code" do
    FactoryBot.create(:item,
      inventory_code: inventory_code,
      inventory_pool: pool,
      owner: pool,
      leihs_model: package_model,
      room: room)

    login(user)
    visit "/inventory/#{pool.id}/list"
    click_on "Add inventory"
    click_on "New package"

    fill_in "Inventory Code", with: inventory_code

    click_on "model_id"
    expect(page).to have_field(placeholder: "Enter search term")
    fill_in "model_id-input", with: package_model.product
    expect(page).to have_content package_model.product
    click_on package_model.product

    # Select one item
    click_on "Select items"
    expect(page).to have_field("items-input", wait: 10)
    fill_in "items-input", with: "ITEM-001"
    await_debounce
    expect(page).to have_content "ITEM-001"
    click_on "ITEM-001"

    click_on "building_id"
    expect(page).to have_field(placeholder: "Enter search term")
    click_on "general building"

    click_on "Room"
    expect(page).to have_field(placeholder: "Enter search term")
    click_on "general room"

    click_on "Create"

    expect(page).to have_text "Inventory code already exists"
    click_on "Update"

    click_on "Create"

    expect(page).to have_text "Package was successfully created"
  end

  scenario "cancel works" do
    login(user)
    visit "/inventory/#{pool.id}/list"
    click_on "Add inventory"
    click_on "New package"

    click_on "model_id"
    expect(page).to have_field(placeholder: "Enter search term")
    fill_in "model_id-input", with: package_model.product
    expect(page).to have_content package_model.product
    click_on package_model.product

    click_on "submit-dropdown"
    click_on "Cancel"

    expect(page).to have_content "Inventory List"
  end
end
