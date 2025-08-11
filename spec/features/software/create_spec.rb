require "spec_helper"
require_relative "../shared/common"

feature "Create software", type: :feature do
  let(:user) { FactoryBot.create(:user, language_locale: "en-GB") }
  let(:pool) { FactoryBot.create(:inventory_pool) }

  let(:product) { Faker::Commerce.product_name }
  let(:version) { Faker::Commerce.color }
  let(:manufacturer) { Faker::Company.name }
  let(:software_information) { Faker::Lorem.paragraph }

  let(:attachment_name_1) { "secd.pdf" }
  let(:attachment_name_2) { "shenpaper.pdf" }

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
    click_on "New software"

    fill_in "Product", with: product
    fill_in "Version", with: version
    fill_in "Manufacturer", with: manufacturer
    fill_in "Software information", with: software_information

    within id: "pool.software.attachments.title" do
      find("input[type='file']", visible: false).attach_file "./spec/files/#{attachment_name_1}"
      find("input[type='file']", visible: false).attach_file "./spec/files/#{attachment_name_2}"
    end

    click_on "Save software"

    expect(page).to have_content "Inventory List"
    expect(page).to have_content "#{product} #{version}"
    expect(find("tr", text: "#{product} #{version}")).to have_content("Software")

    fill_in "search", with: "#{product} #{version}"
    find("a", text: "edit").click

    assert_field("Product", product)
    assert_field("Version", version)
    expect(find_field("Manufacturer").value).to eq manufacturer
    assert_field("Software information", software_information)

    within id: "pool.software.attachments.title" do
      find("tr", text: attachment_name_1)
      find("tr", text: attachment_name_2)
    end
  end

  scenario "fails with invalid mandatory fields" do
    login(user)
    visit "/inventory/#{pool.id}/list"
    click_on "Add inventory"
    click_on "New software"

    click_on "Save software"
    expect(page.find("body", visible: :all).text).to include("Software could not be created because one field is invalid")
    expect(page).to have_content "Too small: expected input to have >=1 characters"
  end

  scenario "fails with confilicting product name" do
    FactoryBot.create(:leihs_model, type: "Software", product: product, version: version)
    login(user)
    visit "/inventory/#{pool.id}/list"
    click_on "Add inventory"
    click_on "New software"
    fill_in "Product", with: product
    fill_in "Version", with: version
    fill_in "Manufacturer", with: manufacturer
    fill_in "Software information", with: software_information

    click_on "Save software"

    expect(page.find("body", visible: :all).text).to include("A software with this name already exists")
  end

  scenario "cancel works" do
    login(user)
    visit "/inventory/#{pool.id}/list"
    click_on "Add inventory"
    click_on "New software"

    fill_in "Product", with: product
    fill_in "Version", with: version
    fill_in "Manufacturer", with: manufacturer
    fill_in "Software information", with: software_information

    click_on "submit-dropdown"
    click_on "Cancel"

    expect(page).to have_content "Inventory List"
  end
end
