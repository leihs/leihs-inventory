require "features_helper"
require_relative "../shared/common"

feature "Duplicate Item from Inventory List", type: :feature do
  let(:user) { FactoryBot.create(:user, language_locale: "en-GB") }
  let(:pool) { FactoryBot.create(:inventory_pool) }
  let(:model) { FactoryBot.create(:leihs_model) }
  let(:building) { FactoryBot.create(:building, name: "Test Building", code: "TB") }
  let(:room) { FactoryBot.create(:room, name: "Test Room", building: building) }
  let(:supplier) { FactoryBot.create(:supplier) }

  # Original item data
  let(:original_serial_number) { Faker::Barcode.isbn }
  let(:original_name) { Faker::Device.model_name }
  let(:original_note) { Faker::Lorem.paragraph }
  let(:original_shelf) { Faker::Alphanumeric.alpha(number: 3).upcase + "-" + Faker::Number.number(digits: 2).to_s }
  let(:original_price) do
    base_price = Faker::Commerce.price(range: 100..5000).floor
    decimals = [*1..9].sample(2).join # Ensures two different non-zero digits
    format("%.2f", "#{base_price}.#{decimals}".to_f)
  end

  # New serial number for duplicated item
  let(:new_serial_number) { Faker::Barcode.isbn }

  before(:each) do
    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)

    # Create original item with various fields populated
    @original_item = FactoryBot.create(:item,
      leihs_model: model,
      inventory_pool: pool,
      owner: pool,
      serial_number: original_serial_number,
      name: original_name,
      note: original_note,
      shelf: original_shelf,
      room: room,
      supplier: supplier,
      price: original_price,
      is_borrowable: true,
      is_broken: false,
      is_incomplete: false)
  end

  scenario "duplicates item with correct field copying" do
    login(user)
    visit "/inventory/#{pool.id}/list"

    # Find and expand the model row
    find("input[name='search']").set(model.product)
    await_debounce

    within find('[data-row="model"]', text: model.product) do
      click_on "expand-button"
    end

    # Find the original item row and open dropdown
    within find('[data-row="item"]', text: @original_item.inventory_code) do
      find('[data-test-id="edit-dropdown"]').click
    end

    # Click "Copy item" link
    click_on "Duplicate item"

    # Verify navigation to create page with fromItem param
    expect(page).to have_current_path(/\/items\/create\?fromItem=#{@original_item.id}/)
    expect(page).to have_content("Create New Item")

    # Verify fields that ARE copied from original
    expect(find('button[data-test-id="model_id"]')).to have_text(model.product)
    expect(find('button[data-test-id="building_id"]')).to have_text(building.name)
    expect(find('button[data-test-id="room_id"]')).to have_text(room.name)
    assert_field("Name", original_name)
    assert_field("Note", original_note)
    assert_field("Shelf", original_shelf)
    assert_field("Initial Price", original_price)
    # expect(find('button[data-test-id="supplier_id"]')).to have_text(supplier.name)
    assert_checked(find('button[data-test-id="is_borrowable-true"]'))

    # Verify fields that are NOT copied (empty/default)
    assert_field("Serial Number", "")   # Empty - explicitly excluded from copy

    # Fill in NEW serial number for duplicated item
    fill_in "Serial Number", with: new_serial_number
    click_on "Running Account"

    # Submit form to create duplicated item
    click_on "Create"

    # Verify success and redirect
    expect(page).to have_text("Item was successfully created")
    expect(page).to have_content("Inventory List")

    # Navigate back to verify both items exist
    find("input[name='search']").set(model.product)
    await_debounce

    within find('[data-row="model"]', text: model.product) do
      click_on "expand-button"
    end

    # Should now see TWO items under the model
    item_rows = all('[data-row="item"]')
    expect(item_rows.count).to eq(2)

    # Find the duplicated item row (the one that's NOT the original)
    duplicated_item_row = item_rows.find do |row|
      !row.text.include?(@original_item.inventory_code)
    end

    # Extract the new inventory code from the duplicated row
    within duplicated_item_row do
      @duplicated_inventory_code = begin
        find('[data-test-id="inventory-code"]').text
      rescue
        nil
      end
      # Alternative: extract from row text if no specific test-id exists
      @duplicated_inventory_code ||= duplicated_item_row.text.match(/[A-Z]+\d+/).to_s
    end

    # Verify the duplicated item has a DIFFERENT inventory code
    expect(@duplicated_inventory_code).not_to eq(@original_item.inventory_code)
    expect(@duplicated_inventory_code).not_to be_empty

    # Click edit on duplicated item to verify all saved fields
    within duplicated_item_row do
      click_on "edit"
    end

    # Verify duplicated item has NEW inventory_code (auto-generated, different from original)
    expect(find_field("Inventory Code").value).to eq(@duplicated_inventory_code)
    expect(find_field("Inventory Code").value).not_to eq(@original_item.inventory_code)

    # Verify duplicated item has NEW serial_number (the one we entered)
    assert_field("Serial Number", new_serial_number)

    # Verify all other fields were COPIED correctly from original
    expect(find('button[data-test-id="model_id"]')).to have_text(model.product)
    assert_field("Name", original_name)
    assert_field("Note", original_note)
    assert_field("Shelf", original_shelf)
    expect(find('button[data-test-id="building_id"]')).to have_text(building.name)
    expect(find('button[data-test-id="room_id"]')).to have_text(room.name)
    assert_field("Initial Price", original_price)
    # expect(find('button[data-test-id="supplier_id"]')).to have_text(supplier.name)
    assert_checked(find('button[data-test-id="is_borrowable-true"]'))
  end
end
