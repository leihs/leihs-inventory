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

        # 2x sign in 1x edited user
        expect( find_all('table tbody tr').count ).to be==3

        fill_in "user-uid", with: @system_admin.id
        select "PATCH", from: 'method'

        # 1x edited user
        expect( find_all('table tbody tr').count ).to be==1

        click_on_first 'Reset'
        # 2x sign in 1x edited user
        expect( find_all('table tbody tr').count ).to be==3


        click_on 'Choose'
        fill_in 'term', with: @system_admin.lastname
        click_on 'Choose user'
        select "PATCH", from: 'method'

        # 1x edited user
        expect( find_all('table tbody tr').count ).to be==1

      end
    end
  end
end
