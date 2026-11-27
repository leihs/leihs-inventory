require "features_helper"
require_relative "../shared/common"
require_relative "shared_setup"

feature "Scan & Edit — field-change warning", type: :feature do
  include_context "scan_edit setup"

  let!(:item2) do
    FactoryBot.create(:item,
      inventory_code: "SCAN-#{Faker::Alphanumeric.alpha(number: 6).upcase}",
      leihs_model: model,
      inventory_pool: pool,
      owner: pool)
  end

  scenario "changing the selected field between submits shows a warning toast" do
    visit_scan_edit
    set_status_note("First")
    submit_barcode(item.inventory_code)
    expect_toast("Item has been successfully updated")

    set_note("Second")
    submit_barcode(item2.inventory_code)
    expect_toast("Field configuration changed")
  end

  scenario "clicking Continue on the warning clears ids and submits the new item" do
    visit_scan_edit
    set_status_note("First")
    submit_barcode(item.inventory_code)
    expect_toast("Item has been successfully updated")

    set_note("Second")
    submit_barcode(item2.inventory_code)
    expect_toast("Field configuration changed")

    find("[data-sonner-toast] button", text: "Continue").click
    expect_toast("Item has been successfully updated", wait: 15)
    expect(page).to have_css("table tbody tr", text: item2.inventory_code, wait: 15)
    expect(query_params["ids"]).to eq([item2.id])
  end
end
