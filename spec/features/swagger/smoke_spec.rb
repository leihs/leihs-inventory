require "spec_helper"
require "pry"

feature "Swagger" do
  context "renders" do
    before :each do
      visit "/inventory/api-docs/"
    end

    scenario "Contains expected elements" do
      expect(page).to have_content "inventory-api"
    end
  end
end
