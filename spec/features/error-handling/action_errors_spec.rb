require "features_helper"
require_relative "../shared/common"

feature "Action Errors ", type: :feature do
  let(:pool) { FactoryBot.create(:inventory_pool) }
  let(:user) { FactoryBot.create(:user, language_locale: "en-GB") }

  before(:each) do
    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)

    login(user)
    visit "/inventory/debug"
  end

  scenario "404 action error shows toast with status code" do
    click_on "Test Action Error (404)"

    expect(page).to have_content("Something went wrong while updating", wait: 5)
    expect(page).to have_content("Error code: 404")

    # Page remains functional - no crash or navigation
    expect(page).to have_content("Error Boundary Tests")
  end

  scenario "400 action error shows toast with status code" do
    click_on "Test Action Error (400)"

    expect(page).to have_content("Something went wrong while updating", wait: 5)
    expect(page).to have_content("Error code: 400")

    # Page remains functional - no crash or navigation
    expect(page).to have_content("Error Boundary Tests")
  end
end
