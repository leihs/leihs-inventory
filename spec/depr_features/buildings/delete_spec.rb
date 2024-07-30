require 'spec_helper'
require 'pry'

feature 'Manage Buildings', type: :feature do

  context ' an admin' do

    before :each do
      @admin = FactoryBot.create :admin
      @buildings = 10.times.map { FactoryBot.create :building }
      sign_in_as @admin
    end

    scenario 'deleting a building' do

      visit '/admin/'
      click_on 'Buildings'

      @buildings.each { |building| expect(page).to have_content building.name }

      click_on @buildings.first.name
      @building_path = current_path

      click_on 'Delete' # delete page
      within '.modal' do
        click_on 'Delete' # submit / confirm
      end

      wait_until { current_path ==  "/admin/buildings/" }

      @buildings.drop(1).each { |building| expect(page).to have_content building.name }

      expect(page).not_to have_content @buildings.first.name

    end

  end

end
