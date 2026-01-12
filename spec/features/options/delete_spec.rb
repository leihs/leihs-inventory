require "spec_helper"
require_relative "../shared/common"

feature "Delete option", type: :feature do
  let(:user) { FactoryBot.create(:user, language_locale: "en-GB") }
  let(:pool) { FactoryBot.create(:inventory_pool) }

  let(:product) { Faker::Commerce.product_name }
  let(:version) { Faker::Commerce.color }
  let(:inventory_code) { Faker::Code.asin }
  let(:price) { Faker::Commerce.price(range: 1..100.0, as_string: false) }

  before(:each) do
    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)

    @option = FactoryBot.create(:option,
      inventory_pool: pool,
      product: product,
      version: version,
      inventory_code: inventory_code,
      price: price)
  end

  scenario "works" do
    login(user)
    visit "/inventory/#{pool.id}"
    click_on "Inventory type"
    click_on "Option"
    fill_in "search", with: "#{product} #{version}"
    await_debounce

    within "table" do
      expect(page).to have_selector("tr", text: "#{product} #{version}")
    end

    within find("tr", text: "#{product} #{version}") do
      click_on "edit"
    end
    click_on "submit-dropdown"
    click_on "Delete"
    click_on "Delete"

    fill_in "search", with: "#{product} #{version}"
    await_debounce
    expect(page).not_to have_content "#{product} #{version}"
  end

  scenario "forbidden" do
    FactoryBot.create(:reservation, option: @option)
    login(user)
    visit "/inventory/#{pool.id}"
    click_on "Inventory type"
    click_on "Option"
    fill_in "search", with: "#{product} #{version}"
    await_debounce

    within find("tr", text: "#{product} #{version}") do
      click_link("edit", wait: 20)
    end

    click_on "submit-dropdown"
    expect(page).not_to have_content "Delete"
  end
end
