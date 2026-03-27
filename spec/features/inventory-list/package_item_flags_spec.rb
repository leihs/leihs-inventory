require "features_helper"
require_relative "../shared/common"
require "securerandom"

# List subtitle "is part of a package" and edit alert "Item is part of package" only when
# an item has a package parent; isolated negative setup has no package model or package items.
feature "Inventory list and edit: package item flags", type: :feature do
  scenario "no package in pool: list and edit omit package flags" do
    token = "PkgFlagNeg-#{SecureRandom.hex(4)}"
    building = FactoryBot.create(:building, name: "#{token} B", code: "PF1")
    room = FactoryBot.create(:room, name: "#{token} R", building: building)
    pool = FactoryBot.create(:inventory_pool, shortname: "PF")
    user = FactoryBot.create(:user, language_locale: "en-GB")
    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)

    item_model = FactoryBot.create(:leihs_model, product: token, version: "v1")

    item_a = FactoryBot.create(:item,
      inventory_code: "#{pool.shortname}N01",
      owner_id: pool.id,
      inventory_pool_id: pool.id,
      leihs_model: item_model,
      room: room,
      shelf: "S-N01",
      is_borrowable: true,
      retired: nil,
      parent_id: nil)

    FactoryBot.create(:item,
      inventory_code: "#{pool.shortname}N02",
      owner_id: pool.id,
      inventory_pool_id: pool.id,
      leihs_model: item_model,
      room: room,
      shelf: "S-N02",
      is_borrowable: true,
      retired: nil,
      parent_id: nil)

    login(user)

    visit "/inventory/#{pool.id}/list?page=1&size=50&with_items=true&retired=false&in_stock=true"
    find("input[name='search']").set(token)
    await_debounce

    row = find("tr", text: item_model.name)
    within("tr", text: item_model.name) do
      click_on "expand-button"
    end

    item_rows = row.all(:xpath, "following-sibling::tr[@data-row='item']", wait: 30)
    wait_until { item_rows.size == 2 }

    item_rows.each do |r|
      expect(r).not_to have_content("is part of a package")
    end

    within find('[data-row="item"]', text: item_a.inventory_code) do
      click_on "edit"
    end

    expect(page).not_to have_content("Item is part of package")
  end

  scenario "package flags via component model row: list row and edit form" do
    building = FactoryBot.create(:building, name: "PkgViaMod B", code: "VM1")
    room = FactoryBot.create(:room, name: "PkgViaMod R", building: building)
    pool = FactoryBot.create(:inventory_pool, shortname: "VM")
    user = FactoryBot.create(:user, language_locale: "en-GB")
    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)

    pkg_model = FactoryBot.create(:leihs_model, product: "PkgViaModPkg", version: "v1", is_package: true)
    item_model = FactoryBot.create(:leihs_model, product: "PkgViaModItem", version: "v1")

    pkg_parent = FactoryBot.create(:item,
      inventory_code: "#{pool.shortname}PKG",
      owner_id: pool.id,
      inventory_pool_id: pool.id,
      leihs_model: pkg_model,
      room: room,
      shelf: "S-PKG",
      is_borrowable: true,
      retired: nil)

    child_item = FactoryBot.create(:item,
      inventory_code: "VM101",
      owner_id: pool.id,
      inventory_pool_id: pool.id,
      leihs_model: item_model,
      room: room,
      shelf: "S-02",
      is_borrowable: true,
      retired: nil,
      parent_id: pkg_parent.id)

    # Omit in_stock: list query with in_stock=true requires parent_id IS NULL, so this model row
    # would be missing when the only lines are package children.
    list_url = "/inventory/#{pool.id}/list?page=1&size=50&with_items=true&retired=false"

    login(user)

    visit list_url
    find("input[name='search']").set("PkgViaModItem")
    await_debounce

    model_row = find("tr", text: item_model.name)
    within("tr", text: item_model.name) do
      click_on "expand-button"
    end

    model_item_rows = model_row.all(:xpath, "following-sibling::tr[@data-row='item']", wait: 30)
    wait_until { model_item_rows.size == 1 }

    item_row = model_item_rows.first
    expect(item_row).to have_content(child_item.inventory_code)
    expect(item_row).to have_content("is part of a package")

    within find('[data-row="item"]', text: child_item.inventory_code) do
      click_on "edit"
    end
    expect(page).to have_content("Item is part of package")
  end

  scenario "package flags via package model row: list row and edit form" do
    building = FactoryBot.create(:building, name: "PkgViaPcg B", code: "VP1")
    room = FactoryBot.create(:room, name: "PkgViaPcg R", building: building)
    pool = FactoryBot.create(:inventory_pool, shortname: "VP")
    user = FactoryBot.create(:user, language_locale: "en-GB")
    FactoryBot.create(:access_right,
      inventory_pool: pool,
      user: user,
      role: :inventory_manager)

    pkg_model = FactoryBot.create(:leihs_model, product: "PkgViaPcgPkg", version: "v1", is_package: true)
    item_model = FactoryBot.create(:leihs_model, product: "PkgViaPcgItem", version: "v1")

    pkg_parent = FactoryBot.create(:item,
      inventory_code: "#{pool.shortname}PKG",
      owner_id: pool.id,
      inventory_pool_id: pool.id,
      leihs_model: pkg_model,
      room: room,
      shelf: "S-PKG",
      is_borrowable: true,
      retired: nil)

    child_item = FactoryBot.create(:item,
      inventory_code: "VP101",
      owner_id: pool.id,
      inventory_pool_id: pool.id,
      leihs_model: item_model,
      room: room,
      shelf: "S-02",
      is_borrowable: true,
      retired: nil,
      parent_id: pkg_parent.id)

    list_url = "/inventory/#{pool.id}/list?page=1&size=50&with_items=true&retired=false"

    login(user)

    visit list_url
    find("input[name='search']").set("PkgViaPcgPkg")
    await_debounce

    pkg_model_row = find("tr", text: pkg_model.name)
    within("tr", text: pkg_model.name) do
      click_on "expand-button"
    end

    package_rows = pkg_model_row.all(:xpath, "following-sibling::tr[@data-row='package']", wait: 30)
    wait_until { package_rows.size == 1 }

    package_rows.first.find('[data-test-id="expand-button"]').click

    nested = []
    current_row = package_rows.first
    while (next_row = current_row.first(:xpath, "following-sibling::tr[1]", minimum: 0, wait: 0))
      break unless next_row["data-row"] == "item"

      nested << next_row
      current_row = next_row
    end

    wait_until { nested.size == 1 }

    item_row = nested.first
    expect(item_row).to have_content(child_item.inventory_code)
    expect(item_row).to have_content("is part of a package")

    within(item_row) do
      click_on "edit"
    end
    expect(page).to have_content("Item is part of package")
  end
end
