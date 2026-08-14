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

  def set_status_note(value)
    click_on "field-select-0"
    within find('[data-test-id="field-options"]') do
      find("button", text: "Status note").click
    end
    find("textarea[name='update.0.value']").set(value)
  end

  def set_note(value)
    click_on "field-select-0"
    within find('[data-test-id="field-options"]') do
      find("button", text: "Note").click
    end
    find("textarea[name='update.0.value']").set(value)
  end

  def submit_barcode(inv_code)
    input = find('[data-test-id="barcode-input"]')
    input.set(inv_code)
    input.send_keys(:return)
  end

  def query_params
    CGI.parse(URI.parse(current_url).query || "")
  end
end
