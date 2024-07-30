require 'spec_helper'
require 'pry'

feature 'Filter users by account status', type: :feature do

  context 'a bunch of users exist, as an Admin via the UI' do

    before :each do
      @admin = FactoryBot.create :admin

      @enabeld_user = FactoryBot.create :user, account_enabled: true
      @not_enabeld_user = FactoryBot.create :user, account_enabled: false

      sign_in_as @admin
    end


    describe 'account enabled filters' do

      before :each do
        visit '/admin/'
        click_on 'Users'
        wait_until { not page.has_content? "Please wait" }
      end

      scenario 'cycle through account enabled filters' do

        select('yes', from: 'Enabled')
        wait_until { all("table.users tbody tr").count == 2 }
        expect(page).to have_content  @enabeld_user.lastname
        expect(page).not_to have_content  @not_enabeld_user.lastname
        expect(page).to have_select('Enabled', selected: 'yes')

        select('(any value)', from: 'Enabled')
        wait_until { all("table.users tbody tr").count == 3 }
        expect(page).to have_select('Enabled', selected: '(any value)')

        select('no', from: 'Enabled')
        wait_until { all("table.users tbody tr").count == 1 }
        expect(page).not_to have_content  @enabeld_user.lastname
        expect(page).to have_content  @not_enabeld_user.lastname
        expect(page).to have_select('Enabled', selected: 'no')

        select('(any value)', from: 'Enabled')
        wait_until { all("table.users tbody tr").count == 3 }
        expect(page).to have_select('Enabled', selected: '(any value)')

      end

    end

  end
end
