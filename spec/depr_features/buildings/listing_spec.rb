require 'spec_helper'
require 'pry'

feature 'Managing Buildings:', type: :feature do

  before :each do
    @admins = 3.times.map { FactoryBot.create :admin }
    @buildings = 10.times.map do 
      building = FactoryBot.create(:building)
      rand(10).times do
        FactoryBot.create(:room, building: building)
      end
      building
    end
    sign_in_as @admins.sample
    click_on "Buildings"
  end

  context 'an admin via the UI' do
    scenario "can see and click all buildings" do
      within("table.buildings tbody") do
        @buildings.each do |building|
          within("tr", text: building.name) do
            expect(current_scope).to have_selector("a[href='/admin/buildings/#{building.id}']")
          end
        end
      end
    end

    describe 'searching for a building ' do
      before :each do
        @search_building = @buildings.sample
        @other_buildings= @buildings - [@search_building]
      end

      scenario 'searching by name works' do
        sup_name = @search_building.name
        term = sup_name[1..(sup_name.length - 2)]
        fill_in 'Search', with: term
        wait_until{all( "table.buildings tbody tr").count == 1 }
        expect(page).to have_content @search_building.name
        @other_buildings.each do |other_building|
          expect(page).not_to have_content other_building.name
        end
      end
    end
  end
end
