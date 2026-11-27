require "features_helper"

RSpec.shared_context "scan_edit setup" do
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

  def visit_scan_edit
    login(user)
    visit "/inventory/#{pool.id}/scan-edit"
  end

  def select_scan_edit_field(label)
    dismiss_open_menus
    click_on "field-select-0"
    within find('[data-test-id="field-options"]', wait: 10) do
      find("span", text: label, match: :first).click
    end
    expect(page).not_to have_css('[data-test-id="field-options"]', wait: 5)
    expect(find('[data-test-id="field-select-0"]', wait: 10)).to have_text(label)
  end

  def set_status_note(value)
    select_scan_edit_field("Status note")
    find("textarea[name='update.0.value']", wait: 10).set(value)
  end

  def set_note(value)
    select_scan_edit_field("Note")
    find("textarea[name='update.0.value']", wait: 10).set(value)
  end

  def submit_barcode(inv_code)
    input = find('[data-test-id="barcode-input"]', wait: 10)
    input.set(inv_code)
    input.send_keys(:return)
  end

  def query_params
    CGI.parse(URI.parse(current_url).query || "")
  end
end
