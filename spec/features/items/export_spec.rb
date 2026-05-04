require "features_helper"
require_relative "../shared/common"
require "csv"
require "cgi"

feature "Items Export", type: :feature do
  let(:user) { FactoryBot.create(:user, language_locale: "en-GB") }
  let(:pool) { FactoryBot.create(:inventory_pool) }
  let!(:building) { FactoryBot.create(:building) }
  let!(:room) { FactoryBot.create(:room, building_id: building.id) }

  let(:inventory_code_1) { "EXPORTTEST-#{SecureRandom.hex(4).upcase}" }
  let(:inventory_code_2) { "EXPORTTEST-#{SecureRandom.hex(4).upcase}" }

  let!(:model) { FactoryBot.create(:leihs_model, product: "Export Test Model") }

  let!(:item_1) do
    FactoryBot.create(:item,
      inventory_code: inventory_code_1,
      leihs_model: model,
      inventory_pool: pool,
      owner: pool,
      room: room)
  end

  let!(:item_2) do
    FactoryBot.create(:item,
      inventory_code: inventory_code_2,
      leihs_model: model,
      inventory_pool: pool,
      owner: pool,
      room: room)
  end

  before(:each) do
    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)
  end

  after(:each) do
    cleanup_download("items.csv")
    cleanup_download("items.xlsx")
  end

  # Navigate to search-edit with a pre-encoded filter_d that matches our EXPORTTEST- items.
  # The filter JSON mirrors the shape the page produces when the user types in the
  # inventory_code field: {"$or":[{"$and":[{"inventory_code":{"$ilike":"EXPORTTEST%"}}]}]}
  def search_edit_url(pool_id)
    filter = CGI.escape('{"$or":[{"$and":[{"inventory_code":{"$ilike":"EXPORTTEST%"}}]}]}')
    "/inventory/#{pool_id}/search-edit?filter_d=#{filter}&page=1&size=50"
  end

  scenario "export button shows total filtered count when nothing is selected" do
    login(user)
    visit search_edit_url(pool.id)

    expect(page).to have_selector('[data-test-id="export-button"]', wait: 10)

    within('[data-test-id="export-button"]') do
      expect(page).to have_text("2")
    end
  end

  scenario "CSV export of all filtered results downloads a file with the correct rows" do
    login(user)
    visit search_edit_url(pool.id)

    expect(page).to have_selector('[data-test-id="export-button"]', wait: 10)

    find('[data-test-id="export-button"]').click
    click_on "CSV"

    downloaded = wait_for_download("items.csv")
    expect(downloaded).not_to be_nil, "items.csv was not downloaded within the timeout"

    csv = CSV.parse(File.read(downloaded))
    header = csv[0]
    rows = csv[1..]

    expect(header).to include("inventory_code")
    expect(header).to include("product")

    code_idx = header.index("inventory_code")
    exported_codes = rows.map { |row| row[code_idx] }

    expect(exported_codes).to include(inventory_code_1)
    expect(exported_codes).to include(inventory_code_2)
  end

  scenario "export button shows selected count and CSV contains only selected items" do
    login(user)
    visit search_edit_url(pool.id)

    # Wait for the results table to appear
    expect(page).to have_selector("table tbody tr", wait: 10)

    # Select only the first row (index 0 = header checkbox, index 1 = first data row)
    row_checkboxes = all('button[role="checkbox"]')
    row_checkboxes[1].click

    # Badge should now show 1 (selected count)
    within('[data-test-id="export-button"]') do
      expect(page).to have_text("1")
    end

    find('[data-test-id="export-button"]').click
    click_on "CSV"

    downloaded = wait_for_download("items.csv")
    expect(downloaded).not_to be_nil, "items.csv was not downloaded within the timeout"

    csv = CSV.parse(File.read(downloaded))
    header = csv[0]
    rows = csv[1..]

    expect(rows.length).to eq(1)

    code_idx = header.index("inventory_code")
    expect([inventory_code_1, inventory_code_2]).to include(rows.first[code_idx])
  end

  scenario "Excel export downloads a non-empty .xlsx file" do
    login(user)
    visit search_edit_url(pool.id)

    expect(page).to have_selector('[data-test-id="export-button"]', wait: 10)

    find('[data-test-id="export-button"]').click
    click_on "Excel"

    downloaded = wait_for_download("items.xlsx")
    expect(downloaded).not_to be_nil, "items.xlsx was not downloaded within the timeout"
    expect(File.size(downloaded)).to be > 0
  end
end
