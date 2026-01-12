require "spec_helper"
require_relative "../shared/common"

feature "Delete model", type: :feature do
  let(:user) { FactoryBot.create(:user, language_locale: "en-GB") }
  let(:pool) { FactoryBot.create(:inventory_pool) }

  let(:product) { Faker::Commerce.product_name }
  let(:version) { Faker::Commerce.color }
  let(:manufacturer) { Faker::Company.name }
  let(:description) { Faker::Lorem.paragraph }
  let(:technical_details) { Faker::Lorem.paragraph }
  let(:internal_description) { Faker::Lorem.paragraph }
  let(:hand_over_note) { Faker::Lorem.paragraph }

  let!(:entitlement_group_1) {
    FactoryBot.create(:entitlement_group,
      inventory_pool: pool)
  }
  let!(:entitlement_group_2) {
    FactoryBot.create(:entitlement_group,
      inventory_pool: pool)
  }
  let!(:entitlement_group_3) {
    FactoryBot.create(:entitlement_group,
      inventory_pool: pool)
  }

  let!(:root_category_1) { FactoryBot.create(:category) }
  let!(:parent_category_1_1) { FactoryBot.create(:category) }
  let!(:parent_category_1_2) { FactoryBot.create(:category) }
  let!(:leaf_category_1_1_1) { FactoryBot.create(:category) }

  let!(:root_category_2) { FactoryBot.create(:category) }
  let!(:parent_category_2_1) { FactoryBot.create(:category) }
  let!(:parent_category_2_2) { FactoryBot.create(:category) }
  let!(:leaf_category_2_1_1) { FactoryBot.create(:category) }

  let(:image_name_1) { "anon.jpg" }
  let(:image_name_2) { "lisp-machine.jpg" }
  let(:image_name_3) { "sap.png" }
  let(:attachment_name_1) { "secd.pdf" }
  let(:attachment_name_2) { "shenpaper.pdf" }
  let(:attachment_name_3) { "turing.pdf" }

  # let!(:compatible_model_1) { FactoryBot.create(:leihs_model) }
  # let!(:compatible_model_2) { FactoryBot.create(:leihs_model) }
  # let!(:compatible_model_3) { FactoryBot.create(:leihs_model) }

  let(:first_accessory_name) { "First accessory old" }
  let(:second_accessory_name) { "Second accessory old" }
  let(:second_accessory_name_new) { "Second accessory new" }
  let(:third_accessory_name_new) { "Third accessory new" }

  let(:first_property_key) { "First key old" }
  let(:first_property_value) { "First value old" }
  let(:second_property_key) { "Second key old" }
  let(:second_property_value) { "Second value old" }
  let(:second_property_key_new) { "Second key new" }
  let(:second_property_value_new) { "Second value new" }
  let(:third_property_key_new) { "Third key new" }
  let(:third_property_value_new) { "Third value new" }

  before(:each) do
    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)

    root_category_1.add_child(parent_category_1_1)
    root_category_1.add_child(parent_category_1_2)
    parent_category_1_1.add_child(leaf_category_1_1_1)

    root_category_2.add_child(parent_category_2_1)
    root_category_2.add_child(parent_category_2_2)
    parent_category_2_1.add_child(leaf_category_2_1_1)

    @model = FactoryBot.create(:leihs_model,
      product: product,
      version: version,
      manufacturer: manufacturer,
      description: description,
      technical_detail: technical_details,
      internal_description: internal_description,
      hand_over_note: hand_over_note)

    FactoryBot.create(:entitlement,
      leihs_model: @model,
      entitlement_group: entitlement_group_1,
      quantity: 1)
    FactoryBot.create(:entitlement,
      leihs_model: @model,
      entitlement_group: entitlement_group_2,
      quantity: 2)

    @model.add_category(leaf_category_1_1_1)
    @model.add_category(parent_category_2_1)

    FactoryBot.create(:accessory,
      leihs_model: @model,
      name: first_accessory_name)
    FactoryBot.create(:accessory,
      leihs_model: @model,
      name: second_accessory_name)

    @model.add_property(key: first_property_key,
      value: first_property_value)
    @model.add_property(key: second_property_key,
      value: second_property_value)

    image_1 = FactoryBot.create(:image, :for_leihs_model,
      target: @model,
      real_filename: image_name_1)
    FactoryBot.create(:image, :for_leihs_model,
      target: @model,
      real_filename: image_name_2)
    @model.cover_image_id = image_1.id

    FactoryBot.create(:attachment, leihs_model: @model,
      real_filename: attachment_name_1)
    FactoryBot.create(:attachment, leihs_model: @model,
      real_filename: attachment_name_2)
  end

  scenario "works" do
    login(user)
    visit "/inventory/#{pool.id}"
    select_value("with_items", "all")
    click_on "Inventory type"
    within("[data-test-id='type-filter-dropdown']") do
      click_on "Model"
    end
    fill_in "search", with: "#{product} #{version}"
    await_debounce

    within "table" do
      expect(page).to have_selector("tr", text: "#{product} #{version}")
    end

    within find("tr", text: "#{product} #{version}") do
      click_link("edit", wait: 20)
    end

    click_on "submit-dropdown"
    click_on "Delete"
    click_on "Delete"

    fill_in "search", with: "#{product} #{version}"
    await_debounce
    expect(page).not_to have_content "#{product} #{version}"
  end

  scenario "disallowed" do
    FactoryBot.create(:item, inventory_pool: pool, leihs_model: @model)
    login(user)
    visit "/inventory/#{pool.id}"
    select_value("with_items", "all")
    click_on "Inventory type"
    within("[data-test-id='type-filter-dropdown']") do
      click_on "Model"
    end
    fill_in "search", with: "#{product} #{version}"
    await_debounce

    within find("tr", text: "#{product} #{version}") do
      click_link("edit", wait: 20)
    end

    click_on "submit-dropdown"

    expect(page).not_to have_content "Delete"
  end
end
