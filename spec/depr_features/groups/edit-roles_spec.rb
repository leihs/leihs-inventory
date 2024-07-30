require 'spec_helper'
require 'pry'

feature 'Editing roles ', type: :feature do
  context 'some admins, a bunch of users and a bunch of groups exist' do
    before :each do
      @admins = 3.times.map { FactoryBot.create :admin }
      @system_admins = 3.times.map { FactoryBot.create :system_admin}
      @user = FactoryBot.create :user
      @pool =  FactoryBot.create :inventory_pool
      @users = 15.times.map { FactoryBot.create :user }
      @group =  FactoryBot.create :group

      FactoryBot.create :group_access_right, group_id: @group.id,
        inventory_pool_id: @pool.id, role: 'customer'
    end


    context 'an system-admin via the UI' do
      before :each do
        @admin = @system_admins.sample
        sign_in_as @admin
      end

      scenario 'edits a system-admin protected group ' do
        visit '/admin/'
        click_on 'Groups'
        click_on @group.name

        within find("table tbody tr", text: @pool.name) do
          find("button", text: "Edit").click
        end

        expect(page).to have_selector(".modal")

        expect(page).to have_css(
          '.modal .fade.alert.alert-danger', 
          text: "Users will be affected!"
        )

        # set access_right
        check "inventory_manager"
        click_on "Save"
        expect(GroupAccessRight.find(inventory_pool_id: @pool[:id], group_id: @group[:id]).role).to eq "inventory_manager"

        # remove all access_rights
        within("table tbody tr", text: @pool.name) do
          click_on "Edit"
        end
        wait_until{ not all(".modal").empty? }
        uncheck :customer
        click_on "Save"
        expect(GroupAccessRight.find(inventory_pool_id: @pool[:id], group_id: @group[:id])).to be_nil
      end
    end
  end
end
