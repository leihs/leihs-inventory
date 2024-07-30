require 'spec_helper'
require 'pry'

feature 'Searching users', type: :feature do

  context 'a bunch of users, as an admin via the UI' do

    before :each do
      @admins = 3.times.map { FactoryBot.create :admin }
      @system_admins = 3.times.map{ FactoryBot.create :system_admin}
      @users = 94.times.map { FactoryBot.create :user }
      @admin = @admins.sample
      sign_in_as @admin
    end


    describe 'searching and filtering users' do

      before :each do
        visit '/admin/'
        click_on 'Users'

        # set 100 per page so we see all
        select '100', from: 'Per page'
        wait_until { not page.has_content? "Please wait" }
      end

      scenario 'without any filters all admins and users are shown' do
        tbody = find("table.users tbody").text
        @system_admins.each do |admin|
          expect(tbody).to have_content admin.email
        end
        @admins.each do |admin|
          expect(tbody).to have_content admin.email
        end
        @users.each do |user|
          expect(tbody).to have_content user.email
        end
      end

      scenario 'filtering by admins' do
        select 'Leihs admin', from: 'Admin'
        wait_until { not page.has_content? "Please wait" }
        within("table.users tbody") do
          @system_admins.each do |admin|
            expect(page).to have_content admin.email
          end
          @admins.each do |user|
            expect(page).to have_content user.email
          end
          @users.each do |user|
            expect(page).not_to have_content user.email
          end
        end
      end

      scenario 'filtering by system admins' do
        select 'System admin', from: 'Admin'
        wait_until { not page.has_content? "Please wait" }
        within("table.users tbody") do
          @system_admins.each do |admin|
            expect(page).to have_content admin.email
          end
          @admins.each do |user|
            expect(page).not_to have_content user.email
          end
          @users.each do |user|
            expect(page).not_to have_content user.email
          end
        end
      end

      describe 'searching for a user ' do

        before :each do
          @search_user = @users.sample
          @other_users= @users - [@search_user]
        end

        scenario 'searching by email works' do
          fill_in 'Search', with: @search_user.email
          wait_until{all( "table.users tbody tr").count == 1 }
          expect(page).to have_content @search_user.email
          @other_users.each do |other_user|
            expect(page).not_to have_content other_user.email
          end
        end

        scenario 'searching with small spelling error works' do
          fill_in 'Search', with: "#{@search_user.firstname}X #{@search_user.lastname}"
          wait_until { not page.has_content? "Please wait" }
          expect(page).to have_content @search_user.email
        end

      end
    end
  end
end
