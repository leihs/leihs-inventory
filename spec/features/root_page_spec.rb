require "features_helper"
require_relative "shared/common"

feature "Root page" do
  let!(:user) { FactoryBot.create(:user, language_locale: "en-GB") }
  let!(:pool_read) { FactoryBot.create(:inventory_pool, name: "Read Pool") }
  let!(:pool_edit) { FactoryBot.create(:inventory_pool, name: "Edit Pool") }

  before :each do
    FactoryBot.create(:access_right, inventory_pool: pool_read, user: user, role: :group_manager)
    FactoryBot.create(:access_right, inventory_pool: pool_edit, user: user, role: :inventory_manager)

    login(user)
    visit "/inventory/"
  end

  scenario "shows the API browser link" do
    expect(page).to have_text("API Browser")
  end

  scenario "shows inventory pools with correct access right icons" do
    expect(page).to have_selector("[data-test-id='pool-row']", minimum: 2)

    within find("[data-test-id='pool-row']", text: pool_read.name) do
      expect(page).to have_selector("[data-test-id='access-right-read']")
      expect(page).not_to have_selector("[data-test-id='access-right-edit']")
    end

    within find("[data-test-id='pool-row']", text: pool_edit.name) do
      expect(page).to have_selector("[data-test-id='access-right-edit']")
      expect(page).not_to have_selector("[data-test-id='access-right-read']")
    end
  end
end
