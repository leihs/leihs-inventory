require "spec_helper"
require_relative "../shared/common"

feature "Create model", type: :feature do
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

  let!(:parent_category_1) { FactoryBot.create(:category) }
  let!(:parent_category_1_1) { FactoryBot.create(:category) }
  let!(:parent_category_1_2) { FactoryBot.create(:category) }
  let!(:leaf_category_1_1) { FactoryBot.create(:category) }

  let!(:parent_category_2) { FactoryBot.create(:category) }
  let!(:parent_category_2_1) { FactoryBot.create(:category) }
  let!(:parent_category_2_2) { FactoryBot.create(:category) }
  let!(:leaf_category_2_1) { FactoryBot.create(:category) }

  let(:image_name_1) { "anon.jpg" }
  let(:image_name_2) { "lisp-machine.jpg" }
  let(:attachment_name_1) { "secd.pdf" }
  let(:attachment_name_2) { "shenpaper.pdf" }

  let!(:compatible_model_1) { FactoryBot.create(:leihs_model) }
  let!(:compatible_model_2) { FactoryBot.create(:leihs_model) }

  let(:first_accessory_name) { "First accessory" }
  let(:second_accessory_name) { "Second accessory" }
  let(:first_property_key) { "First property" }
  let(:first_property_value) { "First value" }
  let(:second_property_key) { "Second property" }
  let(:second_property_value) { "Second value" }

  before(:each) do
    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)

    parent_category_1.add_child(parent_category_1_1)
    parent_category_1.add_child(parent_category_1_2)
    parent_category_1_1.add_child(leaf_category_1_1)

    parent_category_2.add_child(parent_category_2_1)
    parent_category_2.add_child(parent_category_2_2)
    parent_category_2_1.add_child(leaf_category_2_1)
  end

  scenario "works" do
    login(user)
    visit "/inventory/#{pool.id}/list"
    click_on "Add inventory"
    click_on "New model"
    click_on "this is a package"
    fill_in "Product", with: product

    fill_in "Version", with: version

    click_on "Manufacturer"
    fill_in "manufacturer-input", with: manufacturer
    click_on manufacturer

    fill_in "Description", with: description
    fill_in "Technical Details", with: technical_details
    fill_in "Internal Description", with: internal_description
    fill_in "Important notes for hand over", with: hand_over_note

    click_on "Select Entitlement-Group"
    click_on entitlement_group_1.name
    fill_in "entitlements.0.quantity", with: 2

    click_on "Select Entitlement-Group"
    click_on entitlement_group_2.name
    fill_in "entitlements.1.quantity", with: 1

    click_on "Select category"
    click_on leaf_category_1_1.name
    click_on "Select category"
    click_on parent_category_2_1.name

    within id: "pool.model.images.title" do
      find("input[type='file']", visible: false).attach_file "./spec/files/#{image_name_1}"
      find("input[type='file']", visible: false).attach_file "./spec/files/#{image_name_2}"
      find("button[role='radio']", match: :first).click
    end

    within id: "pool.model.attachments.title" do
      find("input[type='file']", visible: false).attach_file "./spec/files/#{attachment_name_1}"
      find("input[type='file']", visible: false).attach_file "./spec/files/#{attachment_name_2}"
    end

    within id: "pool.model.accessories.title" do
      click_on "Add accessory"
      fill_in "accessories.0.name", with: first_accessory_name
      click_on "Add accessory"
      fill_in "accessories.1.name", with: second_accessory_name
    end

    click_on "compatibles"
    fill_in("Search model", with: compatible_model_1.product)

    fill_in "models-input", with: compatible_model_1.product
    within find("[data-test-id='models-list']") do
      click_on "#{compatible_model_1.product} #{compatible_model_1.version}"
    end

    click_on "compatibles"
    fill_in "models-input", with: compatible_model_2.product
    within find("[data-test-id='models-list']") do
      click_on "#{compatible_model_2.product} #{compatible_model_2.version}"
    end

    within id: "pool.model.model_properties.title" do
      click_on "Add property"
      fill_in "properties.0.key", with: first_property_key
      fill_in "properties.0.value", with: first_property_value
      click_on "Add property"
      fill_in "properties.1.key", with: second_property_key
      fill_in "properties.1.value", with: second_property_value
    end

    click_on "Create"

    expect(page).to have_content "Inventory List"
    expect(page).to have_content "#{product} #{version}"

    fill_in "search", with: "#{product} #{version}"
    find("a", text: "edit").click

    expect(
      find("label", text: "this is a package").find(:xpath, "..").find("input", visible: false)[:value]
    ).to eq("on")

    assert_field("Product", product)
    assert_field("Version", version)
    assert_button("manufacturer", manufacturer)
    assert_field("Description", description)
    assert_field("Technical Details", technical_details)
    assert_field("Internal Description", internal_description)
    assert_field("Important notes for hand over", hand_over_note)

    expect(find("tr", text: entitlement_group_1.name).find("input").value).to eq "2"
    expect(find("tr", text: entitlement_group_2.name).find("input").value).to eq "1"

    within id: "pool.model.categories.title" do
      cat_1 = find("tr", text: leaf_category_1_1.name)
      cat_1.find("li", text: "#{parent_category_1.name} / #{parent_category_1_1.name} / #{leaf_category_1_1.name}")

      cat_2 = find("tr", text: parent_category_2_1.name)
      cat_2.find("li", text: "#{parent_category_2.name} / #{parent_category_2_1.name}")
    end

    within id: "pool.model.images.title" do
      find("tr", text: image_name_1)
      assert_checked find("tr", text: image_name_1).find("button[role='radio']")
      find("tr", text: image_name_2)
    end

    within id: "pool.model.attachments.title" do
      find("tr", text: attachment_name_1)
      find("tr", text: attachment_name_2)
    end

    within id: "pool.model.accessories.title" do
      assert_field("accessories.0.name", first_accessory_name)
      assert_field("accessories.1.name", second_accessory_name)
    end

    within id: "pool.model.compatible_models.title" do
      find("tr", text: "#{compatible_model_1.product} #{compatible_model_1.version}")
      find("tr", text: "#{compatible_model_2.product} #{compatible_model_2.version}")
    end

    within id: "pool.model.model_properties.title" do
      assert_field("properties.0.key", first_property_key)
      assert_field("properties.0.value", first_property_value)
      assert_field("properties.1.key", second_property_key)
      assert_field("properties.1.value", second_property_value)
    end
  end

  scenario "fails with invalid mandatory fields" do
    login(user)
    visit "/inventory/#{pool.id}/list"
    click_on "Add inventory"
    click_on "New model"

    click_on "Create"
    expect(page).to have_text("Model could not be created because one field is invalid")
    expect(page).to have_text("Too small: expected input to have >=1 characters")
  end

  scenario "fails with confilicting product name" do
    FactoryBot.create(:leihs_model, type: "Software", product: product, version: version)
    login(user)
    visit "/inventory/#{pool.id}/list"
    click_on "Add inventory"
    click_on "New model"
    fill_in "Product", with: product
    fill_in "Version", with: version

    click_on "Create"

    expect(page.find("body", visible: :all).text).to include("A model with this name already exists")
  end

  scenario "cancel works" do
    login(user)
    visit "/inventory/#{pool.id}/list"
    click_on "Add inventory"
    click_on "New model"

    fill_in "Product", with: product
    fill_in "Version", with: version

    click_on "submit-dropdown"
    click_on "Cancel"

    expect(page).to have_content "Inventory List"
  end
end
