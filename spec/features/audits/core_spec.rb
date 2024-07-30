require 'spec_helper'
require 'pry'

feature 'Audits' do
  context 'as an system admin' do
    before :each do
      @system_admin = FactoryBot.create :system_admin
      sign_in_as @system_admin
    end

    context 'after I edited a user' do
      before :each do
        @user = FactoryBot.create :user
        visit "/admin/users/#{@user.id}"
        click_on 'Edit'
        fill_in 'firstname', with: 'FooHans'
        click_on 'Save'
        wait_until {current_path == "/admin/users/#{@user.id}"}
      end

      scenario 'I can inspect the audited request' do
        click_on 'Audits'
        click_on 'Requests'
        within 'table' do
          click_on_first 'Request'
        end

        expect(page).to have_content @user.firstname
        expect(page).to have_content 'FooHans'
      end

      scenario 'I can inspect the audited changes after searching' do
        click_on 'Audits'
        click_on 'Changes'

        fill_in 'term', with: @user.firstname
        wait_until { all('table tbody tr').count == 2 } 

        within(find_all('table tbody tr').first) do |node|
          expect(find('td.table-name')).to have_content 'users'
          expect(find('td.tg-op')).to have_content 'UPDATE'
          expect(find('td.changed-attributes')).to have_content 'firstname'
          click_on 'Change'
        end

        expect(page).to have_content 'Audited Change'
        expect(page).to have_content @user.firstname
        expect(page).to have_content 'FooHans'
      end
    end
  end
end
