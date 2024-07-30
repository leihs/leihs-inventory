require 'spec_helper'
require 'pry'

feature 'Managing rooms:', type: :feature do

  before :each do
    @admins = 3.times.map { FactoryBot.create :admin }
    @pools = 5.times.map { FactoryBot.create :inventory_pool }
    @buildings = 5.times.map { FactoryBot.create :building }

    @rooms = @buildings.map do |building|
      2.times.map { FactoryBot.create(:room, building: building) }
    end.flatten

    sign_in_as @admins.sample
    click_on "Rooms"
  end

  context 'an admin via the UI' do
    scenario "can see and click all rooms" do
      within("table.rooms tbody") do
        @rooms.each do |room|
          within("tr", text: room.name) do
            expect(current_scope).to have_selector("a[href='/admin/rooms/#{room.id}']")
          end
        end
      end
    end

    describe 'searching for a room ' do
      before :each do
        @search_room = @rooms.sample
        @other_rooms= @rooms - [@search_room]
      end

      scenario 'searching by name works' do
        sup_name = @search_room.name
        term = sup_name[1..(sup_name.length - 2)]
        fill_in 'Search', with: term
        wait_until{all( "table.rooms tbody tr").count == 1 }
        expect(page).to have_content @search_room.name
        @other_rooms.each do |other_room|
          expect(page).not_to have_content other_room.name
        end
      end

      scenario 'filtering by pools works' do
        building = @buildings.sample

        select building.name, from: 'Building'
        wait_until { not page.has_content? "Please wait" }
        expect(all("table.rooms tbody tr").count).to eq building.rooms.count
        within "table.rooms tbody" do
          building.rooms.each do |room|
            expect(current_scope).to have_content room.name
          end
        end
      end

      scenario 'filtering by general works' do
        select 'yes', from: 'General'
        wait_until { not page.has_content? "Please wait" }
        expect(all("table.rooms tbody tr").count).to eq @buildings.count + 1 # including general room from general building
        within "table.rooms tbody" do
          Building.all.each do |building|
            expect(current_scope).to have_content building.name
          end
        end
      end
    end
  end
end
