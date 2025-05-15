require "spec_helper"
require_relative "../shared/common"

feature "Create model", type: :feature do
  scenario "works" do
    user = FactoryBot.create(:user, language_locale: "en-GB")
    pool = FactoryBot.create(:inventory_pool)
    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)
    login(user)
    click_on "Add Inventory"
    click_on "New Model"
  end
end
