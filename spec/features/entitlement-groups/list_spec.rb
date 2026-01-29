require "features_helper"
require_relative "../shared/common"

feature "List entitlement groups", type: :feature do
  let(:user) { FactoryBot.create(:user, language_locale: "en-GB") }
  let(:pool) { FactoryBot.create(:inventory_pool) }

  before(:each) do
    @model1 = FactoryBot.create(:leihs_model)
    @model2 = FactoryBot.create(:leihs_model)
    @user1 = FactoryBot.create(:user, firstname: "Anna", lastname: "Meier", email: "anna@meier.tld")
    @user2 = FactoryBot.create(:user, firstname: "Bert", lastname: "Beier", email: "bert@beier.tld")
    @group1 = FactoryBot.create(:group, name: "Agruppe")
    @group1.add_user(@user1)

    @entitlement_group1 = FactoryBot.create(:entitlement_group,
      inventory_pool: pool,
      models: [{model: @model1, quantity: 3}],
      groups: [@group1],
      users: [@user2])
    @entitlement_group2 = FactoryBot.create(:entitlement_group,
      inventory_pool: pool,
      models: [{model: @model2, quantity: 0}],
      is_verification_required: true)

    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)
  end

  scenario "displays correct values in table columns" do
    login(user)
    visit "/inventory/#{pool.id}/entitlement-groups/"

    within "table" do
      expect(page).to have_selector("tr", text: @entitlement_group1.name, visible: true)
    end

    within find("tr", text: @entitlement_group1.name, visible: true) do
      # Status indicator (is_quantity_ok)
      status_cell = find("td[data-test-id='is_quantity_ok']")
      expect(status_cell[:title]).to eq("Quantity exceeds availability")

      # Name
      expect(page).to have_content(@entitlement_group1.name)

      # Verification required
      expect(page).not_to have_content("Verification required")

      # Number of users
      users_cell = find("td[data-test-id='number_of_users']")
      expect(users_cell.text).to eq "2"

      # Number of models
      models_cell = find("td[data-test-id='number_of_models']")
      expect(models_cell.text).to eq "1"

      # Number of allocations
      allocations_cell = find("td[data-test-id='number_of_allocations']")
      expect(allocations_cell.text).to eq "3"
    end

    within find("tr", text: @entitlement_group2.name, visible: true) do
      # Status indicator (is_quantity_ok)
      status_cell = find("td[data-test-id='is_quantity_ok']")
      expect(status_cell[:title]).not_to eq("Quantity exceeds availability")

      # Name
      expect(page).to have_content(@entitlement_group2.name)

      # Verification required
      expect(page).to have_content("Verification required")

      # Number of users
      users_cell = find("td[data-test-id='number_of_users']")
      expect(users_cell.text).to eq "0"

      # Number of models
      models_cell = find("td[data-test-id='number_of_models']")
      expect(models_cell.text).to eq "1"

      # Number of allocations
      allocations_cell = find("td[data-test-id='number_of_allocations']")
      expect(allocations_cell.text).to eq "0"
    end
  end
end
