require 'features_helper'
require_relative '../shared/common'

feature 'Create license', type: :feature do
  let(:user) { FactoryBot.create(:user, language_locale: 'en-GB') }
  let(:pool) { FactoryBot.create(:inventory_pool) }

  # Software model (type: "Software") - required for licenses
  let!(:software_model) do
    FactoryBot.create(:leihs_model,
                      product: Faker::App.name,
                      type: 'Software')
  end

  # License-specific test data
  let(:inventory_code) { Faker::Barcode.ean }
  let(:license_version) { Faker::App.version }
  let(:dongle_id) { "DONGLE-#{Faker::Alphanumeric.alpha(number: 8).upcase}" }
  let(:total_quantity) { Faker::Number.between(from: 5, to: 50).to_s }
  let(:maintenance_price) do
    base_price = Faker::Commerce.price(range: 100..5000).floor
    decimals = [*1..9].sample(2).join
    format('%.2f', "#{base_price}.#{decimals}".to_f)
  end
  let(:procured_by) { Faker::Company.name }
  let(:note) { Faker::Lorem.paragraph }

  let(:attachment_name_1) { 'secd.pdf' }

  before(:each) do
    FactoryBot.create(:access_right,
                      inventory_pool: pool,
                      user: user,
                      role: :inventory_manager)
  end

  scenario 'works' do
    login(user)
    visit "/inventory/#{pool.id}/list"
    click_on 'Add inventory'
    click_on 'New license'

    fill_in 'Inventory Code', with: inventory_code

    # Software model (mandatory)
    click_on 'software_model_id'
    expect(page).to have_field(placeholder: 'Enter search term')
    fill_in 'software_model_id-input', with: software_model.product
    expect(page).to have_content software_model.product
    click_on software_model.product

    # License version (mandatory)
    fill_in 'License Version', with: license_version

    # Test visibility dependency 1: Dongle ID (value-based)
    # Initially hidden
    expect(page).not_to have_content 'Dongle ID'

    # Select activation type = "dongle"
    click_on 'Activation Type'
    expect(page).to have_content 'Dongle'
    click_on 'Dongle'

    # Field should appear
    expect(page).to have_content 'Dongle ID'
    fill_in 'Dongle ID', with: dongle_id

    # License Type
    click_on 'License Type'
    expect(page).to have_content 'Site license'
    click_on 'Site license'

    # Operating System (checkboxes)
    click_on 'properties_operating_system-windows'
    click_on 'properties_operating_system-linux'

    # Installation (checkboxes)
    click_on 'properties_installation-local'
    click_on 'properties_installation-web'

    # Test visibility dependency 2: Quantity Allocations (presence-based)
    # Initially hidden
    expect(page).not_to have_content 'Quantity allocations'

    # Fill total quantity
    fill_in 'Total quantity', with: total_quantity

    # Quantity allocations field should appear
    expect(page).to have_content 'Quantity allocations'

    # Test visibility dependency 3: Maintenance Chain (cascading)
    yesterday = Date.today - 1

    # Initially all hidden
    expect(page).not_to have_content 'Maintenance expiration'

    # Step 1: Select Maintenance Contract = "Yes"
    click_on 'Maintenance contract'
    expect(page).to have_content 'Yes'
    click_on 'Yes'

    # Step 2: Maintenance expiration appears
    expect(page).to have_content 'Maintenance expiration'
    click_on 'Maintenance expiration'
    click_calendar_day(yesterday)

    # Step 3: Currency appears (presence-based trigger)
    expect(page).to have_content 'Currency'
    click_on 'Currency'
    expect(page).to have_content 'CHF'
    click_on 'CHF'

    # Step 4: Price appears (presence-based trigger)
    # Note: "Price" appears in Maintenance section
    within find('h3', text: 'Maintenance').find(:xpath, '..') do
      expect(page).to have_content 'Price'
      fill_in 'Price', with: maintenance_price
    end

    # License expiration
    click_on 'License expiration'
    click_calendar_day(yesterday)

    # Procured by
    fill_in 'Procured by', with: procured_by

    # Note
    fill_in 'Note', with: note

    # Attachments
    attach_file_by_label 'Attachments', "./spec/files/#{attachment_name_1}"

    # Owner is disabled on create
    expect(page).to have_button('Owner', disabled: true)

    # Submit
    click_on 'Create'
    expect(page).to have_text 'License was successfully created'
    expect(page).to have_content 'Inventory List'

    # Search and find license
    fill_in 'search', with: software_model.product
    await_debounce

    within find('[data-row="model"]', text: software_model.product) do
      click_on 'expand-button'
    end

    within find('[data-row="item"]', text: inventory_code) do
      click_on 'edit'
    end

    # Verify all fields saved correctly
    assert_field 'Inventory Code', inventory_code
    assert_field 'License Version', license_version
    expect(find('button[data-test-id="software_model_id"]')).to have_text(software_model.product)

    # Verify visibility-dependent fields are visible and saved
    expect(page).to have_content 'Dongle ID'
    assert_field 'Dongle ID', dongle_id

    expect(page).to have_content 'Quantity allocations'
    assert_field 'Total quantity', total_quantity

    expect(page).to have_content 'Maintenance expiration'
    expect(find('button[name="properties_maintenance_expiration"]')).to have_text(yesterday.strftime('%Y-%m-%d'))
    expect(page).to have_content 'Currency'
    within find('h3', text: 'Maintenance').find(:xpath, '..') do
      expect(page).to have_content 'Price'
      assert_field 'Price', maintenance_price
    end

    # Verify checkboxes
    assert_checked(find('button[data-test-id="properties_operating_system-windows"]'))
    assert_checked(find('button[data-test-id="properties_operating_system-linux"]'))
    assert_checked(find('button[data-test-id="properties_installation-local"]'))
    assert_checked(find('button[data-test-id="properties_installation-web"]'))

    # Verify dates
    expect(find('button[name="properties_license_expiration"]')).to have_text(yesterday.strftime('%Y-%m-%d'))

    # Verify other fields
    assert_field 'Procured by', procured_by
    assert_field 'Note', note
    expect(page).to have_content(attachment_name_1)
  end

  scenario 'fails with invalid mandatory fields' do
    login(user)
    visit "/inventory/#{pool.id}/list"
    click_on 'Add inventory'
    click_on 'New license'

    # Try submitting empty form
    click_on 'Create'
    expect(page).to have_text 'License could not be created because 2 fields are invalid'

    # Fill software model only
    click_on 'software_model_id'
    expect(page).to have_field(placeholder: 'Enter search term')
    fill_in 'software_model_id-input', with: software_model.product
    expect(page).to have_content software_model.product
    click_on software_model.product

    click_on 'Create'
    expect(page).to have_text 'License could not be created because one field is invalid'

    # Fill license version
    fill_in 'License Version', with: license_version

    # Test conditional required field: Dongle ID when activation type = dongle
    click_on 'Activation Type'
    click_on 'Dongle'

    click_on 'Create'
    expect(page).to have_text 'License could not be created because one field is invalid'

    # Fill dongle ID
    fill_in 'Dongle ID', with: dongle_id

    click_on 'Create'
    expect(page).to have_text 'License was successfully created'
  end

  scenario 'fails with conflicting inventory code' do
    FactoryBot.create(:item,
                      inventory_code: inventory_code,
                      inventory_pool: pool,
                      owner: pool,
                      leihs_model: software_model,
                      properties: {
                        license_version: license_version
                      })

    login(user)
    visit "/inventory/#{pool.id}/list"
    click_on 'Add inventory'
    click_on 'New license'

    fill_in 'Inventory Code', with: inventory_code

    click_on 'software_model_id'
    expect(page).to have_field(placeholder: 'Enter search term')
    fill_in 'software_model_id-input', with: software_model.product
    expect(page).to have_content software_model.product
    click_on software_model.product

    fill_in 'License Version', with: license_version

    click_on 'Create'

    expect(page).to have_text 'Inventory code already exists'
    click_on 'Update'

    click_on 'Create'

    expect(page).to have_text 'License was successfully created'
  end

  scenario 'cancel works' do
    login(user)
    visit "/inventory/#{pool.id}/list"
    click_on 'Add inventory'
    click_on 'New license'

    click_on 'software_model_id'
    expect(page).to have_field(placeholder: 'Enter search term')
    fill_in 'software_model_id-input', with: software_model.product
    expect(page).to have_content software_model.product
    click_on software_model.product

    click_on 'submit-dropdown'
    click_on 'Cancel'

    expect(page).to have_content 'Inventory List'
  end

  scenario 'creates license from software page' do
    login(user)
    visit "/inventory/#{pool.id}/software/#{software_model.id}/licenses/create"

    # Software model should be pre-filled and disabled
    expect(find('button[data-test-id="software_model_id"]')).to have_text(software_model.product)

    software_field = find('button[data-test-id="software_model_id"]')
    expect(software_field[:disabled]).to eq('true')

    # Fill remaining mandatory field
    fill_in 'License Version', with: license_version

    click_on 'Create'

    expect(page).to have_text 'License was successfully created'

    # Verify software model was saved correctly
    fill_in 'search', with: software_model.product
    await_debounce

    within find('[data-row="model"]', text: software_model.product) do
      click_on 'expand-button'
    end

    # Find the newly created license
    within first('[data-row="item"]') do
      click_on 'edit'
    end

    expect(find('button[data-test-id="software_model_id"]')).to have_text(software_model.product)
    assert_field 'License Version', license_version
  end
end
