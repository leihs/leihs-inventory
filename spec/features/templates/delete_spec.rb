require "spec_helper"
require_relative "../shared/common"

feature "Delete template", type: :feature do
  let(:user) { FactoryBot.create(:user, language_locale: "en-GB") }
  let(:pool) { FactoryBot.create(:inventory_pool) }

  before(:each) do
    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)

    @model = FactoryBot.create(:leihs_model)
    @template = FactoryBot.create(:template, inventory_pool: pool)
    @template.add_direct_model(@model)
  end

  scenario "delete from template works" do
    login(user)
    visit "/inventory/#{pool.id}/templates"

    within "table" do
      expect(page).to have_selector("tr", text: @template.name, visible: true)
      expect(page).to have_selector("tr", text: "Quantity exceeds availability", visible: true)
      expect(page).to have_selector("tr", text: "1", visible: true)
      click_on "edit"
    end

    click_on "submit-dropdown"
    click_on "Delete"
    click_on "Delete"

    expect(page.find("body", visible: :all).text).to include("Template was successfully deleted")

    expect(page).not_to have_content @template.name
  end

  scenario "delete from list works" do
    login(user)
    visit "/inventory/#{pool.id}/templates"

    within "table" do
      expect(page).to have_selector("tr", text: @template.name, visible: true)
      expect(page).to have_selector("tr", text: "Quantity exceeds availability", visible: true)
      expect(page).to have_selector("tr", text: "1", visible: true)
      click_on "delete"
    end

    click_on "Delete"
    expect(page.find("body", visible: :all).text).to include("Template was successfully deleted")
    expect(page).not_to have_content @template.name
  end
end
