require "spec_helper"
require "pry"
require_relative "../shared/common"

feature "LanguageSwitcher", type: :feature do
  scenario "User switches language to English" do
    pool = FactoryBot.create(:inventory_pool)
    user = FactoryBot.create(:user)
    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)

    login(user)

    visit "/inventory/debug"

    # Click on the dropdown to select language
    find("button", text: "Select Language").click

    # FIXME:
    # # Expect the dropdown to have the languages
    # within('[role="menu"]') do
    #   expect(page).to have_content("de")
    #   expect(page).to have_content("en")
    #   expect(page).to have_content("fr")
    #   expect(page).to have_content("es")
    # end

    # # Click on the English language option
    # find("[role=menuitem]", text: "en").click

    # # Expect the page to have the word "Welcome"
    # expect(page).to have_content("Welcome")
  end
end
