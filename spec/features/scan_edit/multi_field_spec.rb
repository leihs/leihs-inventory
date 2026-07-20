require "features_helper"
require_relative "../shared/common"
require_relative "../shared/patch_form_helpers"

feature "Scan & Edit - multiple fields", type: :feature do
  let(:user) { FactoryBot.create(:user, language_locale: "en-GB") }
  let(:pool) { FactoryBot.create(:inventory_pool) }
  let!(:model) { FactoryBot.create(:leihs_model) }
  let!(:item) do
    FactoryBot.create(:item,
      inventory_code: "SCAN-#{Faker::Alphanumeric.alpha(number: 6).upcase}",
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

  scenario "setting multiple fields of different types in one scan persists them all" do
    supplier = FactoryBot.create(:supplier)
    building = FactoryBot.create(:building)
    room = FactoryBot.create(:room, building: building)
    serial = "SN-#{SecureRandom.hex(4).upcase}"
    date = Date.today

    login(user)
    visit "/inventory/#{pool.id}/scan-edit"

    idx = 0

    add_patch_field(idx, "Serial Number")
    fill_patch_text(idx, serial)
    idx += 1

    add_patch_field(idx, "Status note")
    fill_patch_text(idx, "multi-field scan note")
    idx += 1

    add_patch_field(idx, "Working order")
    click_patch_radio(idx, "true")
    idx += 1

    add_patch_field(idx, "Borrowable")
    click_patch_radio(idx, "false")
    idx += 1

    add_patch_field(idx, "Relevant for inventory")
    fill_patch_select(idx, "Yes")
    idx += 1

    add_patch_field(idx, "Last Checked")
    fill_patch_calendar(idx, date)
    idx += 1

    add_patch_field(idx, "Initial Price")
    fill_patch_price(idx, "250.00")
    idx += 1

    add_patch_field(idx, "Supplier")
    fill_patch_autocomplete(idx, supplier.name[0..4], supplier.name)
    idx += 1

    add_patch_field(idx, "Building")
    fill_patch_autocomplete(idx, building.name[0..4], building.name)
    idx += 1

    expect(page).to have_content("Room", wait: 5)
    fill_patch_autocomplete(idx, room.name[0..4], room.name)
    idx += 1

    add_patch_field(idx, "Shelf")
    fill_patch_text(idx, "C-03")

    find('[data-test-id="barcode-input"]').set(item.inventory_code)
    find('[data-test-id="barcode-input"]').send_keys(:return)

    expect(page).to have_text("Item has been successfully updated")

    item.reload
    expect(item.serial_number).to eq(serial)
    expect(item.status_note).to eq("multi-field scan note")
    expect(item.is_broken).to be(true)
    expect(item.is_borrowable).to be(false)
    expect(item.is_inventory_relevant).to be(true)
    expect(item.last_check).to eq(date)
    expect(item.price.to_f).to eq(250.0)
    expect(item.supplier_id).to eq(supplier.id)
    expect(item.room_id).to eq(room.id)
    expect(item.shelf).to eq("C-03")
  end
end
