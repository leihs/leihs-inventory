require "features_helper"
require_relative "../shared/common"
require_relative "shared_setup"

feature "Scan & Edit — max-history warning", type: :feature do
  include_context "scan_edit setup"

  def visit_scan_edit_with_history(count)
    login(user)
    ids_query = Array.new(count) { SecureRandom.uuid }
      .map { |id| "ids=#{id}" }
      .join("&")
    visit "/inventory/#{pool.id}/scan-edit?#{ids_query}"
  end

  scenario "submitting when history is full shows the max-history toast" do
    visit_scan_edit_with_history(150)
    set_status_note(Faker::Lorem.sentence)
    submit_barcode(item.inventory_code)
    expect(page).to have_text("Edit history is full")
  end

  scenario "clicking Continue on the max-history toast clears ids and submits" do
    visit_scan_edit_with_history(150)
    set_status_note(Faker::Lorem.sentence)
    submit_barcode(item.inventory_code)
    expect(page).to have_text("Edit history is full")

    find("button", text: "Continue").click
    expect(page).to have_text("Item has been successfully updated")
    expect(query_params["ids"]).to eq([item.id])
  end
end
