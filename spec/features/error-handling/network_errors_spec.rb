require "spec_helper"
require_relative "../shared/common"

feature "Network Errors ", type: :feature do
  let(:pool) { FactoryBot.create(:inventory_pool) }
  let(:user) { FactoryBot.create(:user, language_locale: "en-GB") }

  before(:each) do
    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)

    login(user)
    visit "/inventory/#{pool.id}/"
  end

  scenario "offline and back to online" do
    # Verify page loads normally
    expect(page).to have_content("Inventory List", wait: 10)

    # Mock navigator.onLine to false AND dispatch offline event
    page.execute_script(<<~JS)
      Object.defineProperty(navigator, 'onLine', {#{" "}
        writable: true,#{" "}
        value: false#{" "}
      });
      window.dispatchEvent(new Event('offline'));
    JS

    # Verify offline modal appears
    expect(page).to have_content("No Internet Connection", wait: 5)
    expect(page).to have_content("It seems you are offline")

    # Verify modal cannot be dismissed (no close/cancel button)
    expect(page).not_to have_button("Close")
    expect(page).not_to have_button("Cancel")

    # Mock navigator.onLine back to true AND dispatch online event
    page.execute_script(<<~JS)
      Object.defineProperty(navigator, 'onLine', {#{" "}
        writable: true,#{" "}
        value: true#{" "}
      });
      window.dispatchEvent(new Event('online'));
    JS

    # Wait for modal to disappear
    expect(page).not_to have_content("No Internet Connection", wait: 5)

    # Verify page is still functional after reconnecting
    # The inventory list should still be visible and interactive
    expect(page).to have_content("Inventory List")

    # Verify we can still interact with the page (e.g., filters work)
    expect(page).to have_css("input[placeholder*='Filter']", wait: 5)
  end
end
