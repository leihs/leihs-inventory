require "spec_helper"
require_relative "../shared/common"

feature "Application Errors ", type: :feature do
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

  scenario "render error is caught by error boundary" do
    click_on "Trigger Render Error"

    # Verify error is displayed
    expect(page).to have_content("Application Error")
    expect(page).to have_content("An error occurred during rendering of the application.")
    expect(page).to have_button("Close")
    expect(page).to have_link("Go to Home")

    # Test Close button - closes the modal
    click_on "Close"
    expect(page).not_to have_content("Application Error")

    # Test Open error message - reopens the modal
    click_on "Open error message"
    expect(page).to have_content("Application Error")

    # Technical details are closed by default - click to open them
    find("summary", text: "Technical Details").click
    expect(page).to have_css("details[open]")

    # Test Go to Home button - navigates to /inventory/
    click_on "Go to Home"
    expect(page).to have_current_path("/inventory/")
  end
end
