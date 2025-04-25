require "spec_helper"
require "pry"

feature "Inventory Page", type: :feature do
  scenario "User visits the inventory page" do
    pool = FactoryBot.create(:inventory_pool)
    user = FactoryBot.create(:user)
    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)
    visit "/inventory"

    fill_in("user", with: user.email)
    fill_in("password", with: user.password)
    click_on("Continue")

    expect(page).to have_content("Inventarliste")
  end
end
