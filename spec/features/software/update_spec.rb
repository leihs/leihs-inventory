require "spec_helper"
require_relative "../shared/common"

feature "Update software", type: :feature do
  let(:user) { FactoryBot.create(:user, language_locale: "en-GB") }
  let(:pool) { FactoryBot.create(:inventory_pool) }

  let(:product_old) { Faker::Commerce.product_name }
  let(:product_new) { Faker::Commerce.product_name }

  let(:version_old) { Faker::Commerce.color }
  let(:version_new) { Faker::Commerce.color }

  let(:manufacturer_old) { Faker::Company.name }
  let(:manufacturer_new) { Faker::Company.name }

  let(:software_information_old) { Faker::Lorem.paragraph }
  let(:software_information_new) { Faker::Lorem.paragraph }

  let(:attachment_name_1) { "secd.pdf" }
  let(:attachment_name_2) { "shenpaper.pdf" }
  let(:attachment_name_3) { "turing.pdf" }

  before(:each) do
    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)

    @software = FactoryBot.create(:leihs_model,
      type: "Software",
      product: product_old,
      version: version_old,
      manufacturer: manufacturer_old,
      technical_detail: software_information_old)

    FactoryBot.create(:attachment, leihs_model: @software,
      real_filename: attachment_name_1)
    FactoryBot.create(:attachment, leihs_model: @software,
      real_filename: attachment_name_2)
  end

  scenario "works" do
    login(user)
    visit "/inventory/#{pool.id}"
    select_value("with_items", "all")
    click_on "Inventory type"
    click_on "Software"
    fill_in "search", with: "#{product_old} #{version_old}"
    expect(find("tr", text: "#{product_old} #{version_old}")).to have_content("Software")
    find("a", text: "edit").click

    fill_in "Product", with: product_new
    fill_in "Version", with: version_new
    fill_in "Manufacturer", with: manufacturer_new
    fill_in "Software information", with: software_information_new

    within id: "pool.software.attachments.title" do
      find("tr", text: attachment_name_1).all("button").last.click
      find("input[type='file']", visible: false).attach_file "./spec/files/#{attachment_name_3}"
    end

    click_on "Save"

    expect(page).to have_content "Inventory List"
    select_value("with_items", "all")
    fill_in "search", with: "#{product_new} #{version_new}"

    within "table" do
      expect(page).to have_selector("tr", text: "#{product_new} #{version_new}", visible: true)
      expect(find("tr", text: "#{product_new} #{version_new}")).to have_content("Software")
    end

    within find("tr", text: "#{product_new} #{version_new}", visible: true) do
      click_on "edit"
    end

    assert_field("Product", product_new)
    assert_field("Version", version_new)
    expect(find_field("Manufacturer").value).to eq manufacturer_new
    assert_field("Software information", software_information_new)

    within id: "pool.software.attachments.title" do
      expect(page).not_to have_selector("tr", text: attachment_name_1)
      find("tr", text: attachment_name_2)
      find("tr", text: attachment_name_3)
    end
  end
end
