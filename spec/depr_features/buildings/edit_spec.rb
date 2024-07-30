require 'spec_helper'
require 'pry'

feature 'Manage buildings', type: :feature do

  let(:name) { Faker::Company.name}
  let(:code) { Faker::Lorem.characters(number: 3).upcase }

  before :each do
    @admin = FactoryBot.create :admin
    @buildings = 10.times.map do
      building = FactoryBot.create(:building)
      5.times.map do
        FactoryBot.create(:room, building: building)
      end
      building
    end
    @building = @buildings.sample
  end

  context "an admin via the UI" do
    before(:each){ sign_in_as @admin }

    scenario 'edits a building' do
      visit '/admin/'
      click_on 'Buildings'
      click_on @building.name
      @building_path = current_path

      click_on 'Edit'
      fill_in 'name', with: name
      fill_in 'code', with: code
      click_on 'Save'
      wait_until { all(".modal").empty? }
      wait_until {current_path == @building_path}
      wait_until { all(".wait-component").empty? }

      input_values = all("input").map(&:value).join(" ")
      expect(page.text + input_values).to have_content name
      expect(page.text + input_values).to have_content code

      within("aside nav") do
        click_on "Buildings"
      end

      wait_until { current_path ==  "/admin/buildings/" }
      expect(page).to have_content name
    end

  end

end
