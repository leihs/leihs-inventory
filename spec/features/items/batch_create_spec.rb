require "features_helper"
require_relative "../shared/common"

feature "Batch create items", type: :feature do
  let(:user) { FactoryBot.create(:user, language_locale: "en-GB") }
  let(:pool) { FactoryBot.create(:inventory_pool) }
  let!(:model) { FactoryBot.create(:leihs_model) }

  # Serial numbers for testing updates
  let(:serial_number) { Faker::Barcode.isbn }
  let(:serial_number_2) { Faker::Barcode.isbn }

  before(:each) do
    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)
  end

  scenario "batch create items works" do
    # Phase 1: Setup & Navigation
    login(user)
    click_on "Add inventory"
    click_on "New item"

    # Phase 2: Set Batch Mode with Count = 3
    fill_in "Create number of items", with: "3"

    # Phase 3: Verify Create Button Shows "3 x"
    expect(page).to have_button("3 x Create")

    # Phase 4: Check Disabled Fields
    # Check that inventory_code is disabled
    inventory_code_field = find_field("Inventory Code", disabled: true)
    expect(inventory_code_field).to be_disabled

    click_on "inventory_code-disabled-info"
    expect(page).to have_content("This field is locked when creating multiple items")

    # Check that serial_number is disabled
    serial_number_field = find_field("Serial Number", disabled: true)
    expect(serial_number_field).to be_disabled

    click_on "serial_number-disabled-info"
    expect(page).to have_content("This field is locked when creating multiple items")

    # Check that attachments is disabled
    click_on "attachments-disabled-info"
    expect(page).to have_content("This field is locked when creating multiple items")

    # Phase 5: Select Required Fields
    # Select model (required)
    click_on "model_id"
    expect(page).to have_field(placeholder: "Enter search term")
    fill_in "model_id-input", with: model.product
    expect(page).to have_content model.product
    click_on model.product

    # Select building (required)
    click_on "building_id"
    expect(page).to have_field(placeholder: "Enter search term")
    click_on "general building"

    # Select room (required)
    expect(page).to have_content "Room"
    click_on "Room"
    expect(page).to have_field(placeholder: "Enter search term")
    click_on "general room"

    # Phase 6: Create Items & Verify Redirect
    # Click create button
    click_on "3 x Create"

    # Verify success message
    expect(page).to have_text "Items successfully created"

    # Verify we're on the review page
    expect(page).to have_current_path(%r{/inventory/#{pool.id}/items/review\?.*ids=.*&ids=.*&ids=.*&mid=.*})

    # More specific: verify query params structure
    expect(current_url).to match(/ids=[0-9a-f-]{36}/)
    expect(current_url).to match(/mid=[0-9a-f-]{36}/)

    # Phase 7: Review Page - Header Checks
    # Check page title
    expect(page).to have_content("Items successfully created")

    # Check Summary section exists
    expect(page).to have_content("Summary")

    # Check Count is displayed (in separate divs)
    expect(page).to have_content("Count")
    expect(page).to have_content("3")

    # Check model name is displayed
    expect(page).to have_content(model.product)
    expect(page).to have_link(model.product) # It's a link to the model page

    # Phase 8: Review Page - Table Structure
    # Check table exists
    expect(page).to have_selector("table")

    # Check table has correct number of rows
    within "table" do
      rows = all("tbody tr")
      expect(rows.count).to eq(3)
    end

    # Store item data for later tests
    item_ids = []
    inventory_codes = []

    # Phase 9: Review Page - Table Columns Verification
    within "table" do
      rows = all("tbody tr")

      rows.each_with_index do |row, idx|
        within row do
          cells = all("td")

          # Column 1: Index (1-based)
          index_value = cells[0].text
          expect(index_value).to eq((idx + 1).to_s)

          # Column 2: Inventory Code
          inventory_code = cells[1].text
          expect(inventory_code).not_to be_empty
          expect(inventory_code).to match(/^#{pool.shortname}/) # Starts with pool shortname
          inventory_codes << inventory_code

          # Column 3: Serial Number Input Field
          serial_input = cells[2].find("input[name='serial_number']")
          expect(serial_input).not_to be_disabled
          expect(serial_input.value).to eq("") # Should be empty initially

          # Column 4: UUID Link
          uuid_link = cells[3].find("a")
          uuid_text = uuid_link.text
          expect(uuid_text).to match(/^[0-9a-f-]{36}$/) # UUID format
          expect(uuid_link[:href]).to include("/inventory/#{pool.id}/items/")
          item_ids << {uuid: uuid_text, href: uuid_link[:href]}
        end
      end
    end

    # Verify inventory codes are sequential
    # Assuming format: "POOL123", "POOL124", "POOL125"
    # base_number = inventory_codes.first.gsub(/#{pool.shortname}/, '').to_i
    # inventory_codes.each_with_index do |code, idx|
    #   expected_number = base_number + idx
    #   expect(code).to eq("#{pool.shortname}#{expected_number}")
    # end

    # Phase 10: Test UUID Link Navigation
    # Click on the first item's UUID link
    first_item = item_ids.first
    first_item_href = first_item[:href]

    within "table tbody tr:first-child" do
      click_on first_item[:uuid]
    end

    # Verify we're on the item edit page
    expect(page).to have_current_path(first_item_href)
    expect(page).not_to have_content("Create number of items")

    # Verify the item page displays correct inventory code
    assert_field "Inventory Code", inventory_codes.first

    # Go back to review page
    page.go_back

    # Verify we're back on review page
    expect(page).to have_content("Items successfully created")
    expect(page).to have_content("Summary")

    # Phase 11: Test Serial Number Update
    # Find the second row and update its serial number
    within "table tbody tr:nth-child(2)" do
      fill_in "serial_number", with: serial_number
      # Find and click the save button (likely an icon button)
      find("button[type='submit']").click
    end

    # Wait for success toast
    expect(page).to have_text("Serial number updated", wait: 5)

    # Verify checkmark appears in the saved field (row 2)
    within "table tbody tr:nth-child(2)" do
      # Look for the CircleCheck SVG icon with green color
      checkmark = find("svg.lucide-circle-check.text-green-500")
      expect(checkmark).to be_present
    end

    # Verify the next input field (row 3) is now focused
    within "table tbody tr:nth-child(3)" do
      serial_input = find("input[name='serial_number']")
      expect(serial_input).to match_css(":focus")
    end

    # Store the second item's data
    second_item = item_ids[1]

    # Navigate to the second item to verify the serial number was saved
    within "table tbody tr:nth-child(2)" do
      click_on second_item[:uuid]
    end

    # Verify we're on the item page
    expect(page).to have_current_path(second_item[:href])

    # Verify the serial number field shows the updated value
    assert_field "Serial Number", serial_number

    # Go back to review page
    page.go_back

    # Verify the serial number is still shown in the input field
    within "table tbody tr:nth-child(2)" do
      serial_input = find("input[name='serial_number']")
      expect(serial_input.value).to eq(serial_number)
    end

    # Phase 12: Test Barcode Toggle
    # Initially, barcodes should not be shown (showing inventory codes as text)
    within "table" do
      rows = all("tbody tr")
      rows.each_with_index do |row, idx|
        within row do
          cells = all("td")
          # Column 2 should have inventory code as text (not barcode)
          inventory_code_text = cells[1].text
          expect(inventory_code_text).to eq(inventory_codes[idx])
          # Should NOT have barcode element
          expect(cells[1]).not_to have_selector("[data-test-id^='barcode-']")
        end
      end
    end

    # Find and toggle the barcode switch ON
    barcode_switch = find("#barcode")
    expect(barcode_switch).not_to be_checked # Verify initial state is OFF
    barcode_switch.click

    # Verify barcodes are now displayed
    within "table" do
      rows = all("tbody tr")

      # Verify we have exactly 3 barcodes
      barcodes = all("[data-test-id^='barcode-']")
      expect(barcodes.count).to eq(3)

      rows.each_with_index do |row, idx|
        within row do
          cells = all("td")

          # Column 2 should now have barcode SVG element
          barcode_element = cells[1].find("[data-test-id='barcode-#{inventory_codes[idx]}']")
          expect(barcode_element).to be_present
          expect(barcode_element.tag_name).to eq("svg") # Verify it's an SVG

          # Verify barcode SVG contains a text node with the inventory code
          # The jsbarcode library creates a <text> element inside the SVG
          within barcode_element do
            expect(page).to have_selector("text", text: inventory_codes[idx])
          end
        end
      end
    end

    # Toggle barcode switch back OFF
    barcode_switch.click

    # Verify barcodes are hidden again (back to plain text)
    within "table" do
      row = all("tbody tr").first
      within row do
        cells = all("td")
        expect(cells[1]).not_to have_selector("[data-test-id^='barcode-']")
        # Should show inventory code as plain text again
        expect(cells[1].text).to eq(inventory_codes.first)
      end
    end

    # Phase 13: Test URL Toggle
    # Initially, URLs should not be shown (showing UUIDs)
    within "table" do
      rows = all("tbody tr")
      rows.each_with_index do |row, _idx|
        within row do
          cells = all("td")
          # Column 4 should show UUID (not full URL)
          uuid_link = cells[3].find("a")
          uuid_text = uuid_link.text
          expect(uuid_text).to match(/^[0-9a-f-]{36}$/) # UUID format
          expect(uuid_text).not_to include("/inventory/") # Should NOT show full URL
        end
      end
    end

    # Find and toggle the URL switch ON
    url_switch = find("#urls")
    expect(url_switch).not_to be_checked # Verify initial state is OFF
    url_switch.click

    # Verify full URLs are now displayed
    within "table" do
      rows = all("tbody tr")
      rows.each_with_index do |row, idx|
        within row do
          cells = all("td")
          # Column 4 should now show full URL
          url_link = cells[3].find("a")
          url_text = url_link.text

          # Verify it's the full URL format
          expect(url_text).to include("/inventory/#{pool.id}/items/")

          # Verify the URL points to the correct item
          item_uuid = item_ids[idx][:uuid]
          expected_url = "/inventory/#{pool.id}/items/#{item_uuid}"
          expect(url_text).to eq(expected_url)
        end
      end
    end

    # Toggle URL switch back OFF
    url_switch.click

    # Verify UUIDs are shown again (not full URLs)
    within "table" do
      row = all("tbody tr").first
      within row do
        cells = all("td")
        uuid_link = cells[3].find("a")
        uuid_text = uuid_link.text
        expect(uuid_text).to match(/^[0-9a-f-]{36}$/) # UUID format
        expect(uuid_text).not_to include("/inventory/") # Should NOT show full URL
      end
    end

    click_on "Back to Inventory"
    expect(page).to have_content "Inventory List"
  end
end
