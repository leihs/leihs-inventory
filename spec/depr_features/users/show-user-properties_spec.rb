require 'spec_helper'
require 'pry'

feature 'Show users properties', type: :feature do

  context 'a bunch of users, as an admin via the UI' do

    before :each do
      @admins = 3.times.map { FactoryBot.create :admin }
      @admin = @admins.sample

      @users = 15.times.map do
        FactoryBot.create :user
      end

      @user = @users.sample

      sign_in_as @admin
    end

    scenario 'showing user properties on the user show page' do

      visit '/admin/'
      click_on 'Users'
      fill_in 'Search', with: @user.email
      wait_until{all( "table.users tbody tr").count == 1 }
      click_on_first_user @user
      wait_until(10) do
        page.has_content? "#{@user.firstname} #{@user.lastname}"
      end
      expect(page).to have_content 'Email'
      expect(page).to have_content @user.email

    end
  end
end
