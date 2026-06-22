require "features_helper"
require_relative "../shared/common"
require_relative "../shared/price_field_examples"

feature "Update option", type: :feature do
  let(:user) { FactoryBot.create(:user, language_locale: "en-GB") }
  let(:pool) { FactoryBot.create(:inventory_pool) }

  let(:product_old) { Faker::Commerce.product_name }
  let(:product_new) { Faker::Commerce.product_name }

  let(:version_old) { Faker::Commerce.color }
  let(:version_new) { Faker::Commerce.color }

  let(:inventory_code_old) { Faker::Code.asin }
  let(:inventory_code_new) { Faker::Code.asin }

  let(:price_old) { Faker::Commerce.price(range: 1..100.0, as_string: false) }
  let(:price_new) { Faker::Commerce.price(range: 1..100.0, as_string: false) }

  before(:each) do
    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)

    FactoryBot.create(:option,
      inventory_pool: pool,
      product: product_old,
      version: version_old,
      inventory_code: inventory_code_old,
      price: price_old)
  end

  scenario "works" do
    login(user)
    visit "/inventory/#{pool.id}"
    click_on "Inventory type"
    click_on "Option"
    fill_in "search", with: "#{product_old} #{version_old}"
    await_debounce

    within "table" do
      expect(page).to have_selector("tr", text: "#{product_old} #{version_old}", visible: true)
    end

    within find("tr", text: "#{product_old} #{version_old}", visible: true) do
      click_on "edit"
    end

    fill_in "Product", with: product_new
    fill_in "Version", with: version_new
    fill_in "Inventory code", with: inventory_code_new
    fill_in "Price", with: price_new

    click_on "Save"
    expect(page.find("body", visible: :all).text).to include("Option successfully saved")

    expect(page).to have_content "Inventory List"
    await_debounce
    fill_in "search", with: "#{product_new} #{version_new}"

    within "table" do
      expect(page).to have_selector("tr", text: "#{product_new} #{version_new}", visible: true)
    end

    within find("tr", text: "#{product_new} #{version_new}", visible: true) do
      click_on "edit"
    end

    assert_field("Product", product_new)
    assert_field("Version", version_new)
    assert_field("Inventory code", inventory_code_new)
    assert_field("Price", format_price_display(price_new))
  end

  context "price field formatting" do
    before do
      login(user)
      visit "/inventory/#{pool.id}"
      click_on "Inventory type"
      click_on "Option"
      fill_in "search", with: "#{product_old} #{version_old}"
      await_debounce
      within find("tr", text: "#{product_old} #{version_old}", visible: true) do
        click_on "edit"
      end
    end

    include_examples "price field", field_label: "Price"
  end
end
