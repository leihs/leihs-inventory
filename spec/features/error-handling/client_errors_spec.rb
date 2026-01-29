require "features_helper"
require_relative "../shared/common"

feature "Client Errors ", type: :feature do
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

  scenario "loader error is caught and displayed correctly" do
    click_on "Test Loader Error"

    # Verify error is displayed
    expect(page).to have_content("Loader Error")
    expect(page).to have_content("The client couldn't load the data for this page.")
    expect(page).to have_button("Retry")
    expect(page).to have_button("Close")
    expect(page).to have_link("Go to Home")

    # Test Close button - closes the modal
    click_on "Close"
    expect(page).not_to have_content("Loader Error")

    # Test Open error message - reopens the modal
    click_on "Open error message"
    expect(page).to have_content("Loader Error")

    # Technical details are closed by default - click to open them
    find("summary", text: "Technical Details").click
    expect(page).to have_css("details[open]")
    find("summary", text: "Technical Details").click

    # Test Retry button - reloads page but shows same error
    click_on "Retry"
    expect(page).to have_content("Loader Error")
    expect(page).to have_content("The client couldn't load the data for this page.")

    # Test Go to Home button - navigates to /inventory/
    click_on "Go to Home"
    expect(page).to have_current_path("/inventory/")
  end

  scenario "404 error displays not found page" do
    click_on "Test 404 Error"

    # Verify error is displayed
    expect(page).to have_content("Page Not Found")
    expect(page).to have_content("The page you're looking for doesn't exist.")
    expect(page).to have_button("Close")
    expect(page).to have_link("Go to Home")

    # Test Close button - closes the modal
    click_on "Close"
    expect(page).not_to have_content("Page Not Found")

    # Test Open error message - reopens the modal
    click_on "Open error message"
    expect(page).to have_content("Page Not Found")

    # Test Go to Home button - navigates to /inventory/
    click_on "Go to Home"
    expect(page).to have_current_path("/inventory/")
  end
end
