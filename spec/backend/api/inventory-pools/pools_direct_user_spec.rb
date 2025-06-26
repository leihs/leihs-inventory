require "spec_helper"
require "pry"
require_relative "../_shared"

describe "Call inventory-pool endpoints" do
  context "when retrieving models from an inventory pool" do
    before :each do
      @user, @user_cookies, @user_cookies_str, @cookie_token = create_and_login(:user)

      @inventory_pool = FactoryBot.create(:inventory_pool)

      @models = 3.times.map do
        FactoryBot.create(:leihs_model, id: SecureRandom.uuid)
      end

      LeihsModel.all.each do |model|
        FactoryBot.create(:item, leihs_model: model, inventory_pool_id: @inventory_pool.id, responsible: @inventory_pool, is_borrowable: true)
      end
    end

    let(:client) { session_auth_plain_faraday_json_csrf_client(cookies: @user_cookies) }

    context "with direct access rights as a group manager" do
      before :each do
        FactoryBot.create(:direct_access_right, inventory_pool_id: @inventory_pool.id, user_id: @user.id, role: "group_manager")
      end

      it "returns available models in the pool with a 200 status" do
        resp = client.get "/inventory/profile"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to be 1
      end
    end

    context "without any access rights" do
      it "returns no models and a 200 status" do
        resp = client.get "/inventory/profile"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to be 0
      end
    end

    context "when creating a new model and retrieving it from the pool" do
      before :each do
        category = FactoryBot.create(:category, name: "Test-ModelGroup")
        resp = create_model_post(client, @inventory_pool.id, "Example Model", [category.id])
        expect(resp.status).to eq(200)
      end

      it "returns the newly created model with a 200 status" do
        resp = client.get "/inventory/profile"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to be 0
      end

      it "returns the created model with access rights and a 200 status" do
        FactoryBot.create(:direct_access_right, inventory_pool_id: @inventory_pool.id, user_id: @user.id, role: "group_manager")
        resp = client.get "/inventory/profile"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to be 1
      end
    end
  end
end
