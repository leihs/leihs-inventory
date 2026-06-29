require "features_helper"
require_relative "../shared/common"
require_relative "shared_setup"

feature "Scan & Edit — URL persistence", type: :feature do
  include_context "scan_edit setup"

  scenario "selecting a field and entering a value persists them in the URL" do
    visit_scan_edit
    set_status_note("Persisted text")
    await_debounce
    fields = query_params["field"]
    expect(fields.size).to eq(1)
    expect(fields.first).to include("status_note").and include("Persisted text")
  end

  scenario "field selection and value are restored after page reload" do
    visit_scan_edit
    set_status_note("Round trip")
    await_debounce
    visit current_url
    expect(find("textarea[name='update.0.value']").value).to eq("Round trip")
  end

  scenario "a successful submit adds the item's id to the URL" do
    visit_scan_edit
    set_status_note(Faker::Lorem.sentence)
    submit_barcode(item.inventory_code)
    expect(page).to have_text("Item has been successfully updated")
    expect(query_params["ids"]).to eq([item.id])
  end
end
