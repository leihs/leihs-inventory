require "spec_helper"
require "pry"

feature "Inventory Page", type: :feature do
  scenario "User visits the inventory page" do
    visit "/inventory/models"
    expect(page).to have_content("Inventarliste")
  end
end
