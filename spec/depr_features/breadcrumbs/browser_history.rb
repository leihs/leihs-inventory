
require 'spec_helper'
require 'pry'

feature 'Breadcrumbs ', type: :feature do
  let(:name) { Faker::Company.name}
  let(:description) { Faker::Markdown.sandwich }
  let(:shortname) { Faker::Name.initials }
  let(:email) { Faker::Internet.email }

  context 'an admin and several pools ' do

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

      scenario 'navigates back and forth with browser buttons and checks breadcrumbs' do
        within(".nav-tabs") { click_on "Users" }
        expect(page).to have_css('ol > li', count: 2)

        within("table") { first('tr td.user').click }
        expect(page).to have_css('ol > li', count: 3)

        page.evaluate_script('window.history.back()')
        expect(page).to have_css('ol > li', count: 2)

        page.evaluate_script('window.history.forward()')
        expect(page).to have_css('ol > li', count: 3)
      end
    end
  end
end
