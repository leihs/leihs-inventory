require "features_helper"
require_relative "../shared/common"

feature "Model transportable", type: :feature do
  let(:user) { FactoryBot.create(:user, language_locale: "en-GB") }
  let(:pool) { FactoryBot.create(:inventory_pool) }

  before(:each) do
    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)
  end

  def open_new_model
    visit "/inventory/#{pool.id}/list"
    click_on "Add inventory"
    click_on "New model"
  end

  def open_new_software
    visit "/inventory/#{pool.id}/list"
    click_on "Add inventory"
    click_on "New software"
  end

  def assert_transportable_visible_and_enabled_default
    transportable = find("[data-id='transportable']")
    assert_checked transportable
    expect(transportable[:disabled]).not_to eq("true")
    expect(transportable["aria-disabled"]).not_to eq("true")
  end

  context "when alternative pickup locations are enabled" do
    before(:each) { pool.update(enable_alternative_pickup_locations: true) }

    scenario "shows transportable visible and checked by default on new model" do
      login(user)
      open_new_model
      assert_transportable_visible_and_enabled_default
    end

    scenario "shows transportable visible and checked by default on package model" do
      login(user)
      open_new_model
      click_on "this is a package"
      assert_checked find("[data-id='is-package']")
      assert_transportable_visible_and_enabled_default

      package_model = FactoryBot.create(:package_model, transportable: true)
      visit "/inventory/#{pool.id}/models/#{package_model.id}"
      assert_transportable_visible_and_enabled_default
    end

    scenario "shows transportable visible and checked by default on software" do
      login(user)
      open_new_software
      assert_transportable_visible_and_enabled_default

      software = FactoryBot.create(:leihs_model, type: "Software", transportable: true)
      visit "/inventory/#{pool.id}/software/#{software.id}"
      assert_transportable_visible_and_enabled_default
    end
  end

  context "when alternative pickup locations are disabled" do
    before(:each) do
      expect(pool.enable_alternative_pickup_locations).to eq(false)
    end

    scenario "does not show transportable on model, package model, or software" do
      login(user)
      open_new_model
      expect(page).to have_no_css("[data-id='transportable']")

      click_on "this is a package"
      expect(page).to have_no_css("[data-id='transportable']")

      model = FactoryBot.create(:leihs_model)
      visit "/inventory/#{pool.id}/models/#{model.id}"
      expect(page).to have_no_css("[data-id='transportable']")

      package_model = FactoryBot.create(:package_model)
      visit "/inventory/#{pool.id}/models/#{package_model.id}"
      expect(page).to have_no_css("[data-id='transportable']")

      open_new_software
      expect(page).to have_no_css("[data-id='transportable']")

      software = FactoryBot.create(:leihs_model, type: "Software")
      visit "/inventory/#{pool.id}/software/#{software.id}"
      expect(page).to have_no_css("[data-id='transportable']")
    end
  end
end
