require 'spec_helper'
require 'pry'

feature 'Manage inventory-pool users ', type: :feature do

  context ' an admin, a pool, an entitlement-group, several users, each within at least one group' do

    before :each do
      @admin = FactoryBot.create :admin
      @pool =  FactoryBot.create :inventory_pool
      @entitlement_group = FactoryBot.create :entitlement_group, inventory_pool_id: @pool.id
      @users = 100.times.map{ FactoryBot.create :user }
      @groups = 3.times.map { FactoryBot.create :group }
      @users.each do |user|
        @groups.shuffle.take(2).each do |group|
          database[:groups_users].insert(
            group_id: group[:id],
            user_id: user[:id])
        end
      end
    end

    shared_examples :manage_membership_directly do
      scenario 'manage membership via direct membership' do
        visit '/admin/'
        click_on 'Inventory Pools'
        click_on @pool.name
        within('.nav-tabs') { click_on 'Entitlement Groups' }
        @entitlement_groups_path = current_path
        click_on @entitlement_group.name
        within('.nav-tabs') { click_on 'Users' }
        select 'members and non-members', from: 'Membership'
        click_on_first 'next' # go to the second page because we want also test some internal indexing complexity
        within(first 'tr.user') do
          @email = find("td.user").text.split(/\s/).last
          expect(find_field("member", disabled: true)).not_to be_checked
          within("td.direct-member") { click_on 'Add' }
          wait_until { find_field("member", disabled: true).checked? }
        end
        @list_page = current_url

        visit @entitlement_groups_path
        fill_in 'including-user', with: "Foo.Bar@baz"
        wait_until { page.has_content? "No (more) entitlement-groups found." }
        expect(page).not_to have_content  @entitlement_group.name
        fill_in 'including-user', with: @email
        wait_until { page.has_content? @entitlement_group.name }

        visit @list_page
        within(first 'tr.user') do
          click_on 'Remove'
          wait_until { not find_field("member", disabled: true).checked? }
        end
      end
    end


    shared_examples :manage_membership_via_group do
      scenario 'manage membership via group membership' do
        visit '/admin/'
        user = @users.sample
        click_on 'Inventory Pools'
        click_on @pool.name
        click_on 'Entitlement Groups'
        @entitlement_groups_path = current_path
        click_on @entitlement_group.name
        within('.nav-tabs') { click_on 'Users' }
        select 'members and non-members', from: 'Membership'
        fill_in 'Search', with: user.email
        wait_until{ all('tr.user').count == 1 }
        user_on_users_page = current_url
        within(find('tr.user')) do
          expect(find_field("member", disabled: true)).not_to be_checked
          within('td.group-member') {click_on 'Add'}
        end
        click_on_first 'Add'
        wait_until { all('.modal').empty? }
        visit user_on_users_page
        # now the user is a member, wait_until because we might see stale cached data briefly
        wait_until { find_field('member', disabled: true).checked? }

        # test including-user filter
        visit @entitlement_groups_path
        fill_in 'including-user', with: "Foo.Bar@baz"
        wait_until { page.has_content? "No (more) entitlement-groups found." }
        expect(page).not_to have_content  @entitlement_group.name
        fill_in 'including-user', with: user.email
        wait_until { page.has_content? @entitlement_group.name }

        visit user_on_users_page
        within('td.group-member') { click_on 'Edit' }
        click_on 'Remove'
        wait_until { all('.modal').empty? }
        visit user_on_users_page
        # now the users is no member anymore
        wait_until { not find_field('member', disabled: true).checked? }
      end
    end

    context 'as an admin via the UI' do
      before :each do
        sign_in_as @admin
      end
      include_examples :manage_membership_directly
      include_examples :manage_membership_via_group
    end

    context 'as lending_manager via the UI' do
      before :each do
        @manager = FactoryBot.create :user
        FactoryBot.create :access_right, user: @manager,
          inventory_pool: @pool, role: 'lending_manager'
        sign_in_as @manager
      end
      include_examples :manage_membership_directly
      include_examples :manage_membership_via_group
    end

  end
end

