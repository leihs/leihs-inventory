require "spec_helper"
require "pry"
require_relative "../_shared"

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

feature "Swagger Inventory Endpoints - Models" do
  context "when fetching models for an inventory pool", driver: :selenium_headless do
    include_context :setup_models_min_api

    let(:client) { plain_faraday_json_client }
    let(:inventory_pool_id) { @inventory_pool.id }
    let(:url) { "/inventory/models" }

    context "GET /inventory/models" do
      before :each do
        @models = create_models
      end

      it "retrieves all models and returns status 200" do
        resp = client.get url
        expect(resp.status).to eq(200)
        expect(resp.body["data"].count).to eq(3)
      end

      it "supports pagination and returns status 200 with limited results" do
        resp = client.get "#{url}?page=3&size=1"
        expect(resp.status).to eq(200)
        expect(resp.body["data"].count).to eq(1)
      end
    end

    context "GET /inventory/:pool_id/models for a specific pool" do
      it "returns an empty list for a new pool and returns status 200" do
        resp = client.get url
        expect(resp.status).to eq(200)
        expect(resp.body["data"].count).to eq(0)
      end

      it "returns paginated empty results for a new pool and returns status 200" do
        resp = client.get "#{url}?page=3&size=1"
        expect(resp.status).to eq(200)
        expect(resp.body["data"].count).to eq(0)
      end
    end

    context "POST and GET /inventory/:pool_id/models when creating new models" do
      before :each do
        category = FactoryBot.create(:category)
        resp = create_model(client, inventory_pool_id, Faker::Lorem.word, [category.id])

        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(1)
        @model_id = resp.body[0]["id"]
      end

      it "returns one model after creation and returns status 200" do
        resp = client.get url
        expect(resp.status).to eq(200)
        expect(resp.body["data"].count).to eq(1)
      end

      context "when adding another model" do
        before :each do
          category = FactoryBot.create(:category)
          resp = create_model(client, inventory_pool_id, Faker::Lorem.word, [category.id])

          expect(resp.status).to eq(200)
          expect(resp.body.count).to eq(1)
        end

        it "returns both models and returns status 200" do
          resp = client.get url
          expect(resp.status).to eq(200)
          expect(resp.body["data"].count).to eq(2)
        end
      end
    end
  end
end
