require "features_helper"
require_relative "../shared/common"

feature "Duplicate License", type: :feature do
  let(:user) { FactoryBot.create(:user, language_locale: "en-GB") }
  let(:pool) { FactoryBot.create(:inventory_pool) }

  let!(:software_model) do
    FactoryBot.create(:leihs_model,
      product: Faker::App.name,
      type: "Software")
  end

  let(:license_version) { Faker::App.version }
  let(:dongle_id) { "DONGLE-#{Faker::Alphanumeric.alpha(number: 8).upcase}" }
  let(:procured_by) { Faker::Company.name }
  let(:note) { Faker::Lorem.paragraph }

  let(:attachment_name) { "secd.pdf" }

  before(:each) do
    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)

    @original_license = FactoryBot.create(:item,
      leihs_model: software_model,
      inventory_pool: pool,
      owner: pool,
      note: note,
      properties: {
        license_version: license_version,
        license_type: "site_license",
        activation_type: "dongle",
        dongle_id: dongle_id,
        procured_by: procured_by
      })

    FactoryBot.create(:attachment, item: @original_license, real_filename: attachment_name)
  end

  scenario "duplicates license with correct field copying" do
    login(user)
    visit "/inventory/#{pool.id}/list"

    find("input[name='search']").set(software_model.product)
    await_debounce

    within find('[data-row="model"]', text: software_model.product) do
      click_on "expand-button"
    end

    within find('[data-row="item"]', text: @original_license.inventory_code) do
      find('[data-test-id="edit-dropdown"]').click
    end

    click_on "Duplicate license"

    expect(page).to have_current_path(%r{/licenses/create\?fromItem=#{@original_license.id}})
    expect(page).to have_content("Create New License")

    # Fields that ARE copied
    expect(find('button[data-test-id="software_model_id"]')).to have_text(software_model.product)
    assert_field("Procured by", procured_by)
    assert_field("Note", note)

    # Fields that are NOT copied
    expect(page).not_to have_content(attachment_name) # attachments excluded from copy

    # Fill mandatory inventory code and submit
    fill_in "Inventory Code", with: Faker::Barcode.ean

    click_on "Create"
    expect(page).to have_text("License was successfully created")
    expect(page).to have_content("Inventory List")

    # Verify two license rows now exist under the model
    find("input[name='search']").set(software_model.product)
    await_debounce

    within find('[data-row="model"]', text: software_model.product) do
      click_on "expand-button"
    end

    item_rows = all('[data-row="item"]')
    expect(item_rows.count).to eq(2)

    # Find the new (duplicated) license row
    duplicated_row = item_rows.find { |row| !row.text.include?(@original_license.inventory_code) }

    within duplicated_row do
      click_on "edit"
    end

    # Verify copied fields persisted on the new license
    expect(find('button[data-test-id="software_model_id"]')).to have_text(software_model.product)
    assert_field("Procured by", procured_by)
    assert_field("Note", note)

    # Verify excluded fields are absent
    expect(find_field("Inventory Code").value).not_to eq(@original_license.inventory_code)
    expect(page).not_to have_content(attachment_name)
  end
end
