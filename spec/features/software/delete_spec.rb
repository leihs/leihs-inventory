require "spec_helper"
require_relative "../shared/common"

feature "Delete software", type: :feature do
  let(:user) { FactoryBot.create(:user, language_locale: "en-GB") }
  let(:pool) { FactoryBot.create(:inventory_pool) }

  let(:product) { Faker::Commerce.product_name }
  let(:version) { Faker::Commerce.color }
  let(:manufacturer) { Faker::Company.name }
  let(:software_information) { Faker::Lorem.paragraph }

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
      product: product,
      version: version,
      manufacturer: manufacturer,
      technical_detail: software_information)

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
    fill_in "search", with: "#{product} #{version}"
    expect(find("tr", text: "#{product} #{version}")).to have_content("Software")

    within "tr", text: "#{product} #{version}" do
      find("a", text: "edit").click
    end

    click_on "submit-dropdown"
    click_on "Delete"
    click_on "Delete"

    fill_in "search", with: "#{product} #{version}"
    expect(page).not_to have_content "#{product} #{version}"
  end
end
