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
  let(:price) { Faker::Commerce.price(range: 100..5000).to_s }
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

    # expect(page).to have_field("Owner", disabled: true)

    click_on "Last Checked"

    yesterday = Date.today - 1
    day = yesterday.day

    click_on day.to_s

    fill_in "Responsible person", with: user_name
    fill_in "User/Typical usage", with: typical_usage

    click_on "properties_reference-investment"
    fill_in "Project Number*", with: project_number
    fill_in "Invoice Number", with: invoice_number

    ## TODO: fix date spec BE
    click_on "Invoice Date"
    click_on day.to_s

    ## TODO: fix price spec (parse as number)
    # fill_in "Initial Price", with: price

    click_on "Supplier"
    expect(page).to have_field(placeholder: "Enter search term")
    fill_in "supplier_id-input", with: supplier.name
    expect(page).to have_content supplier.name
    click_on supplier.name

    ## TODO: fix date spec BE
    click_on "Warranty expiration"
    click_on day.to_s

    ## TODO: fix date spec BE
    click_on "Contract expiration"
    click_on day.to_s

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

    expect(page).to have_content "Inventory List"

    fill_in "search", with: model.product
    click_on "expand-row"

    expect(page).to have_content inventory_code
  end
end
