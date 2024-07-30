require 'spec_helper'
require 'pry'



feature 'Manage inventory-pool users ', type: :feature do

  context ' an admin, a pool, and  several users ' do

    before :each do
      @admin = FactoryBot.create :admin
      @pool =  FactoryBot.create :inventory_pool
      @users = 10.times.map{ FactoryBot.create :user }
      sign_in_as @admin
    end

    scenario ' managing roles of a user' do
      @user = @users.first

      click_on 'Inventory Pools'
      click_on @pool.name
      within('.nav-tabs') { click_on "Users" }
      select 'any', from: 'Role'
      fill_in 'Search', with: @user.email
      wait_until { all("table tbody tr").count == 1 }
      expect(page.find("table")).not_to have_content "customer"
      expect(page.find("table")).not_to have_content "inventory_manager"

      within_first("td.direct-roles", text: 'Edit'){ click_on 'Edit' }
      wait_until{ not all(".modal").empty? }
      check "inventory_manager"
      click_on "Save"

      # check on user page
      click_on_first_user(@user)
      within ".effective-roles" do
        ['customer', 'group_manager', 'lending_manager', 'inventory_manager'].each do |role|
          expect(find_field(role, disabled: true)).to be_checked
        end
      end
      within ".direct-roles" do
        ['customer', 'group_manager', 'lending_manager', 'inventory_manager'].each do |role|
          expect(find_field(role, disabled: true)).to be_checked
        end
      end

      # check on users page
      within("ol.breadcrumb") do
        find("[data-test-id='#{@pool.name}']").click
      end

      # test filtering by role:
      select 'inventory_manager', from: 'Role'
      wait_until { all("table tbody tr").count == 1 }
      ['customer', 'group_manager', 'lending_manager', 'inventory_manager'].each do |role|
        expect(page).to have_field(role, disabled: true, checked: true)
      end


      # now change the role to lending_manager
      within("td.direct-roles"){ click_on 'Edit' }
      wait_until{ not all(".modal").empty? }
      uncheck 'inventory_manager'
      check 'lending_manager'
      click_on "Save"

      # check on user page
      select 'lending_manager', from: 'Role'
      click_on_first_user(@user)
      within ".direct-roles" do
        ['customer', 'group_manager', 'lending_manager'].each do |role|
          expect(find_field(role, disabled: true)).to be_checked
        end
      end

      #check on users page
      within("ol.breadcrumb") do
        find("[data-test-id='#{@pool.name}']").click
      end

      select 'any', from: 'Role'
      fill_in 'Search', with: @users.first.email
      wait_until { all("table tbody tr").count == 1 }
      ['customer', 'group_manager', 'lending_manager'].each do |role|
        expect(page).to have_field(role, disabled: true, checked: true)
      end

      expect(find("table")).to have_content("customer")
      expect(find("table")).to have_content("group_manager")
      expect(find("table")).to have_content("lending_manager")
      expect(find("table")).not_to have_content("inventory_manager")

      # remove all roles
      within("table td.direct-roles") { click_on 'Edit' }
      wait_until{ not all(".modal").empty? }
      uncheck 'customer'
      click_on "Save"
      wait_until{ all(".modal").empty? }
      wait_until do
        not find("table").has_content? "customer"
      end

    end
  end
end





