require 'spec_helper'
require 'pry'

feature 'Manage inventory-pool roles of groups', type: :feature do
  context 'an admin, a pool, groups and a lending_manager exist' do
    before :each do
      @admin = FactoryBot.create :admin
      @pool =  FactoryBot.create :inventory_pool
      @manager = FactoryBot.create :user
      FactoryBot.create :access_right, user: @manager,
        inventory_pool: @pool, role: 'lending_manager'
      @groups = 10.times.map{ FactoryBot.create :group}
    end


    context "the lending_manager via the UI" do
      before(:each) { sign_in_as @manager }

      context "visits the roles page of a group" do
        before :each do
          @group = @groups.first
          @group_id = @group[:id]
          @inventory_pool_id = @pool[:id]
          click_on "Inventory Pools"
          click_on @pool.name
          within('.nav-tabs') { 
            click_on "Groups" 
          }
          select 'any', from: 'Role'
          fill_in 'Search', with: @groups.first.name
          wait_until { find("table").has_content?(@group.name) }
          expect(page.find("table")).not_to have_content "customer"
          expect(page.find("table")).not_to have_content "inventory_manager"
          expect(page).to have_selector("table tbody tr", count: 1)
          within find("table tbody tr", text: @group.name) do
            click_on "Edit"
          end
        end

        scenario 'can manage roles up to lending_manager' do
          # set access_right
          expect(page).to have_css('.modal')
          check "lending_manager"
          click_on "Save"
          expect(page).not_to have_css('.modal')
          
          expect(GroupAccessRight.find(inventory_pool_id: @pool[:id], group_id: @group[:id]).role).to eq "lending_manager"
          within find("table tbody tr", text: @group.name) do
            click_on "Edit"
          end

          uncheck :customer
          click_on "Save"
          wait_until do
            GroupAccessRight.find(inventory_pool_id: @inventory_pool_id, group_id: @group_id).nil?
          end
        end

        scenario "can not escalate roles up to inventory_manager" do
          expect(page).to have_css('.modal', text: "Edit roles")
          check "inventory_manager"
          click_on "Save"
          wait_until { page.has_content? "ERROR 403" }
          wait_until do
            GroupAccessRight.find(inventory_pool_id: @inventory_pool_id, group_id: @group_id).nil?
          end
        end
      end
    end
  end
end
