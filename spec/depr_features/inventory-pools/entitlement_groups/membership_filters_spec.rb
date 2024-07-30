require 'spec_helper'
require 'pry'




feature 'Entitlement-group Membership filtering ', type: :feature do

  context ' an admin, a pool, an entitlement-group, several users, each within at least one group' do

    before :each do
      @admin = FactoryBot.create :admin
      @manager = FactoryBot.create :user

      @pool =  FactoryBot.create :inventory_pool
      @entitlement_group = FactoryBot.create :entitlement_group, inventory_pool_id: @pool.id
      100.times.map{ FactoryBot.create :user }
      @groups = 3.times.map { FactoryBot.create :group }

      @users = database[:users].to_set

      @users.each do |user|
        @groups.shuffle.take(2).each do |group|
          database[:groups_users].insert(
            group_id: group[:id],
            user_id: user[:id])
        end
      end

      @direct_members = @users.to_a.shuffle.take((@users.count/2.0).floor).to_set
      @direct_members.each do |user|
        database[:entitlement_groups_direct_users].insert(
          entitlement_group_id: @entitlement_group.id,
          user_id: user[:id])
      end

      @member_group = @groups.sample
      database[:entitlement_groups_groups].insert(
        entitlement_group_id: @entitlement_group.id,
        group_id: @member_group.id)

      @group_members = database[:users].inner_join(:groups_users, user_id: :id) \
        .where(group_id: @member_group.id).select_all(:users).to_set

      @members = @direct_members + @group_members
      @non_members = @users - @members

    end


    shared_examples :filter_membership do
      scenario 'Filter membership' do
        visit '/admin/'
        click_on 'Inventory Pools'
        click_on @pool.name
        click_on 'Entitlement Groups'
        click_on @entitlement_group.name
        within('.nav-tabs') { click_on 'Users' }

        select '1000', from: 'Per page'

        select 'members and non-members', from: 'Membership'
        wait_until { all("tr .user").count == @users.count }

        # direct_members filter
        select 'direct members', from:  'Membership'
        wait_until { all("tr .user").count == @direct_members.count }

        select 'members and non-members', from: 'Membership'
        wait_until { all("tr .user").count == @users.count }

        # group_members filter
        select 'group members', from:  'Membership'
        wait_until { all("tr .user").count == @group_members.count }

        select 'members and non-members', from: 'Membership'
        wait_until { all("tr .user").count == @users.count }

        # non member filter
        select 'non-members', from:  'Membership'
        wait_until { all("tr .user").count == @non_members.count }

      end
    end


    context 'as an admin via the UI' do
      before(:each){ sign_in_as @admin }
      include_examples :filter_membership
    end

    context 'as lending_manager via the UI' do
      before :each do
        FactoryBot.create :access_right, user: @manager,
          inventory_pool: @pool, role: 'lending_manager'
        sign_in_as @manager
      end
      include_examples :filter_membership
    end

  end
end
