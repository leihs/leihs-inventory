require "features_helper"
require_relative "../shared/common"

feature "Update license", type: :feature do
  let(:user) { FactoryBot.create(:user, language_locale: "en-GB") }
  let(:pool) { FactoryBot.create(:inventory_pool) }

  # Old and new values for all fields
  let(:inventory_code_old) { Faker::Barcode.ean }
  let(:inventory_code_new) { Faker::Barcode.ean }

  let(:license_version_old) { Faker::App.version }
  let(:license_version_new) { Faker::App.version }

  let(:dongle_id_old) { "DONGLE-#{Faker::Alphanumeric.alpha(number: 8).upcase}" }
  let(:dongle_id_new) { "DONGLE-#{Faker::Alphanumeric.alpha(number: 8).upcase}" }

  let(:total_quantity_old) { Faker::Number.between(from: 5, to: 50).to_s }
  let(:total_quantity_new) { Faker::Number.between(from: 5, to: 50).to_s }

  let(:maintenance_price_old) do
    base_price = Faker::Commerce.price(range: 100..5000).floor
    decimals = [*1..9].sample(2).join
    format("%.2f", "#{base_price}.#{decimals}".to_f)
  end
  let(:maintenance_price_new) do
    base_price = Faker::Commerce.price(range: 100..5000).floor
    decimals = [*1..9].sample(2).join
    format("%.2f", "#{base_price}.#{decimals}".to_f)
  end

  let(:procured_by_old) { Faker::Company.name }
  let(:procured_by_new) { Faker::Company.name }

  let(:note_old) { Faker::Lorem.paragraph }
  let(:note_new) { Faker::Lorem.paragraph }

  let(:attachment_name_old) { "secd.pdf" }
  let(:attachment_name_new) { "shenpaper.pdf" }

  let!(:software_model_old) do
    FactoryBot.create(:leihs_model,
      product: Faker::App.name,
      type: "Software")
  end
  let!(:software_model_new) do
    FactoryBot.create(:leihs_model,
      product: Faker::App.name,
      type: "Software")
  end
  let!(:other_pool) { FactoryBot.create(:inventory_pool) }

  before(:each) do
    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)
  end

  scenario "works" do
    yesterday = Date.today - 1

    # Create existing license with old values
    @license = FactoryBot.create(:item,
      inventory_code: inventory_code_old,
      leihs_model: software_model_old,
      inventory_pool: pool,
      owner: pool,
      properties: {
        license_version: license_version_old,
        activation_type: "dongle",
        dongle_id: dongle_id_old,
        license_type: "site_license",
        operating_system: %w[windows linux],
        installation: ["local"],
        total_quantity: total_quantity_old,
        license_expiration: yesterday,
        maintenance_contract: "true",
        maintenance_expiration: yesterday,
        maintenance_currency: "CHF",
        maintenance_price: maintenance_price_old,
        procured_by: procured_by_old
      },
      note: note_old)

    FactoryBot.create(:attachment, item: @license, real_filename: attachment_name_old)

    login(user)
    visit "/inventory/#{pool.id}/list"

    fill_in "search", with: software_model_old.product
    await_debounce

    within find('[data-row="model"]', text: software_model_old.product) do
      click_on "expand-button"
    end

    within find('[data-row="item"]', text: inventory_code_old) do
      click_on "edit"
    end

    # Verify old values are displayed
    expect(page).to have_content(attachment_name_old)

    # Remove old attachment
    within find("tr", text: attachment_name_old) do
      find('button[type="button"]', text: "", match: :prefer_exact).click
    end

    expect(page).not_to have_content(attachment_name_old)

    # Add new attachment
    attach_file_by_label "Attachments", "./spec/files/#{attachment_name_new}"
    expect(page).to have_content(attachment_name_new)

    # Update basic fields
    fill_in "Inventory Code", with: inventory_code_new

    click_on "software_model_id"
    expect(page).to have_field(placeholder: "Enter search term")
    fill_in "software_model_id-input", with: software_model_new.product
    expect(page).to have_content software_model_new.product
    click_on software_model_new.product

    fill_in "License Version", with: license_version_new

    # Update activation type (test visibility change)
    click_on "Activation type"
    click_on "Serial Number"

    # Dongle ID field should disappear
    expect(page).not_to have_content "Dongle ID"

    # Change back to dongle to test visibility again
    click_on "Activation type"
    click_on "Dongle"
    expect(page).to have_content "Dongle ID"
    fill_in "Dongle ID", with: dongle_id_new

    # Update license type
    click_on "License type"
    click_on "Multiple Workplace"

    # Update checkboxes (uncheck old, check new)
    click_on "properties_operating_system-linux"
    click_on "properties_operating_system-mac_os_x"

    click_on "properties_installation-web"
    click_on "Running Account"

    # Update total quantity
    fill_in "Total quantity", with: total_quantity_new

    # Update dates
    today = Date.today
    click_on "License expiration"
    find("[data-day='#{today.strftime("%-m/%-d/%Y")}']").click

    # Test maintenance visibility chain still works
    expect(page).to have_content "Maintenance expiration"
    click_on "Maintenance expiration"
    find("[data-day='#{today.strftime("%-m/%-d/%Y")}']").click

    expect(page).to have_content "Currency"
    click_on "Currency"
    click_on "EUR"

    within find('#fields\.Maintenance\.title') do
      expect(page).to have_content "Price"
      fill_in "Price", with: maintenance_price_new
    end

    # Update other fields
    fill_in "Procured by", with: procured_by_new
    fill_in "Note", with: note_new

    # Save
    click_on "Save"
    expect(page).to have_text("License was successfully saved")
    expect(page).to have_text("Inventory List")

    # Navigate back and verify all updates
    fill_in "search", with: software_model_new.product
    await_debounce

    within find('[data-row="model"]', text: software_model_new.product) do
      click_on "expand-button"
    end

    within find('[data-row="item"]', text: inventory_code_new) do
      click_on "edit"
    end

    # Verify all new values
    assert_field "Inventory Code", inventory_code_new

    expect(find('button[data-test-id="software_model_id"]')).to have_text(software_model_new.product)

    assert_field "License Version", license_version_new

    # Verify visibility-dependent fields
    expect(page).to have_content "Dongle ID"
    assert_field "Dongle ID", dongle_id_new

    assert_field "Total quantity", total_quantity_new

    # Verify checkboxes
    assert_checked(find('button[data-test-id="properties_operating_system-windows"]'))
    assert_unchecked(find('button[data-test-id="properties_operating_system-linux"]'))
    assert_checked(find('button[data-test-id="properties_operating_system-mac_os_x"]'))

    assert_checked(find('button[data-test-id="properties_installation-local"]'))
    assert_checked(find('button[data-test-id="properties_installation-web"]'))

    # Verify dates
    expect(find('button[name="properties_license_expiration"]')).to have_text(today.strftime("%Y-%m-%d"))
    expect(find('button[name="properties_maintenance_expiration"]')).to have_text(today.strftime("%Y-%m-%d"))

    # Verify maintenance fields
    within find('#fields\.Maintenance\.title') do
      assert_field "Price", maintenance_price_new
    end

    # Verify other fields
    assert_field "Procured by", procured_by_new
    assert_field "Note", note_new

    # Verify attachments
    expect(page).not_to have_content(attachment_name_old)
    expect(page).to have_content(attachment_name_new)
  end

  scenario "protected fields are disabled when not owner" do
    @license = FactoryBot.create(:item,
      inventory_code: inventory_code_old,
      leihs_model: software_model_old,
      inventory_pool: pool,
      owner: other_pool,
      properties: {
        license_version: license_version_old
      })

    login(user)
    visit "/inventory/#{pool.id}/list"

    fill_in "search", with: software_model_old.product
    await_debounce

    within find('[data-row="model"]', text: software_model_old.product) do
      click_on "expand-button"
    end

    within find('[data-row="item"]', text: inventory_code_old) do
      click_on "edit"
    end

    inventory_code_field = find_field("Inventory Code", disabled: true)
    expect(inventory_code_field).to be_disabled

    click_on "inventory_code-disabled-info"
    expect(page).to have_content("Only the owner can edit this field")

    click_on "Save"
    expect(page).to have_text("License was successfully saved")
    expect(page).to have_text("Inventory List")
  end
end
