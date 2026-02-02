require "features_helper"
require_relative "../shared/common"

feature "Delete entitlement group", type: :feature do
  let(:user) { FactoryBot.create(:user, language_locale: "en-GB") }
  let(:pool) { FactoryBot.create(:inventory_pool) }

  before(:each) do
    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)

    @model = FactoryBot.create(:leihs_model)
    @entitlement_group1 = FactoryBot.create(:entitlement_group, inventory_pool: pool, models: [@model])
  end

  scenario "delete from entitlement group works" do
    login(user)
    visit "/inventory/#{pool.id}/entitlement-groups/"

    within "table" do
      expect(page).to have_selector("tr", text: @entitlement_group1.name)
      click_on "edit"
    end

    click_on "submit-dropdown"
    click_on "Delete"
    click_on "Delete"

    expect(page.find("body", visible: :all).text).to include("Entitlement group was successfully deleted")
    expect(page).not_to have_content @entitlement_group1.name
  end

  scenario "delete from list works" do
    login(user)
    visit "/inventory/#{pool.id}/entitlement-groups/"

    within "table" do
      expect(page).to have_selector("tr", text: @entitlement_group1.name)
      click_on "delete"
    end

    click_on "Delete"
    expect(page.find("body", visible: :all).text).to include("Entitlement group was successfully deleted")
    expect(page).not_to have_content @entitlement_group1.name
  end
end
