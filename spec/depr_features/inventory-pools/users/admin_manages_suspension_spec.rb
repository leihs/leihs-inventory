require 'spec_helper'
require 'pry'


feature 'Manage inventory-pool users ', type: :feature do

  context ' an admin, a pool, and  several users ' do

    before :each do
      @admin = FactoryBot.create :admin
      @pool =  FactoryBot.create :inventory_pool
      @users = 10.times.map{ FactoryBot.create :user }
      @user = @users.sample
      sign_in_as @admin
    end

    context ' managing the suspension of a user' do

      scenario 'on the users table' do
        # suspend on the users table
        click_on 'Inventory Pools'
        click_on @pool.name
        within('.nav-tabs') { click_on "Users" }
        select 'any', from: 'Role'
        fill_in 'Search', with: @user.email
        expect(page).to have_selector("table tbody tr", count: 1)

        within("td.suspension"){ click_on 'Edit' }
        expect { page.not.to have_content "Suspended for" }
        fill_in 'suspended_until', with: (Date.today + 1.day).iso8601
        fill_in 'suspended_reason', with: 'Some reason'
        click_on 'Save'
        wait_until { page.has_content? "Suspended for" }
        expect(database[:suspensions].where(user_id: @user.id).first).to be
        within("tbody tr"){ click_on 'Reset' }
        wait_until { page.has_content? "Not suspended" }
        expect(database[:suspensions].where(user_id: @user.id).first).not_to be
      end

      scenario "on the inventory-pool-user's page" do
        click_on 'Inventory Pools'
        click_on @pool.name
        within('.nav-tabs') { click_on "Users" }
        select 'any', from: 'Role'
        fill_in 'Search', with: @user.email
        expect(page).to have_selector("table tbody tr", count: 1)

        click_on_first_user @user
        expect(page).to have_content("Not suspended")

        within("#suspension") do
          click_on "Edit" 
        end

        expect(page).to have_css(".modal" , text: "Edit Suspension") 

        fill_in 'suspended_until', with: (Date.today + 1.day).iso8601
        fill_in 'suspended_reason', with: 'Some reason'
        click_on 'Save'
        expect(page).not_to have_selector(".modal")
        expect(page).to have_content "Suspended for" 

        expect(database[:suspensions].where(user_id: @user.id).first).to be

        # reset within modal
        within("#suspension"){ click_on "Edit" }
        wait_until{ not all(".modal").empty? }
        click_on 'Reset suspension'
        click_on 'Save'
        wait_until{ all(".modal").empty? }
        wait_until { page.has_content? "Not suspended" }
        expect(database[:suspensions].where(user_id: @user.id).first).not_to be


        # Quick reset from page
        within("#suspension"){ click_on "Edit" }
        wait_until{ not all(".modal").empty? }
        fill_in 'suspended_until', with: (Date.today + 1.day).iso8601
        fill_in 'suspended_reason', with: 'Some reason'
        click_on 'Save'
        wait_until{ all(".modal").empty? }
        wait_until { page.has_content? "Suspended for" }
        expect(database[:suspensions].where(user_id: @user.id).first).to be
        within("#suspension"){ click_on "Reset" }
        wait_until{ all(".modal").empty? }
        wait_until { page.has_content? "Not suspended" }
        expect(database[:suspensions].where(user_id: @user.id).first).not_to be
      end

      scenario' suspend forever and test suspension filter' do
        # suspend on the users table
        click_on 'Inventory Pools'
        expect(page).to have_content(@pool.name)
        click_on @pool.name
        within('.nav-tabs') { click_on "Users" }
        select 'any', from: 'Role'
        fill_in 'Search', with: @user.email
        expect(page).to have_selector("table tbody tr", count: 1)
        within("td.suspension"){ click_on "Edit" }

        expect(page).to have_content('suspended_until')
        fill_in 'suspended_until', with: (Date.today + 100.years).iso8601
        click_on 'Save'
        expect(page).not_to have_selector(".modal")

        select 'any', from: 'Role'
        select('suspended', from: 'Suspension')
        expect(page).to have_selector("table tbody tr", count: 1)

        expect(page.find("table")).to have_content 'forever'
      end
    end
  end
end
