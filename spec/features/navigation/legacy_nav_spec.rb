require "spec_helper"
require_relative "../shared/common"

feature "Navigation to legacy", type: :feature do
  let(:user) { FactoryBot.create(:user, language_locale: "en-GB") }
  let(:pool) { FactoryBot.create(:inventory_pool) }
  before(:each) do
    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)
  end

  scenario "global search" do
    login(user)
    fill_in "global search", with: "hello"
    find("input[label='global search']").send_keys(:return)
    expect(page.current_url).to include("#{pool.id}/search?search_term=hello")
  end
end
