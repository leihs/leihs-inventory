require 'spec_helper'
require 'pry'

feature 'Manage inventory-pools', type: :feature do

  before :each do
    @admin = FactoryBot.create :admin
  end

  let(:name) { Faker::Company.name}
  let(:description) { Faker::Markdown.sandwich }
  let(:shortname) { Faker::Name.initials }
  let(:email) { Faker::Internet.email }

  context 'an admin via the UI' do

    before(:each){ sign_in_as @admin }

    scenario ' creates a new inventory-pool ' do

      visit '/admin/'

      within("aside nav") do
        click_on "Inventory Pools"
      end

      expect(all("a, button", text: 'Add Inventory Pool')).not_to be_empty
      first("button", text: 'Add Inventory Pool').click
      # click_on 'Add Inventory Pool'
      fill_in 'name', with: name
      fill_in 'description', with: description
      fill_in 'shortname', with: shortname
      fill_in 'email', with: email
      click_on_toggle 'is_active'
      click_on 'Save'
      wait_until { all(".modal").empty? }
      wait_until { not page.has_content? "Add Inventory Pool" }
      @inventory_pool_path = current_path
      @inventory_pool_id = current_path.match(/.*\/([^\/]+)/)[1]

      # input_values = all("input").map(&:value).join(" ")
      expect(page.text).to have_content name
      expect(page.text).to have_content shortname
      expect(page.text).to have_content email
      expect(page.text).to have_content description

      # The inventory pools path includes the newly created inventory pool and
      # we can get to it via clicking its name

      within("aside nav") do
        click_on "Inventory Pools"
      end

      wait_until { current_path == "/admin/inventory-pools/" }
      wait_until { page.has_content? name }
      click_on name
      wait_until { current_path == @inventory_pool_path }


    end

  end

  context 'a inventory-pool manager' do

    before :each do
      @pool =  FactoryBot.create :inventory_pool
      @manager = FactoryBot.create :user
      FactoryBot.create :access_right, user: @manager,
        inventory_pool: @pool, role: 'inventory_manager'
    end

    context 'via the UI' do
      before(:each){ sign_in_as @manager }

      scenario 'there is no create button' do
      within("aside nav") do
        click_on "Inventory Pools"
      end
        wait_until { page.has_content? @pool.name }
        expect(all("a, button", text: 'Add Inventory Pool')).to be_empty
      end

    end


    context 'via API' do

      let :http_client do
        plain_faraday_client
      end

      let :prepare_http_client do
        @api_token = FactoryBot.create :api_token, user_id: @manager.id
        @token_secret = @api_token.token_secret
        http_client.headers["Authorization"] = "Token #{@token_secret}"
        http_client.headers["Content-Type"] = "application/json"
      end

      before :each do
        prepare_http_client
      end


      scenario 'creating an inventory_pool is forbidden ' do
        resp = http_client.post "/admin/inventory-pools/", {}.to_json
        expect(resp.status).to be== 403
      end

    end

  end

end
