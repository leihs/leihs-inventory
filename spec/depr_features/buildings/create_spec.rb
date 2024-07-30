require 'spec_helper'
require 'pry'

feature 'Manage Buildings', type: :feature do

  before :each do
    @admin = FactoryBot.create :admin
  end

  let(:name) { Faker::Company.name}
  let(:code) { Faker::Lorem.characters(number: 3).upcase }

  context 'an admin via the UI' do

    before(:each){ sign_in_as @admin }

    scenario ' creates a new building ' do

      visit '/admin/'
      click_on 'Buildings'
      expect(all("a, button", text: 'Add')).not_to be_empty
      click_on_first 'Add Building'
      wait_until { page.has_css?(".modal", text: "Add Building") }
      # wait_until { page.has_content? "Add Building" }
      fill_in 'name', with: name
      fill_in 'code', with: code
      click_on 'Save'

      wait_until { all(".modal").empty? }
      wait_until { not page.has_content? "Add Building" }
      @building_path = current_path
      expect(page.text).to have_content name
      expect(page.text).to have_content code


      within("aside nav") do
        click_on("Buildings")
      end

      wait_until { current_path == "/admin/buildings/" }
      wait_until { page.has_content? name }
      click_on name
      wait_until { current_path == @building_path }

    end

  end

end
