require 'spec_helper'
require 'pry'

feature 'Manage inventory-pool delegations ', type: :feature do

  context ' an admin, a pool, and several delegations ' do

    def create_delegation(pool)
      delegation = FactoryBot.create(:delegation)
      FactoryBot.create(:direct_access_right,
                        inventory_pool_id: pool.id,
                        user_id: delegation.id,
                        role: 'customer')
      FactoryBot.create(:direct_access_right,
                        inventory_pool_id: pool.id,
                        user_id: delegation.responsible_user.id,
                        role: 'customer')
      delegation
    end

    before :each do
      @admin = FactoryBot.create :admin
      @pool =  FactoryBot.create :inventory_pool
      @delegations = 10.times.map { create_delegation(@pool) }
      @delegation = @delegations.sample
      sign_in_as @admin
    end

    scenario ' managing the suspension of a delegation' do

      # suspend on the delegations table
      click_on 'Inventory Pools'
      click_on @pool.name
      click_on "Delegations"
      select('members and non-members', from: 'Membership')
      fill_in 'Search', with: @delegation.name
      wait_until { all("table tbody tr").count == 1 }
      within("td.suspension"){ click_on 'Edit' }
      expect { page.not.to have_content "Suspended for" }
      fill_in 'suspended_until', with: (Date.today + 1.day).iso8601
      fill_in 'suspended_reason', with: 'Some reason'
      click_on 'Save'

      # suspension info is here and we can cancel suspension
      within("table tbody tr") { click_on(@delegation.name) }
      wait_until { page.has_content? "Suspended for" }
      wait_until { page.has_content? "Some reason" }
      within("#suspension") { click_on "Edit" }
      wait_until{ not all(".modal").empty? }
      click_on "Reset suspension"
      click_on "Save"
      wait_until{ all(".modal").empty? }
      wait_until { page.has_content? "Not suspended" }

      # suspend again from here
      within("#suspension") { click_on "Edit" }
      wait_until{ not all(".modal").empty? }
      fill_in 'suspended_until', with: (Date.today + 1.day).iso8601
      fill_in 'suspended_reason', with: 'Some reason'
      click_on 'Save'
      wait_until{ all(".modal").empty? }
      wait_until { page.has_content? "Suspended for" }

      # suspend forever and test suspension filter
      within("#suspension") { click_on "Edit" }
      wait_until{ not all(".modal").empty? }
      fill_in 'suspended_until', with: (Date.today + 100.years).iso8601
      click_on 'Save'
      wait_until{ all(".modal").empty? }
      wait_until { page.has_content? "Suspended forever" }
    end
  end
end
