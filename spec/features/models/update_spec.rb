require "spec_helper"
require_relative "../shared/common"

feature "Update model", type: :feature do
  let(:user) { FactoryBot.create(:user, language_locale: "en-GB") }
  let(:pool) { FactoryBot.create(:inventory_pool) }

  let(:product_old) { Faker::Commerce.product_name }
  let(:product_new) { Faker::Commerce.product_name }

  let(:version_old) { Faker::Commerce.color }
  let(:version_new) { Faker::Commerce.color }

  let(:manufacturer_old) { Faker::Company.name }
  let(:manufacturer_new) { Faker::Company.name }

  let(:description_old) { Faker::Lorem.paragraph }
  let(:description_new) { Faker::Lorem.paragraph }

  let(:technical_details_old) { Faker::Lorem.paragraph }
  let(:technical_details_new) { Faker::Lorem.paragraph }

  let(:internal_description_old) { Faker::Lorem.paragraph }
  let(:internal_description_new) { Faker::Lorem.paragraph }

  let(:hand_over_note_old) { Faker::Lorem.paragraph }
  let(:hand_over_note_new) { Faker::Lorem.paragraph }

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

  let!(:compatible_model_1) { FactoryBot.create(:leihs_model) }
  let!(:compatible_model_2) { FactoryBot.create(:leihs_model) }
  let!(:compatible_model_3) { FactoryBot.create(:leihs_model) }

  let(:first_accessory_name_old) { "First accessory old" }
  let(:second_accessory_name_old) { "Second accessory old" }
  let(:second_accessory_name_new) { "Second accessory new" }
  let(:third_accessory_name_new) { "Third accessory new" }

  let(:first_property_key_old) { "First key old" }
  let(:first_property_value_old) { "First value old" }
  let(:second_property_key_old) { "Second key old" }
  let(:second_property_value_old) { "Second value old" }
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
      product: product_old,
      version: version_old,
      manufacturer: manufacturer_old,
      description: description_old,
      technical_detail: technical_details_old,
      internal_description: internal_description_old,
      hand_over_note: hand_over_note_old)

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
      name: first_accessory_name_old)
    FactoryBot.create(:accessory,
      leihs_model: @model,
      name: second_accessory_name_old)

    @model.add_compatible_model(compatible_model_1)
    @model.add_compatible_model(compatible_model_2)

    @model.add_property(key: first_property_key_old,
      value: first_property_value_old)
    @model.add_property(key: second_property_key_old,
      value: second_property_value_old)

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
    select_value("with_items", "without_items")
    fill_in "search", with: "#{product_old} #{version_old}"
    find("a", text: "edit").click

    fill_in "Product", with: product_new
    fill_in "Version", with: version_new
    fill_in "Manufacturer", with: manufacturer_new
    fill_in "Description", with: description_new
    fill_in "Technical Details", with: technical_details_new
    fill_in "Internal Description", with: internal_description_new
    fill_in "Important notes for hand over", with: hand_over_note_new

    find("tr", text: entitlement_group_1.name).find("button").click
    click_on "Select Entitlement-Group"
    find("[data-value='#{entitlement_group_3.name}']").click
    fill_in "entitlements.1.quantity", with: 1

    find("tr", text: leaf_category_1_1_1.name).find("button").click
    click_on "Select category"
    find("[data-value='#{parent_category_1_1.name}']").click

    within id: "pool.model.images.title" do
      find("tr", text: image_name_1).all("button").last.click
      find("tr", text: image_name_2).all("button").first.click
      find("input[type='file']", visible: false).attach_file "./spec/files/#{image_name_3}"
    end

    within id: "pool.model.attachments.title" do
      find("tr", text: attachment_name_1).all("button").last.click
      find("input[type='file']", visible: false).attach_file "./spec/files/#{attachment_name_3}"
    end

    within id: "pool.model.accessories.title" do
      first("tr button").click
      fill_in "accessories.0.name", with: second_accessory_name_new
      click_on "Add accessory"
      fill_in "accessories.1.name", with: third_accessory_name_new
    end

    within id: "pool.model.compatible_models.title" do
      find("tr", text: compatible_model_1.name).find("button").click
    end
    within id: "pool.model.compatible_models.title" do
      click_on "Select model"
    end
    fill_in_command_field("Search model", compatible_model_3.product)
    within find("[data-test-id='compatible-models-list']") do
      find("[data-value='#{compatible_model_3.name}']").click
    end

    within id: "pool.model.model_properties.title" do
      first("tr button").click
      fill_in "properties.0.key", with: second_property_key_new
      fill_in "properties.0.value", with: second_property_value_new
      click_on "Add property"
      fill_in "properties.1.key", with: third_property_key_new
      fill_in "properties.1.value", with: third_property_value_new
    end

    binding.pry
    click_on "Save"

    expect(page).to have_content "Inventory List"
    select_value("with_items", "without_items")
    fill_in "search", with: "#{product_new} #{version_new}"
    find("a", text: "edit").click

    assert_field("Product", product_new)
    assert_field("Version", version_new)
    expect(find_field("Manufacturer").value).to eq manufacturer_new
    assert_field("Description", description_new)
    assert_field("Technical Details", technical_details_new)
    assert_field("Internal Description", internal_description_new)
    assert_field("Important notes for hand over", hand_over_note_new)

    expect(page).not_to have_selector("tr", text: entitlement_group_1.name)
    expect(find("tr", text: entitlement_group_2.name).find("input").value).to eq "2"
    expect(find("tr", text: entitlement_group_3.name).find("input").value).to eq "1"

    within id: "pool.model.categories.title" do
      expect(current_scope).not_to have_selector("tr", text: leaf_category_1_1_1.name)
      cat_2 = find("tr", text: parent_category_2_1.name)
      cat_2.find("li", text: "#{root_category_2.name} / #{parent_category_2_1.name}")
      cat_3 = find("tr", text: parent_category_1_1.name)
      cat_3.find("li", text: "#{root_category_1.name} / #{parent_category_1_1.name}")
    end

    within id: "pool.model.images.title" do
      expect(page).not_to have_selector("tr", text: image_name_1)
      find("tr", text: image_name_2)
      assert_checked find("tr", text: image_name_2).find("button[role='radio']")
      find("tr", text: image_name_3)
    end

    within id: "pool.model.attachments.title" do
      expect(page).not_to have_selector("tr", text: attachment_name_1)
      find("tr", text: attachment_name_2)
      find("tr", text: attachment_name_3)
    end

    within id: "pool.model.accessories.title" do
      expect(all("tr").count).to eq 2
      assert_field("accessories.0.name", second_accessory_name_new)
      assert_field("accessories.1.name", third_accessory_name_new)
    end

    within id: "pool.model.compatible_models.title" do
      expect(current_scope).not_to have_selector("tr", text: compatible_model_1.name)
      find("tr", text: compatible_model_2.name)
      find("tr", text: compatible_model_3.name)
    end

    within id: "pool.model.model_properties.title" do
      assert_field("properties.0.key", second_property_key_new)
      assert_field("properties.0.value", second_property_value_new)
      assert_field("properties.1.key", third_property_key_new)
      assert_field("properties.1.value", third_property_value_new)
    end
  end
end
