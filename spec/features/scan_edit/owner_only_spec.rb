require "features_helper"
require_relative "../shared/common"
require_relative "shared_setup"

feature "Scan & Edit — owner-only field restriction", type: :feature do
  include_context "scan_edit setup"

  let(:other_pool) { FactoryBot.create(:inventory_pool) }
  let(:field_label) { "Owner Restricted #{SecureRandom.hex(4)}" }
  let!(:owner_only_field) do
    field_name = "owner_restricted_#{SecureRandom.hex(4)}"
    FactoryBot.create(:field,
      id: "properties_#{field_name}",
      data: Sequel.pg_jsonb({
        label: field_label,
        type: "text",
        group: "General Information",
        attribute: ["properties", field_name],
        target_type: "item",
        permissions: {
          role: "inventory_manager",
          owner: true
        }
      }))
  end

  # Item managed by pool but owned by other_pool — visible in search but not editable with owner-only fields
  let!(:managed_item) do
    FactoryBot.create(:item,
      inventory_code: "OWN-#{Faker::Alphanumeric.alpha(number: 6).upcase}",
      leihs_model: model,
      inventory_pool: pool,
      owner: other_pool)
  end

  scenario "submitting an owner-only field for a non-owned item shows an error toast with the field label" do
    visit_scan_edit

    click_on "field-select-0"
    within find('[data-test-id="field-options"]') do
      find("button", text: field_label).click
    end

    fill_in "update.0.value", with: "some value"

    submit_barcode(managed_item.inventory_code)

    expect(page).to have_text("Item cannot be updated")
    expect(page).to have_text(field_label)
  end
end
