require "spec_helper"
require "pry"
require "securerandom"

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

feature "Swagger Inventory Endpoints" do
  context "when fetching models for a pool", driver: :selenium_headless do
    before :each do
      @user = FactoryBot.create(:user, id: "ce3665a0-2711-44b8-aa47-11fb881c3f91", login: "test", password: "password")

      @inventory_pool = FactoryBot.create(:inventory_pool, id: "8f613f14-3b6d-4d5c-9804-913e2da1109e")

      FactoryBot.create(:direct_access_right, inventory_pool_id: @inventory_pool.id, user_id: @user.id, role: "group_manager")

      @models = 3.times.map do |i|
        FactoryBot.create(:leihs_model, id: SecureRandom.uuid)
      end

      LeihsModel.all.each do |model|
        FactoryBot.create(:item, leihs_model: model, inventory_pool_id: @inventory_pool.id, responsible: @inventory_pool, is_borrowable: true)
      end
    end

    let(:client) { plain_faraday_json_client }

    context "GET /inventory/models" do
      it "retrieves all models and returns 200" do
        resp = client.get "/inventory/models"
        expect(resp.status).to eq(200)
        expect(resp.body["data"].count).to eq(3)
      end

      it "supports pagination and returns 200 with limited results" do
        resp = client.get "/inventory/models?page=3&size=1"
        expect(resp.status).to eq(200)
        expect(resp.body["data"].count).to eq(1)
      end
    end

    context "GET /inventory/:pool_id/models for specific pool" do
      it "returns an empty list for a new pool and returns 200" do
        resp = client.get "/inventory/#{@inventory_pool.id}/models"
        expect(resp.status).to eq(200)
        expect(resp.body["data"].count).to eq(0)
      end

      it "returns paginated empty results for a new pool and returns 200" do
        resp = client.get "/inventory/#{@inventory_pool.id}/models?page=3&size=1"
        expect(resp.status).to eq(200)
        expect(resp.body["data"].count).to eq(0)
      end
    end

    context "POST and GET /inventory/:pool_id/models with new models" do
      before :each do
        category = FactoryBot.create(:category, name: "Test-ModelGroup")
        resp = create_model(client, @inventory_pool.id, "Example Model", [category.id])

        expect(resp.status).to eq(200)
        @model_id = resp.body["id"]
      end

      it "returns one model after creation and returns 200" do
        resp = client.get "/inventory/#{@inventory_pool.id}/models"
        expect(resp.status).to eq(200)
        expect(resp.body["data"].count).to eq(1)
      end

      context "adding another model" do
        before :each do
          category = FactoryBot.create(:category, name: "Test-ModelGroup2")
          resp = create_model(client, @inventory_pool.id, "Example Model2", [category.id])

          expect(resp.status).to eq(200)
          @model_id = resp.body["id"]
        end

        it "returns both models and returns 200" do
          resp = client.get "/inventory/#{@inventory_pool.id}/models"
          expect(resp.status).to eq(200)
          expect(resp.body["data"].count).to eq(2)
        end
      end
    end
  end
end
