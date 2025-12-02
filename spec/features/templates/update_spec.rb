require "spec_helper"
require_relative "../shared/common"

feature "Update template", type: :feature do
  let(:user) { FactoryBot.create(:user, language_locale: "en-GB") }
  let(:pool) { FactoryBot.create(:inventory_pool) }

  let(:template_name_old) { Faker::Commerce.product_name }
  let(:template_name_new) { Faker::Commerce.product_name }

  before(:each) do
    @model = FactoryBot.create(:leihs_model)
    @model2 = FactoryBot.create(:leihs_model)

    @template = FactoryBot.create(:template, inventory_pool: pool)
    @template.add_direct_model(@model)

    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)
  end

  scenario "works" do
    login(user)
    visit "/inventory/#{pool.id}/templates"

    expect(page).to have_content @template.name

    within "table" do
      expect(page).to have_selector("tr", text: @template.name, visible: true)
      expect(page).to have_selector("tr", text: "Quantity exceeds availability", visible: true)
      expect(page).to have_selector("tr", text: "1", visible: true)
      click_on "edit"
    end

    assert_field("Template name*", @template.name)
    within id: "pool.templates.template.models.title" do
      find("tr", text: "#{@model.product} #{@model.version}")
    end

    fill_in "Template name*", with: template_name_new

    click_on "Select model"
    fill_in "models-input", with: "#{@model2.product} #{@model2.version}"
    within find("[data-test-id='models-list']") do
      click_on "#{@model2.product} #{@model2.version}"
    end

    within "tr", text: @model.product do
      fill_in "quantity", with: "10"
    end

    within "tr", text: @model2.product do
      fill_in "quantity", with: "5"
    end

    click_on "Save"
    expect(page).to have_text("Template was successfully saved")

    expect(page).to have_content "Inventory List"
    expect(page).to have_content template_name_new

    within "table" do
      expect(page).to have_selector("tr", text: template_name_new, visible: true)
      expect(page).to have_selector("tr", text: "Quantity exceeds availability", visible: true)
      expect(page).to have_selector("tr", text: "2", visible: true)
      click_on "edit"
    end

    assert_field("Template name*", template_name_new)

    within "tr", text: @model.product do
      assert_field "quantity", "10"
    end

    within "tr", text: @model2.product do
      assert_field "quantity", "5"
    end

    within id: "pool.templates.template.models.title" do
      find("tr", text: "#{@model.product} #{@model.version}")
      find("tr", text: "#{@model2.product} #{@model2.version}")
    end

    within "tr", text: @model.product do
      assert_field "quantity", "10"
    end

    within "tr", text: @model2.product do
      assert_field "quantity", "5"
    end
  end
end
