require "features_helper"
require_relative "../shared/common"

feature "Software license sub-row on inventory list", type: :feature do
  def expand_license_row(software_model, license)
    find("input[name='search']").set(software_model.product)
    await_debounce

    within find('[data-row="model"]', text: software_model.product, wait: 10) do
      click_on "expand-button"
    end

    find('[data-row="item"]', text: license.inventory_code, wait: 10)
  end

  scenario "shows default license type label (free) and hides pool when owner matches responsible pool" do
    user = FactoryBot.create(:user, language_locale: "en-GB")
    pool = FactoryBot.create(:inventory_pool, name: "PoolSameOwnerResp")
    room = FactoryBot.create(:room)

    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)

    software_model = FactoryBot.create(:leihs_model,
      product: "SoftwareDefaultLicenseType",
      version: "1.0",
      type: "Software")

    license = FactoryBot.create(:item,
      leihs_model: software_model,
      inventory_pool: pool,
      owner: pool,
      room: room,
      properties: {
        license_type: "free"
      })

    login(user)
    visit "/inventory/#{pool.id}/list"
    select_value("with_items", "with_items")

    item_row = expand_license_row(software_model, license)

    within item_row do
      expect(page).to have_content("Free")
    end
  end

  scenario "shows multiple workplace and all operating system labels" do
    user = FactoryBot.create(:user, language_locale: "en-GB")
    pool = FactoryBot.create(:inventory_pool)
    room = FactoryBot.create(:room)

    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)

    software_model = FactoryBot.create(:leihs_model,
      product: "SoftwareMultiOs",
      version: "2.0",
      type: "Software")

    license = FactoryBot.create(:item,
      leihs_model: software_model,
      inventory_pool: pool,
      owner: pool,
      room: room,
      properties: {
        license_type: "multiple_workplace",
        operating_system: %w[windows linux mac_os_x ios]
      })

    login(user)
    visit "/inventory/#{pool.id}/list"
    select_value("with_items", "with_items")

    item_row = expand_license_row(software_model, license)

    within item_row do
      expect(page).to have_content("Multiple Workplace")
      expect(page).to have_content("Windows")
      expect(page).to have_content("Linux")
      expect(page).to have_content("Mac OS X")
      expect(page).to have_content("iOS")
    end
  end

  scenario "shows responsible pool name when owner differs from responsible pool" do
    user = FactoryBot.create(:user, language_locale: "en-GB")
    owner_pool = FactoryBot.create(:inventory_pool, name: "OwnerDepartmentPool")
    responsible_pool = FactoryBot.create(:inventory_pool, name: "ResponsibleDepartmentPool")
    room = FactoryBot.create(:room)

    FactoryBot.create(:access_right,
      inventory_pool: responsible_pool,
      user: user,
      role: :inventory_manager)

    software_model = FactoryBot.create(:leihs_model,
      product: "SoftwareDelegatedResp",
      version: "3.0",
      type: "Software")

    license = FactoryBot.create(:item,
      leihs_model: software_model,
      inventory_pool: responsible_pool,
      owner: owner_pool,
      room: room,
      properties: {
        license_type: "single_workplace"
      })

    login(user)
    visit "/inventory/#{responsible_pool.id}/list"
    select_value("with_items", "with_items")

    item_row = expand_license_row(software_model, license)

    within item_row do
      expect(page).to have_content(responsible_pool.name)
      expect(page).not_to have_content(owner_pool.name)
    end
  end

  scenario "shows operating system, responsible pool, and license type when owner differs from responsible pool" do
    user = FactoryBot.create(:user, language_locale: "en-GB")
    owner_pool = FactoryBot.create(:inventory_pool, name: "OwnerPoolCombined")
    responsible_pool = FactoryBot.create(:inventory_pool, name: "ResponsiblePoolCombined")
    room = FactoryBot.create(:room)

    FactoryBot.create(:access_right,
      inventory_pool: responsible_pool,
      user: user,
      role: :inventory_manager)

    software_model = FactoryBot.create(:leihs_model,
      product: "SoftwareCombinedRow",
      version: "4.0",
      type: "Software")

    license = FactoryBot.create(:item,
      leihs_model: software_model,
      inventory_pool: responsible_pool,
      owner: owner_pool,
      room: room,
      properties: {
        license_type: "site_license",
        operating_system: %w[linux windows mac_os_x ios]
      })

    login(user)
    visit "/inventory/#{responsible_pool.id}/list"
    select_value("with_items", "with_items")

    item_row = expand_license_row(software_model, license)

    within item_row do
      expect(page).to have_content(responsible_pool.name)
      expect(page).not_to have_content(owner_pool.name)
      expect(page).to have_content("Site License")
      expect(page).to have_content("Windows")
      expect(page).to have_content("Linux")
      expect(page).to have_content("Mac OS X")
      expect(page).to have_content("iOS")
    end
  end
end
