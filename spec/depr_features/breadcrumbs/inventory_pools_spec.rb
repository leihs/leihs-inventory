require 'spec_helper'
require 'pry'

feature 'Breadcrumbs ', type: :feature do
  let(:name) { Faker::Company.name}
  let(:description) { Faker::Markdown.sandwich }
  let(:shortname) { Faker::Name.initials }
  let(:email) { Faker::Internet.email }

  context 'an admin and a pool' do

    def create_delegation(pool)
      delegation = FactoryBot.create(:delegation)
      FactoryBot.create(:direct_access_right,
                        inventory_pool_id: pool.id,
                        user_id: delegation.id,
                        role: 'customer')
      FactoryBot.create(:direct_access_right,
                        inventory_pool_id: pool.id,
                        user_id: delegation.responsible_user.id,
                        role: 'customer')
      delegation
    end

    before :each do
      @admin = FactoryBot.create :system_admin
      @pools = 1.times.map { FactoryBot.create :inventory_pool }
      @users = 1.times.map { FactoryBot.create :user }
      @user = @users.sample
      @groups = 1.times.map{ FactoryBot.create :group}
      @group = @groups.sample
      @pool = @pools.sample
      @entitlement_group = FactoryBot.create :entitlement_group, inventory_pool_id: @pool.id
      @delegations = 1.times.map { create_delegation(@pool) }
      @delegation = @delegations.sample
    end

    context "an system admin via the UI" do
      before(:each){ sign_in_as @admin }

      before do
        visit '/admin/'
        within("aside nav") { click_on "Inventory Pools" }
        click_on(@pool.name)
      end

      scenario 'navigates to a user and back via breadcrumbs ' do
        expect(page).to have_selector('ol', text: "Inventory Pools")
        expect(page).to have_selector('ol', text: @pool.name)
        expect(page).to have_css('ol > li', count: 2)

        within(".nav-tabs") { click_on "Users" }
        expect(page).to have_css('ol > li', count: 2)

        within("table") { first('tr td.user').click }
        expect(page).to have_css('ol > li', count: 3)

        name = within("article header") { first("h1").text }
        expect(page).to have_selector('ol', text: name)

        within(".breadcrumb") do
          find("[data-test-id='#{@pool.name}']").click
        end
        expect(page).to have_css('ol > li', count: 2)

        within("ol.breadcrumb") { click_on("Inventory Pools") }
        expect(page).not_to have_content('ol.breadcrumb')
      end

      scenario 'navigates to a group and back via breadcrumbs ' do
        expect(page).to have_selector('ol', text: "Inventory Pools")
        expect(page).to have_selector('ol', text: @pool.name)
        expect(page).to have_css('ol > li', count: 2)

        within(".nav-tabs") { click_on "Groups" }
        expect(page).to have_css('ol > li', count: 2)

        select "(any role or none)", from: "role"
        click_on(@group.name)

        expect(page).to have_selector('ol', text: @group.name)
        expect(page).to have_css('ol > li', count: 3)

        within(".nav-tabs") { click_on "Users" }
        expect(page).to have_css('ol > li', count: 3)

        select "members and non-members", from: "Membership"
        find("li", text: @user.email).click

        expect(page).to have_selector('ol', text: @user.firstname + " " + @user.lastname)
        expect(page).to have_css('ol > li', count: 4)

        within(".breadcrumb") do
          find("[data-test-id='#{@group.name}']").click
        end
        expect(page).to have_css('ol > li', count: 3)

        within(".breadcrumb") do
          find("[data-test-id='#{@pool.name}']").click
        end
        expect(page).to have_css('ol > li', count: 2)

        within("ol.breadcrumb") { click_on("Inventory Pools") }
        expect(page).not_to have_content('ol.breadcrumb')
      end


      scenario 'navigates to a delegation and back via breadcrumbs ' do
        within(".nav-tabs") { click_on "Delegations" }
        expect(page).to have_css('ol > li', count: 2)

        find(".name.text-left").click
        expect(page).to have_selector('ol', text: @delegation.firstname)
        expect(page).to have_css('ol > li', count: 3)

        within(".nav-tabs") { click_on "Users" }
        expect(page).to have_css('ol > li', count: 3)

        find("td.user").click
        name = within("article header") { first("h1").text }
        expect(page).to have_selector('ol', text: name)
        expect(page).to have_css('ol > li', count: 4)

        within(".breadcrumb") do
          find("[data-test-id='#{@delegation.firstname}']").click
        end
        expect(page).to have_css('ol > li', count: 3)

        within(".breadcrumb") do
          find("[data-test-id='#{@pool.name}']").click
        end
        expect(page).to have_css('ol > li', count: 2)

        within("ol.breadcrumb") { click_on("Inventory Pools") }
        expect(page).not_to have_content('ol.breadcrumb')
      end


      scenario 'navigates to a entitlement-group and back via breadcrumbs ' do
        within(".nav-tabs") { click_on "Entitlement Groups" }
        expect(page).to have_css('ol > li', count: 2)

        click_on @entitlement_group.name
        expect(page).to have_selector('ol', text: @entitlement_group.name)
        expect(page).to have_css('ol > li', count: 3)

        within(".nav-tabs") { click_on "Users" }
        expect(page).to have_css('ol > li', count: 3)

        select "members and non-members", from: "Membership"
        first("td.user").click

        name = within("article header") { first("h1").text }
        expect(page).to have_selector('ol', text: name)
        expect(page).to have_css('ol > li', count: 4)

        within(".breadcrumb") do
          find("[data-test-id='#{@entitlement_group.name}']").click
        end
        expect(page).to have_css('ol > li', count: 3)

        within(".breadcrumb") do
          find("[data-test-id='#{@pool.name}']").click
        end
        expect(page).to have_css('ol > li', count: 2)

        within("ol.breadcrumb") { click_on("Inventory Pools") }
        expect(page).not_to have_content('ol.breadcrumb')
      end
    end
  end
end
