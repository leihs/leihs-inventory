require "spec_helper"
require_relative "../shared/common"

feature "Create entitlement group", type: :feature do
  let(:user) { FactoryBot.create(:user, language_locale: "en-GB") }
  let(:pool) { FactoryBot.create(:inventory_pool) }

  let(:entitlement_group_name) { Faker::Sports::Football.team }

  before(:each) do
    @model = FactoryBot.create(:leihs_model)
    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)
    FactoryBot.create(:user, firstname: "Anna", lastname: "Meier", email: "anna@meier.tld")
    FactoryBot.create(:group, name: "Agruppe")
  end

  scenario "works" do
    login(user)
    visit "/inventory/#{pool.id}/entitlement-groups/"
    expect(page).to have_content "No entitlement groups found"
    click_on "New entitlement group"

    fill_in "Entitlement group name*", with: entitlement_group_name

    click_on "Select model"
    fill_in "models-input", with: @model.product
    within find("[data-test-id='models-list']") do
      click_on "#{@model.product} #{@model.version}"
    end

    within "tr", text: @model.product do
      fill_in "quantity", with: "10"
    end

    click_on "Select group"
    fill_in_command_field("Search group", "Agruppe")
    within find("[data-test-id='groups-list']") do
      find("[data-value='Agruppe']").click
    end

    click_on "Select user"
    fill_in_command_field("Search user", "Meier")
    within find("[data-test-id='users-list']") do
      find("[data-value='Anna Meier - anna@meier.tld']").click
    end

    click_on "Create"
    expect(page.find("body", visible: :all).text).to include("Entitlement group was successfully created")

    expect(page).to have_content "Inventory List"
    expect(page).to have_content entitlement_group_name

    find("a", text: "edit").click

    assert_field("Entitlement group name*", entitlement_group_name)
    within id: "pool.entitlement_groups.entitlement_group.models.title" do
      find("tr", text: "#{@model.product} #{@model.version}")
    end
    within "tr", text: @model.product do
      assert_field "quantity", "10"
    end
    within id: "pool.entitlement_groups.entitlement_group.groups.title" do
      expect(page).to have_content "Agruppe"
    end
    within id: "pool.entitlement_groups.entitlement_group.users.title" do
      expect(page).to have_content "Anna Meier"
    end
  end

  scenario "fails with invalid mandatory fields" do
    login(user)
    visit "/inventory/#{pool.id}/entitlement-groups/"
    click_on "New entitlement group"

    click_on "Create"
    expect(page.find("body", visible: :all).text).to include("Entitlement group could not be created because one field is invalid")
    expect(page).to have_content "Too small: expected input to have >=1 characters"
  end

  scenario "cancel works" do
    login(user)
    visit "/inventory/#{pool.id}/entitlement-groups/"

    click_on "New entitlement group"

    fill_in "Entitlement group name*", with: entitlement_group_name
    click_on "Select model"
    fill_in "models-input", with: @model.product
    within find("[data-test-id='models-list']") do
      click_on "#{@model.product} #{@model.version}"
    end

    click_on "submit-dropdown"
    click_on "Cancel"

    expect(page).to have_content "Inventory List"
  end
end
