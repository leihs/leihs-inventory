require 'spec_helper'
require 'pry'

shared_examples :direct_user do
  scenario 'adding and removing a direct user' do
    @user = @users.find { |u| u.id != @delegation.delegator_user_id }
    click_on 'Inventory Pools'
    click_on @pool.name
    click_on 'Delegations'
    @delegations_path = current_path
    click_on @delegation.firstname
    @delegation_path = current_path
    within('.nav-tabs') { click_on 'Users' }
    select 'members and non-members', from: 'Membership'
    fill_in 'Search', with: @user.email
    wait_until{ all('.delegation table tbody tr').count == 1 }
    @delegation_users_url = current_url
    within(find('tr.user'))do
      expect(find(:checkbox, id: 'member', disabled: true)).not_to be_checked
      expect(find(:checkbox, id: 'direct_member', disabled: true)).not_to be_checked
      expect(find(:checkbox, id: 'group_member', disabled: true)).not_to be_checked
    end
    within("tr td.direct-member"){ click_on 'Add' }
    wait_until{ first("tr.user td.direct-member", text: 'Remove') }
    within(find('tr.user'))do
      expect(find(:checkbox, id: 'member', disabled: true)).to be_checked
      expect(find(:checkbox, id: 'direct_member', disabled: true)).to be_checked
      expect(find(:checkbox, id: 'group_member', disabled: true)).not_to be_checked
    end

    visit @delegation_path
    within("tr", text: "Number of users"){ expect(find("td.users-count").text).to eq "2"}
    within("tr", text: "Number of direct users"){ expect(find("td.direct-users-count").text).to eq "2"}
    within("tr", text: "Number of groups"){ expect(find("td.groups-count").text).to eq "0"}

    # test including-user filter
    visit @delegations_path
    fill_in "including-user", with: "foo.bar@baz"
    wait_until{ page.has_content? "No (more) delegations found." }
    expect(page).not_to have_content @delegation.firstname
    wait_until{ not page.has_content? @delegation.firstname }
    fill_in "including-user", with: @user.email
    wait_until{ page.has_content? @delegation.firstname }

    # remove user again
    visit @delegation_users_url
    wait_until{ first("tr.user td.direct-member", text: 'Remove') }
    within("tr.user td.direct-member"){ click_on 'Remove' }
    wait_until{ first("tr.user td.direct-member", text: 'Add') }
    within(find('tr.user'))do
      expect(find(:checkbox, id: 'member', disabled: true)).not_to be_checked
      expect(find(:checkbox, id: 'direct_member', disabled: true)).not_to be_checked
      expect(find(:checkbox, id: 'group_member', disabled: true)).not_to be_checked
    end
  end
end

shared_examples :group_user do
  scenario 'adding and removing a group via included user' do
    @user = @users.sample
    click_on 'Inventory Pools'
    click_on @pool.name
    click_on 'Delegations'
    @delegations_path = current_path
    click_on @delegation.firstname
    @delegation_path = current_path
    within('.nav-tabs') { click_on 'Users' }
    select 'members and non-members', from: 'Membership'
    fill_in 'Search', with: @user.email
    wait_until{ all('tr.user').count == 1 }
    @delegation_users_url = current_url
    within(find('tr.user'))do
      expect(find(:checkbox, id: 'member', disabled: true)).not_to be_checked
      expect(find(:checkbox, id: 'direct_member', disabled: true)).not_to be_checked
      expect(find(:checkbox, id: 'group_member', disabled: true)).not_to be_checked
    end
    within("tr td.group-member"){ click_on 'Add' }
    click_on_first 'Add'
    wait_until { all('.modal').empty? }
    visit @delegation_users_url
    wait_until{ all('tr.user').count == 1 }
    within(find('tr.user'))do
      expect(find(:checkbox, id: 'member', disabled: true)).to be_checked
      expect(find(:checkbox, id: 'direct_member', disabled: true)).not_to be_checked
      expect(find(:checkbox, id: 'group_member', disabled: true)).to be_checked
    end

    visit @delegation_path
    within("tr", text: "Number of direct users"){ expect(find("td.direct-users-count").text).to eq "1"}
    within("tr", text: "Number of groups"){ expect(find("td.groups-count").text).to eq "1"}

    # test including-user filter
    visit @delegations_path
    fill_in "including-user", with: "foo.bar@baz"
    wait_until{ page.has_content? "No (more) delegations found." }
    expect(page).not_to have_content @delegation.firstname
    wait_until{ not page.has_content? @delegation.firstname }
    fill_in "including-user", with: @user.email
    wait_until{ page.has_content? @delegation.firstname }

    visit @delegation_users_url
    within("tr td.group-member"){ click_on 'Edit' }
    wait_until{ not all('button, a', text: 'Remove').empty?}
    click_on 'Remove'
    wait_until { all('.modal').empty? }
    visit @delegation_users_url
    within(find('tr.user'))do
      expect(find(:checkbox, id: 'member', disabled: true)).not_to be_checked
      expect(find(:checkbox, id: 'direct_member', disabled: true)).not_to be_checked
      expect(find(:checkbox, id: 'group_member', disabled: true)).not_to be_checked
    end
  end
end


feature 'Manage delegation users ' do

  context 'an admin, 3 groups, 100 users each in 2 groups, and a delegation in a pool exist' do

    before :each do
      @admin = FactoryBot.create :admin
      @users = 100.times.map { FactoryBot.create :user }.to_a
      @groups = 3.times.map{ FactoryBot.create :group }.to_a
      @users.each do |user|
        @groups.shuffle.take(2).each do |group|
          database[:groups_users].insert(
            group_id: group[:id],
            user_id: user[:id])
        end
      end
      @pool = FactoryBot.create :inventory_pool
      @delegation = FactoryBot.create :delegation
      FactoryBot.create :access_right,
        user_id: @delegation.id,
        inventory_pool_id: @pool.id,
        role: 'customer'
    end

    context 'as an admin via the UI' do
      before(:each){sign_in_as @admin}
      include_examples :direct_user
      include_examples :group_user
    end

    context 'as a lending_manager of the pool via the UI ' do
      before :each do
        @manager = FactoryBot.create :user
        FactoryBot.create :access_right, user: @manager,
          inventory_pool: @pool, role: 'lending_manager'
        sign_in_as @manager
      end
      include_examples :direct_user
      include_examples :group_user
    end
  end
end
