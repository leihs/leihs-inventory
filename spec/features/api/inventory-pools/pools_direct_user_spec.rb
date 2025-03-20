require "spec_helper"
require "pry"

def create_model(client, inventory_pool_id, product, category_ids)
  client.post "/inventory/#{inventory_pool_id}/models" do |req|
    req.body = {
      product: product,
      category_ids: category_ids,
      version: "1",
      type: "Model",
      is_package: false
    }.to_json
    req.headers["Content-Type"] = "application/json"
    req.headers["Accept"] = "application/json"
  end
end

feature "Call inventory-pool endpoints" do
  context "when retrieving models from an inventory pool", driver: :selenium_headless do
    before :each do
      # TODO: write test with user (401)
      # @user = FactoryBot.create(:user, login: "admin", password: "password")
      @user = FactoryBot.create(:admin, login: "admin", password: "password")

      resp = basic_auth_plain_faraday_json_client(@user.login, @user.password).get("/inventory/login")
      expect(resp.status).to eq(200)
      cookie_token = parse_cookie(resp.headers["set-cookie"])["leihs-user-session"]
      @cookie = CGI::Cookie.new("name" => "leihs-user-session", "value" => cookie_token)

      @inventory_pool = FactoryBot.create(:inventory_pool)

      @models = 3.times.map do
        FactoryBot.create(:leihs_model, id: SecureRandom.uuid)
      end

      LeihsModel.all.each do |model|
        FactoryBot.create(:item, leihs_model: model, inventory_pool_id: @inventory_pool.id, responsible: @inventory_pool, is_borrowable: true)
      end
    end

    let(:client) {
      # TODO: write test with plain (401)
      # plain_faraday_json_client
      session_auth_plain_faraday_json_client(@cookie.to_s)
    }

    context "with direct access rights as a group manager" do
      before :each do
        FactoryBot.create(:direct_access_right, inventory_pool_id: @inventory_pool.id, user_id: @user.id, role: "group_manager")
      end

      it "returns available models in the pool with a 200 status" do
        resp = client.get "/inventory/pools"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to be 1
      end
    end

    context "without any access rights" do
      it "returns no models and a 200 status" do
        resp = client.get "/inventory/pools"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to be 0
      end
    end

    context "when creating a new model and retrieving it from the pool" do
      before :each do
        category = FactoryBot.create(:category, name: "Test-ModelGroup")
        resp = create_model(client, @inventory_pool.id, "Example Model", [category.id])
        expect(resp.status).to eq(200)
      end

      it "returns the newly created model with a 200 status" do
        resp = client.get "/inventory/pools"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to be 0
      end

      it "returns the created model with access rights and a 200 status" do
        FactoryBot.create(:direct_access_right, inventory_pool_id: @inventory_pool.id, user_id: @user.id, role: "group_manager")
        resp = client.get "/inventory/pools"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to be 1
      end
    end
  end
end
