require "spec_helper"
require "pry"
require_relative "../_shared"

describe "Inventory API Endpoints" do
  context "when fetching models for a specific inventory pool" do
    before :each do
      @user = FactoryBot.create(:user, login: "test", password: "password")
      @inventory_pool = FactoryBot.create(:inventory_pool)

      @direct_access_right = FactoryBot.create(:direct_access_right, inventory_pool_id: @inventory_pool.id, user_id: @user.id, role: "group_manager")

      @models = 3.times.map do
        FactoryBot.create(:leihs_model, id: SecureRandom.uuid)
      end

      @models << FactoryBot.create(:leihs_model, id: SecureRandom.uuid, product: "Abc Model")
      @models << FactoryBot.create(:leihs_model, id: SecureRandom.uuid, product: "Xyz Model")

      LeihsModel.all.each do |model|
        FactoryBot.create(:item, leihs_model: model, inventory_pool_id: @inventory_pool.id, responsible: @inventory_pool, is_borrowable: true)
      end
    end

    before do
      @admin, @admin_cookies, @user_cookies_str, @cookie_token = create_and_login(:admin)
    end

    let(:client) { session_auth_plain_faraday_json_client(cookies: @admin_cookies) }

    context "GET /inventory/{:pool-id}/models/" do
      it "retrieves all compatible models and returns status 200" do
        resp = client.get "/inventory/#{@inventory_pool.id}/models/"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(LeihsModel.count)
      end

      it "retrieves paginated results with status 200 and 1 model" do
        resp = client.get "/inventory/#{@inventory_pool.id}/models/?page=1&size=1"
        expect(resp.status).to eq(200)
        expect(resp.body["pagination"]["total_rows"]).to eq(5)
        expect(resp.body["data"].count).to eq(1)
      end
      
      
      it "retrieves expected results by search" do
                ["Abc", "Xyz"].each do |search_term|

        resp = client.get "/inventory/#{@inventory_pool.id}/models/?search=#{search_term}"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(1)
                  end
              end

    end
  end
end