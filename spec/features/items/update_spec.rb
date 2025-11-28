require "spec_helper"
require_relative "../shared/common"

feature "Update item", type: :feature do
  let(:user) { FactoryBot.create(:user, language_locale: "en-GB") }
  let(:pool) { FactoryBot.create(:inventory_pool) }

  let(:inventory_code_old) { Faker::Barcode.ean }
  let(:inventory_code_new) { Faker::Barcode.ean }

  let(:serial_number_old) { Faker::Barcode.isbn }
  let(:serial_number_new) { Faker::Barcode.isbn }

  let(:mac_address_old) { Faker::Internet.mac_address }
  let(:mac_address_new) { Faker::Internet.mac_address }

  let(:imei_number_old) { Faker::Number.number(digits: 15).to_s }
  let(:imei_number_new) { Faker::Number.number(digits: 15).to_s }

  let(:name_old) { Faker::Device.model_name }
  let(:name_new) { Faker::Device.model_name }

  let(:note_old) { Faker::Lorem.paragraph }
  let(:note_new) { Faker::Lorem.paragraph }

  let(:invoice_number_old) { Faker::Number.number(digits: 10).to_s }
  let(:invoice_number_new) { Faker::Number.number(digits: 10).to_s }

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

  let(:project_number_old) { Faker::Number.number(digits: 5).to_s }
  let(:project_number_new) { Faker::Number.number(digits: 5).to_s }

  let(:status_note_old) { Faker::Lorem.sentence }
  let(:status_note_new) { Faker::Lorem.sentence }

  let(:shelf_old) { Faker::Alphanumeric.alpha(number: 3).upcase + "-" + Faker::Number.number(digits: 2).to_s }
  let(:shelf_new) { Faker::Alphanumeric.alpha(number: 3).upcase + "-" + Faker::Number.number(digits: 2).to_s }

  let(:attachment_name_old) { "secd.pdf" }
  let(:attachment_name_new) { "shenpaper.pdf" }

  let!(:model_old) { FactoryBot.create(:leihs_model) }
  let!(:model_new) { FactoryBot.create(:leihs_model) }
  let!(:supplier_old) { FactoryBot.create(:supplier) }
  let!(:supplier_new) { FactoryBot.create(:supplier) }
  let!(:other_pool) { FactoryBot.create(:inventory_pool) }

  before(:each) do
    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)
  end

  scenario "works" do
    yesterday = Date.today - 1

    @item = FactoryBot.create(:item,
      inventory_code: inventory_code_old,
      leihs_model: model_old,
      inventory_pool: pool,
      owner: pool,
      serial_number: serial_number_old,
      properties: {
        mac_address: mac_address_old,
        imei_number: imei_number_old,
        price: price_old,
        reference: "investment",
        project_number: project_number_old,
        invoice_date: yesterday,
        warranty_expiration: yesterday,
        contract_expiration: yesterday
      },
      invoice_number: invoice_number_old,
      name: name_old,
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
      shelf: shelf_old)

    FactoryBot.create(:attachment, item: @item, real_filename: attachment_name_old)

    login(user)
    visit "/inventory/#{pool.id}/list"

    fill_in "search", with: model_old.product

    within find('[data-row="model"]', text: model_old.product) do
      click_on "expand-button"
    end

    within find('[data-row="item"]', text: inventory_code_old) do
      click_on "edit"
    end

    expect(page).to have_content(attachment_name_old)

    within find("tr", text: attachment_name_old) do
      find('button[type="button"]', text: "", match: :prefer_exact).click
    end

    expect(page).not_to have_content(attachment_name_old)

    attach_file_by_label "Attachments", "./spec/files/#{attachment_name_new}"

    expect(page).to have_content(attachment_name_new)

    fill_in "Inventory Code", with: inventory_code_new

    click_on "model_id"
    expect(page).to have_field(placeholder: "Enter search term")
    fill_in "model_id-input", with: model_new.product
    expect(page).to have_content model_new.product
    click_on model_new.product

    fill_in "Serial Number", with: serial_number_new
    fill_in "MAC-Address", with: mac_address_new
    fill_in "IMEI-Number", with: imei_number_new
    fill_in "Name", with: name_new

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
    find("[data-day='#{today.strftime("%m/%d/%Y")}']").click

    fill_in "Responsible person", with: user_name_new
    fill_in "User/Typical usage", with: typical_usage_new

    click_on "properties_reference-invoice"
    expect(page).not_to have_content "Project Number*"
    fill_in "Invoice Number", with: invoice_number_new

    click_on "Invoice Date"
    find("[data-day='#{today.strftime("%m/%d/%Y")}']").click

    fill_in "Initial Price", with: price_new

    click_on "Supplier"
    expect(page).to have_field(placeholder: "Enter search term")
    fill_in "supplier_id-input", with: supplier_new.name
    expect(page).to have_content supplier_new.name
    click_on supplier_new.name

    click_on "Warranty expiration"
    find("[data-day='#{today.strftime("%m/%d/%Y")}']").click

    click_on "Contract expiration"
    find("[data-day='#{today.strftime("%m/%d/%Y")}']").click

    click_on "building_id"
    expect(page).to have_field(placeholder: "Enter search term")
    click_on "general building"

    expect(page).to have_content "Room"

    click_on "Room"
    expect(page).to have_field(placeholder: "Enter search term")
    click_on "general room"

    fill_in "Shelf", with: shelf_new

    click_on "Save"
    expect(page).to have_text("Item was successfully saved")
    expect(page).to have_text("Inventory List")

    fill_in "search", with: model_new.product

    within find('[data-row="model"]', text: model_new.product) do
      click_on "expand-button"
    end

    within find('[data-row="item"]', text: inventory_code_new) do
      click_on "edit"
    end

    assert_field "Inventory Code", inventory_code_new
    expect(find('button[data-test-id="model_id"]')).to have_text(model_new.product)

    assert_field "Serial Number", serial_number_new
    assert_field "MAC-Address", mac_address_new
    assert_field "IMEI-Number", imei_number_new
    assert_field "Name", name_new
    assert_field "Note", note_new

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

    assert_checked(find('button[data-test-id="properties_reference-invoice"]'))

    assert_field "Invoice Number", invoice_number_new

    expect(find('button[name="invoice_date"]')).to have_text(today.strftime("%Y-%m-%d"))

    assert_field "Initial Price", price_new

    expect(find('button[data-test-id="supplier_id"]')).to have_text(supplier_new.name)

    expect(find('button[name="properties_warranty_expiration"]')).to have_text(today.strftime("%Y-%m-%d"))
    expect(find('button[name="properties_contract_expiration"]')).to have_text(today.strftime("%Y-%m-%d"))

    expect(find('button[data-test-id="building_id"]')).to have_text("general building")
    expect(find('button[data-test-id="room_id"]')).to have_text("general room")

    assert_field "Shelf", shelf_new

    expect(page).not_to have_content(attachment_name_old)
    expect(page).to have_content(attachment_name_new)
  end

  scenario "protected fields are disabled when not owner" do
    @item = FactoryBot.create(:item,
      inventory_code: inventory_code_old,
      leihs_model: model_old,
      inventory_pool: pool,
      owner: other_pool)

    login(user)
    visit "/inventory/#{pool.id}/list"

    fill_in "search", with: model_old.product

    within find('[data-row="model"]', text: model_old.product) do
      click_on "expand-button"
    end

    within find('[data-row="item"]', text: inventory_code_old) do
      click_on "edit"
    end

    inventory_code_field = find_field("Inventory Code", disabled: true)
    expect(inventory_code_field).to be_disabled
    inventory_code_field.hover
    expect(page).to have_content("Only the owner can edit this field")

    click_on "Save"
    expect(page).to have_text("Item was successfully saved")
    expect(page).to have_text("Inventory List")
  end
end
