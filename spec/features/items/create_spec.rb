require "spec_helper"
require_relative "../shared/common"

feature "Create item", type: :feature do
  let(:user) { FactoryBot.create(:user, language_locale: "en-GB") }
  let(:pool) { FactoryBot.create(:inventory_pool) }
  let!(:model) { FactoryBot.create(:leihs_model) }
  let!(:supplier) { FactoryBot.create(:supplier) }

  let(:inventory_code) { Faker::Barcode.ean }
  let(:serial_number) { Faker::Barcode.isbn }
  let(:mac_address) { Faker::Internet.mac_address }
  let(:imei_number) { Faker::Number.number(digits: 15).to_s }
  let(:name) { Faker::Device.model_name }
  let(:note) { Faker::Lorem.paragraph }
  let(:invoice_number) { Faker::Number.number(digits: 10).to_s }
  let(:price) do
    base_price = Faker::Commerce.price(range: 100..5000).floor
    decimals = [*1..9].sample(2).join # Ensures two different non-zero digits
    format("%.2f", "#{base_price}.#{decimals}".to_f)
  end
  let(:user_name) { Faker::Name.name }
  let(:reason_for_retirement) { Faker::Lorem.sentence }
  let(:typical_usage) { Faker::Lorem.sentence }
  let(:project_number) { Faker::Number.number(digits: 5).to_s }
  let(:status_note) { Faker::Lorem.sentence }
  let(:shelf) { Faker::Alphanumeric.alpha(number: 3).upcase + "-" + Faker::Number.number(digits: 2).to_s }

  let(:attachment_name_1) { "secd.pdf" }
  let(:attachment_name_2) { "shenpaper.pdf" }

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
    click_on "New item"

    fill_in "Inventory Code", with: inventory_code

    click_on "model_id"
    expect(page).to have_field(placeholder: "Enter search term")
    fill_in "model_id-input", with: model.product
    expect(page).to have_content model.product
    click_on model.product

    fill_in "Serial Number", with: serial_number
    fill_in "MAC-Address", with: mac_address
    fill_in "IMEI-Number", with: imei_number
    fill_in "Name", with: name

    attach_file_by_label "Attachments", "./spec/files/#{attachment_name_1}"

    fill_in "Note", with: note

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

    expect(page).to have_button("Owner", disabled: true)

    click_on "Last Checked"

    yesterday = Date.today - 1
    find("[data-day='#{yesterday.strftime("%m/%d/%Y")}']").click

    fill_in "Responsible person", with: user_name
    fill_in "User/Typical usage", with: typical_usage

    expect(page).not_to have_content "Project Number*"
    click_on "properties_reference-investment"
    fill_in "Project Number*", with: project_number
    fill_in "Invoice Number", with: invoice_number

    click_on "Invoice Date"
    find("[data-day='#{yesterday.strftime("%m/%d/%Y")}']").click

    fill_in "Initial Price", with: price

    click_on "Supplier"
    expect(page).to have_field(placeholder: "Enter search term")
    fill_in "supplier_id-input", with: supplier.name
    expect(page).to have_content supplier.name
    click_on supplier.name

    click_on "Warranty expiration"
    find("[data-day='#{yesterday.strftime("%m/%d/%Y")}']").click

    click_on "Contract expiration"
    find("[data-day='#{yesterday.strftime("%m/%d/%Y")}']").click

    expect(page).not_to have_content "Room"

    click_on "building_id"
    expect(page).to have_field(placeholder: "Enter search term")
    click_on "general building"

    expect(page).to have_content "Room"

    click_on "Room"
    expect(page).to have_field(placeholder: "Enter search term")
    click_on "general room"

    fill_in "Shelf", with: shelf

    click_on "Create"
    expect(page).to have_text "Item was successfully created"
    expect(page).to have_content "Inventory List"

    fill_in "search", with: model.product

    within find('[data-row="model"]', text: model.product) do
      click_on "expand-button"
    end

    within find('[data-row="item"]', text: inventory_code) do
      click_on "edit"
    end

    assert_field "Inventory Code", inventory_code
    expect(find('button[data-test-id="model_id"]')).to have_text(model.product)

    assert_field "Serial Number", serial_number
    assert_field "MAC-Address", mac_address
    assert_field "IMEI-Number", imei_number
    assert_field "Name", name
    assert_field "Note", note

    expect(page).to have_content(attachment_name_1)
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

    assert_checked(find('button[data-test-id="properties_reference-investment"]'))

    assert_field "Project Number*", project_number
    assert_field "Invoice Number", invoice_number

    expect(find('button[name="invoice_date"]')).to have_text(yesterday.strftime("%Y-%m-%d"))

    assert_field "Initial Price", price

    expect(find('button[data-test-id="supplier_id"]')).to have_text(supplier.name)

    expect(find('button[name="properties_warranty_expiration"]')).to have_text(yesterday.strftime("%Y-%m-%d"))
    expect(find('button[name="properties_contract_expiration"]')).to have_text(yesterday.strftime("%Y-%m-%d"))

    expect(find('button[data-test-id="building_id"]')).to have_text("general building")
    expect(find('button[data-test-id="room_id"]')).to have_text("general room")

    assert_field "Shelf", shelf
  end

  scenario "fails with invalid mandatory fields" do
    login(user)
    visit "/inventory/#{pool.id}/list"
    click_on "Add inventory"
    click_on "New item"

    click_on "Create"

    expect(page).to have_text "Item could not be created because 2 fields are invalid"

    click_on "model_id"
    expect(page).to have_field(placeholder: "Enter search term")
    fill_in "model_id-input", with: model.product
    expect(page).to have_content model.product
    click_on model.product

    click_on "building_id"
    expect(page).to have_field(placeholder: "Enter search term")
    click_on "general building"

    click_on "Create"
    expect(page).to have_text "Item could not be created because one field is invalid"

    click_on "Room"
    expect(page).to have_field(placeholder: "Enter search term")
    click_on "general room"

    click_on "properties_reference-investment"

    click_on "Retirement"
    expect(page).to have_content "Yes"
    click_on "Yes"

    click_on "Create"
    expect(page).to have_text "Item could not be created because 2 fields are invalid"
  end

  scenario "fails with conflicting inventory code" do
    FactoryBot.create(:item, inventory_code: inventory_code, inventory_pool: pool, owner: pool, leihs_model: model)
    login(user)
    visit "/inventory/#{pool.id}/list"
    click_on "Add inventory"
    click_on "New item"

    fill_in "Inventory Code", with: inventory_code

    click_on "model_id"
    expect(page).to have_field(placeholder: "Enter search term")
    fill_in "model_id-input", with: model.product
    expect(page).to have_content model.product
    click_on model.product

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

    expect(page).to have_text "Item was successfully created"
  end

  scenario "cancel works" do
    login(user)
    visit "/inventory/#{pool.id}/list"
    click_on "Add inventory"
    click_on "New item"

    click_on "model_id"
    expect(page).to have_field(placeholder: "Enter search term")
    fill_in "model_id-input", with: model.product
    expect(page).to have_content model.product
    click_on model.product

    click_on "submit-dropdown"
    click_on "Cancel"

    expect(page).to have_content "Inventory List"
  end
end
