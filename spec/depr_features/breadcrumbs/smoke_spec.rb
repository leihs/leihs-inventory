require 'spec_helper'
require 'pry'

feature 'Breadcrumps are visible ', type: :feature do
  let(:name) { Faker::Company.name}
  let(:description) { Faker::Markdown.sandwich }
  let(:shortname) { Faker::Name.initials }
  let(:email) { Faker::Internet.email }

  context 'an system admin and some pools ' do
    before :each do
      @admin = FactoryBot.create :system_admin
      @pools = 1.times.map { FactoryBot.create :inventory_pool }
      @users = 1.times.map { FactoryBot.create :user }
      @user = @users.sample
      @pool = @pools.sample
    end

    context "an system admin via the UI " do
      before(:each){ sign_in_as @admin }

      scenario 'navigates to routes and checks breadcrumps ' do
        visit '/admin/inventory-pools/'
        click_on(@pool.name)
        breadcrumbs = find("ol.breadcrumb")

        visit "/admin/users/"
        find("li", text: @user.email).click
        breadcrumbs = find("ol.breadcrumb")

        visit '/admin/groups/'
        click_on("All users")
        breadcrumbs = find("ol.breadcrumb")

        visit 'admin/inventory-fields/'
        click_on("attachments")
        breadcrumbs = find("ol.breadcrumb")

        visit 'admin/buildings/'
        click_on("general building (general)")
        breadcrumbs = find("ol.breadcrumb")

        visit 'admin/rooms/'
        click_on("general room")
        breadcrumbs = find("ol.breadcrumb")

        visit 'admin/suppliers/'
        click_on_first("Add Supplier")
        wait_until { page.has_content? "Add a Supplier" }
        fill_in 'name', with: 'Test'
        click_on "Save"
        wait_until { page.has_content? "Test" }
        breadcrumbs = find("ol.breadcrumb")

        visit 'admin/mail-templates/'
        click_on_first("approved")
        breadcrumbs = find("ol.breadcrumb")

        visit 'admin/system/authentication-systems/'
        click_on("password")
        breadcrumbs = find("ol.breadcrumb")
      end
    end
  end
end
