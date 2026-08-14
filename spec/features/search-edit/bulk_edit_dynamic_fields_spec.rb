require "features_helper"
require_relative "../shared/common"
require_relative "../shared/patch_form_helpers"

feature "Search & Edit - bulk edit dynamic fields", type: :feature do
  let(:user) { FactoryBot.create(:user, language_locale: "en-GB") }
  let(:pool) { FactoryBot.create(:inventory_pool) }
  let!(:model) { FactoryBot.create(:leihs_model) }

  before(:each) do
    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)
  end

  scenario "bulk-edits text and dynamic property fields across multiple selected items" do
    suffix = SecureRandom.hex(4)
    inv_prefix = "BULK-#{suffix.upcase}"

    FactoryBot.create(:field,
      id: "properties_ampere_#{suffix}",
      data: Sequel.pg_jsonb({
        label: "Ampère",
        type: "text",
        group: "General Information",
        attribute: ["properties", "ampere_#{suffix}"],
        target_type: "item",
        permissions: {role: "inventory_manager", owner: false}
      }))

    FactoryBot.create(:field,
      id: "properties_electrical_power_#{suffix}",
      data: Sequel.pg_jsonb({
        label: "Power consumption in kw/h",
        type: "text",
        group: "General Information",
        attribute: ["properties", "electrical_power_#{suffix}"],
        target_type: "item",
        permissions: {role: "inventory_manager", owner: false}
      }))

    item_a = FactoryBot.create(:item,
      inventory_code: "#{inv_prefix}-A",
      leihs_model: model,
      inventory_pool: pool,
      owner: pool)

    item_b = FactoryBot.create(:item,
      inventory_code: "#{inv_prefix}-B",
      leihs_model: model,
      inventory_pool: pool,
      owner: pool)

    login(user)
    visit "/inventory/#{pool.id}/search-edit"

    click_on "Add filters to start your search."
    expect(page).to have_button("Add search parameter")
    click_on "or-0-field-select-0"
    within find('[data-test-id="field-options"]') do
      find("span", text: "Inventory Code").click
    end
    await_debounce
    find("input[name='$or.0.$and.0.value']").set(inv_prefix)
    await_debounce

    expect(page).to have_css("tbody tr", minimum: 2)
    find("thead button[role='checkbox']").click

    expect(page).to have_button("edit-button", disabled: false, wait: 10)
    click_on "edit-button"
    expect(page).to have_content("Add field")

    idx = 0

    add_patch_field(idx, "Status note")
    fill_patch_text(idx, "bulk updated note")
    idx += 1

    add_patch_field(idx, "Borrowable")
    click_patch_radio(idx, "false")
    idx += 1

    add_patch_field(idx, "Ampère")
    fill_patch_text(idx, "16A")
    idx += 1

    add_patch_field(idx, "Power consumption in kw/h")
    fill_patch_text(idx, "2.5")

    click_on "apply-button"
    expect(page).to have_content("2 Items were successfully updated", wait: 15)

    [item_a, item_b].each do |it|
      it.reload
      expect(it.status_note).to eq("bulk updated note")
      expect(it.is_borrowable).to be(false)
      props = (it.properties || {}).transform_keys(&:to_s)
      expect(props["ampere_#{suffix}"]).to eq("16A")
      expect(props["electrical_power_#{suffix}"]).to eq("2.5")
    end
  end
end
