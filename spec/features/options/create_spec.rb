require "spec_helper"
require_relative "../shared/common"

feature "Create option", type: :feature do
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
  end

  scenario "works" do
    login(user)
    visit "/inventory/#{pool.id}/list"
    click_on "Add inventory"
    click_on "New option"

    fill_in "Product", with: product
    fill_in "Version", with: version
    fill_in "Inventory code", with: inventory_code
    fill_in "Price", with: price

    click_on "Save"
    expect(page.find("body", visible: :all).text).to include("Option successfully created")

    expect(page).to have_content "Inventory List"

    fill_in "search", with: "#{product} #{version}"
    expect(page).to have_content "#{product} #{version}"
    expect(find("tr", text: "#{product} #{version}")).to have_content("Option")

    find("a", text: "edit").click

    assert_field("Product", product)
    assert_field("Version", version)
    expect(find_field("Inventory code").value).to eq inventory_code
    assert_field("Price", price.to_s)
  end

  scenario "fails with invalid mandatory fields" do
    login(user)
    visit "/inventory/#{pool.id}/list"
    click_on "Add inventory"
    click_on "New option"

    click_on "Save"
    expect(page.find("body", visible: :all).text).to include("Option could not be created because 2 fields are invalid")
    expect(page).to have_content "Too small: expected input to have >=1 characters"
    expect(page).to have_content "Too small: expected input to have >=1 characters"
  end

  scenario "fails with confilicting inventory code" do
    FactoryBot.create(:option, inventory_code: inventory_code)
    login(user)
    visit "/inventory/#{pool.id}/list"
    click_on "Add inventory"
    click_on "New option"
    fill_in "Product", with: product
    fill_in "Version", with: version
    fill_in "Inventory code", with: inventory_code
    fill_in "price", with: price

    click_on "Save"

    expect(page.find("body", visible: :all).text).to include("A option with this inventory code already exists")
  end

  scenario "cancel works" do
    login(user)
    visit "/inventory/#{pool.id}/list"
    click_on "Add inventory"
    click_on "New option"

    fill_in "Product", with: product
    fill_in "Version", with: version
    fill_in "Inventory code", with: inventory_code
    fill_in "price", with: price

    click_on "submit-dropdown"
    click_on "Cancel"

    expect(page).to have_content "Inventory List"
  end
end
