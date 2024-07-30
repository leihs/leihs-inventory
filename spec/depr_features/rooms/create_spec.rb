require 'spec_helper'
require 'pry'

feature 'Manage rooms', type: :feature do
  before :each do
    @admin = FactoryBot.create :admin
    @buildings = 10.times.map { FactoryBot.create(:building) }
    @building = @buildings.sample
  end

  let(:name) { Faker::Company.name}
  let(:description) { Faker::Markdown.sandwich }

  context 'an admin via the UI' do
    before(:each){ sign_in_as @admin }

    scenario ' creates a new room ' do
      click_on 'Rooms'
      expect(all("a, button", text: 'Add Room')).not_to be_empty
      click_on_first 'Add Room'
      fill_in 'name', with: name
      fill_in 'description', with: description
      within('.modal') do 
        select(@building.name, from: 'building')
      end
      click_on 'Save'
      wait_until { all(".modal").empty? }
      @room_path = current_path
      expect(page.text).to have_content name
      expect(page.text).to have_content description
      expect(page.text).to have_content @building.name

      # The inventory pools path includes the newly created inventory pool and
      within find("aside nav", match: :first) do
        click_on "Rooms"
      end

      wait_until { current_path == "/admin/rooms/" }
      wait_until { page.has_content? name }
      click_on name
      wait_until { current_path == @room_path }
    end
  end
end
