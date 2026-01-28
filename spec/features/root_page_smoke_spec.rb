require "spec_helper"
require "pry"

feature "Root page" do
  context "renders" do
    before :each do
      visit "/"
    end

    scenario "Contains expected elements" do
      expect(page).to have_title "Inventory"
    end
  end
end
