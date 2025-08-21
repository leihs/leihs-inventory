require "spec_helper"
require_relative "../shared/common"

feature "Create template", type: :feature do
  let(:user) { FactoryBot.create(:user, language_locale: "en-GB") }
  let(:pool) { FactoryBot.create(:inventory_pool) }

  let(:template_name) { Faker::Commerce.product_name }

  before(:each) do
    @model = FactoryBot.create(:leihs_model)
    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)
  end

  scenario "works" do
    login(user)
    visit "/inventory/#{pool.id}/templates"
    click_on "New template"

    fill_in "Template name*", with: template_name
    click_on "Select model"
    fill_in_command_field("Search model", @model.product)
    within find("[data-test-id='models-list']") do
      find("[data-value='#{@model.product} #{@model.version}']").click
    end

    within "tr", text: @model.product do
      fill_in "quantity", with: "10"
    end

    click_on "Save template"
    expect(page.find("body", visible: :all).text).to include("Template was successfully created")

    expect(page).to have_content "Inventory List"
    expect(page).to have_content template_name

    find("a", text: "edit").click

    assert_field("Template name*", template_name)
    within id: "pool.templates.template.models.title" do
      find("tr", text: "#{@model.product} #{@model.version}")
    end

    within "tr", text: @model.product do
      assert_field "quantity", "10"
    end
  end

  scenario "fails with invalid mandatory fields" do
    login(user)
    visit "/inventory/#{pool.id}/templates"
    click_on "New template"

    click_on "Save"
    expect(page.find("body", visible: :all).text).to include("Template could not be created because 2 fields are invalid")
    expect(page).to have_content "Too small: expected input to have >=1 characters"
    expect(page).to have_content "Too small: expected input to have >=1 characters"
  end

  scenario "cancel works" do
    login(user)
    visit "/inventory/#{pool.id}/templates"

    click_on "New template"

    fill_in "Template name*", with: template_name
    click_on "Select model"
    fill_in_command_field("Search model", @model.product)
    within find("[data-test-id='models-list']") do
      find("[data-value='#{@model.product} #{@model.version}']").click
    end

    click_on "submit-dropdown"
    click_on "Cancel"

    expect(page).to have_content "Inventory List"
  end
end
