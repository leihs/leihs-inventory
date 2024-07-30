
require 'spec_helper'
require 'pry'

feature 'Authentication-Systems', type: :feature do

  context 'an admin, a system_admin, some groups, a bunch of users (each at least in one group), and one authentication-system exist' do

    before :each do
      @admin = FactoryBot.create :admin
      @auth_system = FactoryBot.create :authentication_system
      @system_admin =  FactoryBot.create :system_admin,
        email: 'admin@example.com', password: 'secret'
      @users = 100.times.map { FactoryBot.create :user }.to_a
      @groups = 3.times.map{ FactoryBot.create :group }.to_a
      @users.each do |user|
        @groups.shuffle.take(2).each do |group|
          database[:groups_users].insert(
            group_id: group[:id],
            user_id: user[:id])
        end
      end
    end

    context 'as a system_admin' do
      before(:each){ @current_user = @system_admin }

      context 'via the UI' do
        before(:each){ sign_in_as @current_user }

        scenario 'CRUD' do
          within 'aside nav' do
            click_on 'Settings'
            click_on 'Authentication Systems'
          end
          click_on_first 'Add Authentication System'
          fill_in 'id', with: 'test-auth-system'
          fill_in 'name', with: 'Test Authentication System'
          fill_in 'type', with: 'external'
          fill_in 'description', with: 'foo bar baz'
          fill_in 'internal_private_key', with: 'INT PRIV-KEY'
          fill_in 'internal_public_key', with: 'INT PUB-KEY'
          fill_in 'external_public_key', with: 'EXT PUB-KEY'
          fill_in 'external_sign_in_url', with: 'http://exsys/sign-in'
          fill_in 'external_sign_out_url', with: 'http://exsys/sign-out'
          click_on 'Save'
          wait_until do
            current_path.match /authentication-systems\/test-auth-system$/
          end
          sleep 1
          # field_content = all("input, textarea").map(&:value).join(" ")
          expect(page.text).to have_content /Test Authentication System/
          expect(page.text).to have_content /foo bar baz/
          expect(page.text).to have_content /external/
          expect(page.text).to have_content /INT PRIV-KEY/
          expect(page.text).to have_content /INT PUB-KEY/
          expect(page.text).to have_content /EXT PUB-KEY/
          expect(page.text).to have_content %r{http://exsys/sign-in}
          expect(page.text).to have_content %r{http://exsys/sign-out}
          click_on 'Edit'
          fill_in 'description', with: 'baz bar foo'
          click_on 'Save'
          wait_until do
            current_path.match /authentication-systems\/test-auth-system$/
          end
          expect(page).to have_content 'baz bar foo'
          click_on 'Delete'
          within '.modal' do
            click_on 'Delete' 
          end
        end

        scenario 'adding and removing a direct user' do
          @user = @users.sample
          within 'aside nav' do
            click_on 'Settings'
            click_on 'Authentication Systems'
          end
          click_on @auth_system.id
          within '.nav-tabs' do
            click_on "Users"
          end
          select "members and non-members", from: 'Membership'
          fill_in 'Search', with: @user.email
          wait_until{ all('.users tbody tr').count == 1 }
          within('table.users tbody tr')do
            expect(find(:checkbox, id: 'member', disabled: true)).not_to be_checked
            expect(find(:checkbox, id: 'direct_member', disabled: true)).not_to be_checked
            expect(find(:checkbox, id: 'group_member', disabled: true)).not_to be_checked
          end
          within(".direct-member"){ click_on 'Add' }
          wait_until { all('.modal').empty? }
          within('table.users tbody tr')do
            expect(find(:checkbox, id: 'member', disabled: true)).to be_checked
            expect(find(:checkbox, id: 'direct_member', disabled: true)).to be_checked
            expect(find(:checkbox, id: 'group_member', disabled: true)).not_to be_checked
          end
          within(".direct-member"){ click_on 'Remove' }
          wait_until { all('.modal').empty? }
          within('table.users tbody tr')do
            expect(find(:checkbox, id: 'member', disabled: true)).not_to be_checked
            expect(find(:checkbox, id: 'direct_member', disabled: true)).not_to be_checked
            expect(find(:checkbox, id: 'group_member', disabled: true)).not_to be_checked
          end
        end

        scenario 'adding and removing a user via a group' do
          @user = @users.sample
          within 'aside nav' do
            click_on 'Settings'
            click_on 'Authentication Systems'
          end
          click_on @auth_system.id
          within '.nav-tabs' do
            click_on "Users"
          end
          select "members and non-members", from: 'Membership'
          fill_in 'Search', with: @user.email
          wait_until{ all('.users tbody tr').count == 1 }
          @users_page = current_url
          within('.users tbody tr') do
            expect(find(:checkbox, id: 'member', disabled: true)).not_to be_checked
            expect(find(:checkbox, id: 'direct_member', disabled: true)).not_to be_checked
            expect(find(:checkbox, id: 'group_member', disabled: true)).not_to be_checked
          end
          within(".group-member"){ click_on 'Add' }
          @group = @groups.filter{|g|
            database[:groups_users].where(user_id: @user.id, group_id: g.id).first
          }.sample
          fill_in 'Search', with: @group.name
          wait_until{ all(".groups tbody tr").count == 1}
          within(".groups tbody tr"){ click_on 'Add' }
          wait_until { all('.modal').empty? }
          within('.groups tbody tr') do
            expect(find(:checkbox, id: 'group_member', disabled: true)).to be_checked
          end
          visit @users_page
          wait_until{ all('.users tbody tr').count == 1 }
          within('.users tbody tr') do
            expect(find(:checkbox, id: 'member', disabled: true)).to be_checked
            expect(find(:checkbox, id: 'direct_member', disabled: true)).not_to be_checked
            expect(find(:checkbox, id: 'group_member', disabled: true)).to be_checked
          end
          within(".group-member"){ click_on 'Edit' }
          fill_in 'Search', with: @group.name
          wait_until{ all(".groups tbody tr").count == 1}
          within(".groups tbody tr"){ click_on 'Remove' }
          wait_until { all('.modal').empty? }
          within('.groups tbody tr') do
            expect(find(:checkbox, id: 'group_member', disabled: true)).not_to be_checked
          end
          visit @users_page
          wait_until{ all('.users tbody tr').count == 1 }
          within('.users tbody tr') do
            expect(find(:checkbox, id: 'member', disabled: true)).not_to be_checked
            expect(find(:checkbox, id: 'direct_member', disabled: true)).not_to be_checked
            expect(find(:checkbox, id: 'group_member', disabled: true)).not_to be_checked
          end

        end
      end
    end
  end
end

