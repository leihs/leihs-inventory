require "spec_helper"
require_relative "../shared/common"

feature "Delete software", type: :feature do
  let(:user) { FactoryBot.create(:user, language_locale: "en-GB") }
  let(:pool) { FactoryBot.create(:inventory_pool) }

  let(:product_old) { Faker::Commerce.product_name }
  let(:version_old) { Faker::Commerce.color }
  let(:manufacturer_old) { Faker::Company.name }
  let(:software_information_old) { Faker::Lorem.paragraph }

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

    sleep 0.4
    find("a", text: "edit", visible: true, match: :first).click

    click_on "submit-dropdown"
    click_on "Delete"
    click_on "Delete"

    fill_in "search", with: "#{product_old} #{version_old}"
    expect(page).not_to have_content "#{product_old} #{version_old}"
  end
end
