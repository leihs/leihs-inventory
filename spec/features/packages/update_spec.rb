require "features_helper"
require_relative "../shared/common"

feature "Update package", type: :feature do
  let(:user) { FactoryBot.create(:user, language_locale: "en-GB") }
  let(:pool) { FactoryBot.create(:inventory_pool) }

  let(:inventory_code_old) { "PKG-" + Faker::Barcode.ean }
  let(:inventory_code_new) { "PKG-" + Faker::Barcode.ean }

  let(:note_old) { Faker::Lorem.paragraph }
  let(:note_new) { Faker::Lorem.paragraph }

  let(:price_old) do
    base_price = Faker::Commerce.price(range: 100..5000).floor
    decimals = [*1..9].sample(2).join
    format("%.2f", "#{base_price}.#{decimals}".to_f)
  end
  let(:price_new) do
    base_price = Faker::Commerce.price(range: 100..5000).floor
    decimals = [*1..9].sample(2).join
    format("%.2f", "#{base_price}.#{decimals}".to_f)
  end

  let(:user_name_old) { Faker::Name.name }
  let(:user_name_new) { Faker::Name.name }

  let(:reason_for_retirement_old) { Faker::Lorem.sentence }
  let(:reason_for_retirement_new) { Faker::Lorem.sentence }

  let(:typical_usage_old) { Faker::Lorem.sentence }
  let(:typical_usage_new) { Faker::Lorem.sentence }

  let(:status_note_old) { Faker::Lorem.sentence }
  let(:status_note_new) { Faker::Lorem.sentence }

  let(:shelf_old) { Faker::Alphanumeric.alpha(number: 3).upcase + "-" + Faker::Number.number(digits: 2).to_s }
  let(:shelf_new) { Faker::Alphanumeric.alpha(number: 3).upcase + "-" + Faker::Number.number(digits: 2).to_s }

  let!(:package_model_old) { FactoryBot.create(:package_model, product: "Old Package Model") }
  let!(:package_model_new) { FactoryBot.create(:package_model, product: "New Package Model") }
  let!(:regular_model) { FactoryBot.create(:leihs_model, product: "Regular Item Model") }
  let!(:other_pool) { FactoryBot.create(:inventory_pool) }

  # Buildings and rooms
  let!(:building_old) { FactoryBot.create(:building, name: "Old Building") }
  let!(:room_old) { FactoryBot.create(:room, name: "Old Room", building: building_old) }
  let!(:building_new) { FactoryBot.create(:building, name: "New Building") }
  let!(:room_new) { FactoryBot.create(:room, name: "New Room", building: building_new) }

  # Items for package content
  let!(:item1_old) do
    FactoryBot.create(:item,
      inventory_code: "ITEM-001-OLD",
      leihs_model: regular_model,
      inventory_pool: pool,
      owner: pool,
      room: room_old)
  end

  let!(:item2_old) do
    FactoryBot.create(:item,
      inventory_code: "ITEM-002-OLD",
      leihs_model: regular_model,
      inventory_pool: pool,
      owner: pool,
      room: room_old)
  end

  let!(:item3_new) do
    FactoryBot.create(:item,
      inventory_code: "ITEM-003-NEW",
      leihs_model: regular_model,
      inventory_pool: pool,
      owner: pool,
      room: room_new)
  end

  before(:each) do
    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)
  end

  scenario "works" do
    yesterday = Date.today - 1

    @package = FactoryBot.create(:item,
      inventory_code: inventory_code_old,
      leihs_model: package_model_old,
      inventory_pool: pool,
      owner: pool,
      properties: {
        price: price_old
      },
      note: note_old,
      retired: yesterday,
      retired_reason: reason_for_retirement_old,
      is_broken: true,
      is_incomplete: true,
      is_borrowable: true,
      status_note: status_note_old,
      is_inventory_relevant: false,
      last_check: yesterday,
      responsible: user_name_old,
      user_name: user_name_old,
      room: room_old,
      shelf: shelf_old)

    # Assign initial items to package
    item1_old.update(parent_id: @package.id)
    item2_old.update(parent_id: @package.id)

    login(user)
    visit "/inventory/#{pool.id}/list"

    fill_in "search", with: package_model_old.product
    await_debounce

    within find('[data-row="model"]', text: package_model_old.product) do
      click_on "expand-button"
    end

    within find('[data-row="package"]', text: inventory_code_old) do
      click_on "edit"
    end

    fill_in "Inventory Code", with: inventory_code_new

    click_on "model_id"
    expect(page).to have_field(placeholder: "Enter search term")
    fill_in "model_id-input", with: package_model_new.product
    expect(page).to have_content package_model_new.product
    click_on package_model_new.product

    # Update package content: remove item2_old, add item3_new
    # First remove item2_old
    within find("tr", text: "ITEM-002-OLD") do
      find('button[type="button"]', text: "", match: :prefer_exact).click
    end

    expect(page).not_to have_content "ITEM-002-OLD"

    # Add item3_new
    click_on "Select items"
    expect(page).to have_field("items-input", wait: 10)
    fill_in "items-input", with: "ITEM-003-NEW"
    await_debounce
    expect(page).to have_content "ITEM-003-NEW"
    click_on "ITEM-003-NEW"

    fill_in "Note", with: note_new

    click_on "Retirement"
    expect(page).to have_content "No"
    click_on "No"

    expect(page).not_to have_content "Reason for Retirement*"

    click_on "is_broken-false"
    click_on "is_incomplete-false"
    click_on "is_borrowable-false"

    fill_in "Status note", with: status_note_new

    click_on "Relevant for inventory"
    expect(page).to have_content "Yes"
    click_on "Yes"

    click_on "Last Checked"

    today = Date.today
    find("[data-day='#{today.strftime("%-m/%-d/%Y")}']").click

    fill_in "Responsible person", with: user_name_new
    fill_in "User/Typical usage", with: typical_usage_new

    click_on "building_id"
    expect(page).to have_field(placeholder: "Enter search term")
    fill_in "building_id-input", with: building_new.name
    expect(page).to have_content building_new.name
    click_on building_new.name

    expect(page).to have_content "Room"

    click_on "Room"
    expect(page).to have_field(placeholder: "Enter search term")
    fill_in "room_id-input", with: room_new.name
    expect(page).to have_content room_new.name
    click_on room_new.name

    fill_in "Shelf", with: shelf_new

    fill_in "Initial Price", with: price_new

    click_on "Save"
    expect(page).to have_text("Package was successfully saved")
    expect(page).to have_text("Inventory List")

    fill_in "search", with: package_model_new.product
    await_debounce

    within find('[data-row="model"]', text: package_model_new.product) do
      click_on "expand-button"
    end

    within find('[data-row="package"]', text: inventory_code_new) do
      click_on "edit"
    end

    assert_field "Inventory Code", inventory_code_new
    expect(find('button[data-test-id="model_id"]')).to have_text(package_model_new.product)

    # Verify package content: should have item1_old and item3_new
    expect(page).to have_content "ITEM-001-OLD"
    expect(page).not_to have_content "ITEM-002-OLD"
    expect(page).to have_content "ITEM-003-NEW"

    expect(find('button[name="retired"]')).to have_text("No")

    assert_unchecked(find('button[data-test-id="is_broken-true"]'))
    assert_unchecked(find('button[data-test-id="is_incomplete-true"]'))
    assert_unchecked(find('button[data-test-id="is_borrowable-true"]'))

    assert_field "Status note", status_note_new

    expect(find('button[name="is_inventory_relevant"]')).to have_text("Yes")

    expect(find('button[data-test-id="owner_id"]')).to have_text(pool.name)
    expect(find('button[name="last_check"]')).to have_text(today.strftime("%Y-%m-%d"))

    assert_field "Responsible person", user_name_new
    assert_field "User/Typical usage", typical_usage_new

    assert_field "Note", note_new

    expect(find('button[data-test-id="building_id"]')).to have_text(building_new.name)
    expect(find('button[data-test-id="room_id"]')).to have_text(room_new.name)

    assert_field "Shelf", shelf_new

    assert_field "Initial Price", price_new
  end

  scenario "removing all items shows toast and auto-sets retirement" do
    @package = FactoryBot.create(:item,
      inventory_code: inventory_code_old,
      leihs_model: package_model_old,
      inventory_pool: pool,
      owner: pool,
      room: room_old)

    # Assign items to package
    item1_old.update(parent_id: @package.id)
    item2_old.update(parent_id: @package.id)

    login(user)
    visit "/inventory/#{pool.id}/list"

    fill_in "search", with: package_model_old.product
    await_debounce

    within find('[data-row="model"]', text: package_model_old.product) do
      click_on "expand-button"
    end

    within find('[data-row="package"]', text: inventory_code_old) do
      click_on "edit"
    end

    # Verify package starts with items and is not retired
    expect(page).to have_content "ITEM-001-OLD"
    expect(page).to have_content "ITEM-002-OLD"
    expect(find('button[name="retired"]')).to have_text("No")

    # Remove all items from package
    within find("tr", text: "ITEM-001-OLD") do
      find('button[type="button"]', text: "", match: :prefer_exact).click
    end

    within find("tr", text: "ITEM-002-OLD") do
      find('button[type="button"]', text: "", match: :prefer_exact).click
    end

    expect(page).not_to have_content "ITEM-001-OLD"
    expect(page).not_to have_content "ITEM-002-OLD"

    # Verify toast appears with expected message
    expect(page).to have_text("All items have been removed from this package. Retired and retire reason have been automatically set.")

    # Verify retirement fields are auto-populated
    expect(find('button[name="retired"]')).to have_text("Yes")
    assert_field "Reason for Retirement", "Package was automatically retired because it has no items assigned"

    click_on "Save"
    expect(page).to have_text("Package was successfully saved")
    expect(page).to have_text("Inventory List")

    # Verify persistence
    fill_in "search", with: package_model_old.product
    await_debounce

    within find('[data-row="model"]', text: package_model_old.product) do
      click_on "expand-button"
    end

    within find('[data-row="package"]', text: inventory_code_old) do
      click_on "edit"
    end

    expect(find('button[name="retired"]')).to have_text("Yes")
    assert_field "Reason for Retirement", "Package was automatically retired because it has no items assigned"
    expect(page).not_to have_content "ITEM-001-OLD"
    expect(page).not_to have_content "ITEM-002-OLD"
  end

  scenario "removing all items preserves existing retired_reason if present" do
    @package = FactoryBot.create(:item,
      inventory_code: inventory_code_old,
      leihs_model: package_model_old,
      inventory_pool: pool,
      owner: pool,
      room: room_old)

    # Assign items to package
    item1_old.update(parent_id: @package.id)
    item2_old.update(parent_id: @package.id)

    login(user)
    visit "/inventory/#{pool.id}/list"

    fill_in "search", with: package_model_old.product
    await_debounce

    within find('[data-row="model"]', text: package_model_old.product) do
      click_on "expand-button"
    end

    within find('[data-row="package"]', text: inventory_code_old) do
      click_on "edit"
    end

    # Set custom retirement reason first
    click_on "Retirement"
    expect(page).to have_content "Yes"
    click_on "Yes"

    custom_reason = "My custom retirement reason"
    fill_in "Reason for Retirement", with: custom_reason

    # Now remove all items
    within find("tr", text: "ITEM-001-OLD") do
      find('button[type="button"]', text: "", match: :prefer_exact).click
    end

    within find("tr", text: "ITEM-002-OLD") do
      find('button[type="button"]', text: "", match: :prefer_exact).click
    end

    # Verify toast appears
    expect(page).to have_text("All items have been removed from this package. Retired and retire reason have been automatically set.")

    # Verify retirement is still Yes
    expect(find('button[name="retired"]')).to have_text("Yes")

    # Verify custom reason is preserved (NOT overwritten with auto-generated text)
    assert_field "Reason for Retirement", custom_reason

    click_on "Save"
    expect(page).to have_text("Package was successfully saved")
    expect(page).to have_text("Inventory List")

    # Verify custom reason persisted
    fill_in "search", with: package_model_old.product
    await_debounce

    within find('[data-row="model"]', text: package_model_old.product) do
      click_on "expand-button"
    end

    within find('[data-row="package"]', text: inventory_code_old) do
      click_on "edit"
    end

    expect(find('button[name="retired"]')).to have_text("Yes")
    assert_field "Reason for Retirement", custom_reason
  end

  scenario "protected fields are disabled when not owner" do
    @package = FactoryBot.create(:item,
      inventory_code: inventory_code_old,
      leihs_model: package_model_old,
      inventory_pool: pool,
      owner: other_pool,
      room: room_old)

    login(user)
    visit "/inventory/#{pool.id}/list"

    fill_in "search", with: package_model_old.product
    await_debounce

    within find('[data-row="model"]', text: package_model_old.product) do
      click_on "expand-button"
    end

    within find('[data-row="package"]', text: inventory_code_old) do
      click_on "edit"
    end

    inventory_code_field = find_field("Inventory Code", disabled: true)
    expect(inventory_code_field).to be_disabled

    click_on "inventory_code-disabled-info"
    expect(page).to have_content("Only the owner can edit this field")

    click_on "Save"
    expect(page).to have_text("Package was successfully saved")
    expect(page).to have_text("Inventory List")
  end
end
