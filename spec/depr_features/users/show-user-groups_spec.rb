require 'spec_helper'
require 'pry'

feature 'Show users groups', type: :feature do

  before :each do
    @admins = 3.times.map { FactoryBot.create :admin }
    @user = FactoryBot.create :user
    5.times do 
      @user.add_group FactoryBot.create(:group)
    end
  end

  context "an admin via the UI" do

    before :each do
      @admin = @admins.sample
      sign_in_as @admin
    end

    scenario 'can see all groups a user is member of' do

      visit '/admin/'
      click_on 'Users'

      fill_in 'Search', with: "#{@user.firstname} #{@user.lastname}"
      click_on_first_user @user

      wait_until { page.has_content? @user.firstname } 
      find("h1", text: @user.firstname)

      loop do
        group = find(".nav-item", text: "Groups")
        group.click
        break if group['aria-selected'] == 'true'
      end

      within ".tab-content" do
        @user.groups.each do |group|
          expect(current_scope).to have_content(group.name)
        end
      end
    end
  end
end
