require "spec_helper"
require_relative "../shared/common"

feature "Update entitlement group", type: :feature do
  let(:user) { FactoryBot.create(:user, language_locale: "en-GB") }
  let(:pool) { FactoryBot.create(:inventory_pool) }

  let(:entitlement_group_name_old) { Faker::Sports::Football.team }
  let(:entitlement_group_name_new) { Faker::Sports::Football.team }

  before(:each) do
    @model = FactoryBot.create(:leihs_model)
    @model2 = FactoryBot.create(:leihs_model)

    @entitlement_group = FactoryBot.create(:entitlement_group, inventory_pool: pool, models: [@model])

    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)
  end

  scenario "works" do
    login(user)
    visit "/inventory/#{pool.id}/entitlement-groups/"

    expect(page).to have_content @entitlement_group.name

    within "table" do
      expect(page).to have_selector("tr", text: @entitlement_group.name, visible: true)
      click_on "edit"
    end

    assert_field("Entitlement group name*", @entitlement_group.name)
    within id: "pool.entitlement_groups.entitlement_group.models.title" do
      find("tr", text: "#{@model.product} #{@model.version}")
    end

    fill_in "Entitlement group name*", with: entitlement_group_name_new
    click_on "Select model"
    fill_in_command_field("Search model", @model2.product)
    within find("[data-test-id='models-list']") do
      find("[data-value='#{@model2.product} #{@model2.version}']").click
    end

    within "tr", text: @model.product do
      fill_in "quantity", with: "10"
    end

    within "tr", text: @model2.product do
      fill_in "quantity", with: "5"
    end

    click_on "Save entitlement group"
    expect(page.find("body", visible: :all).text).to include("Entitlement group was successfully saved")

    expect(page).to have_content "Inventory List"
    expect(page).to have_content entitlement_group_name_new

    within "table" do
      expect(page).to have_selector("tr", text: entitlement_group_name_new, visible: true)
      click_on "edit"
    end

    assert_field("Entitlement group name*", entitlement_group_name_new)

    within "tr", text: @model.product do
      assert_field "quantity", "10"
    end

    within "tr", text: @model2.product do
      assert_field "quantity", "5"
    end

    within id: "pool.entitlement_groups.entitlement_group.models.title" do
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
