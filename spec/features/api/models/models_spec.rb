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

feature "Swagger Inventory Endpoints" do
  context "when fetching models for a pool", driver: :selenium_headless do
    include_context :setup_models_min_api

    let(:client) { plain_faraday_json_client }
    let(:inventory_pool_id) {
      puts "inventory_pool_id: #{@inventory_pool.id}"
      @inventory_pool.id
    }

    ["/", "/#{@inventory_pool_id}"].each do |path|
      let(:url) { "/inventory#{path}models" }

      context "GET /inventory/models" do
        before :each do
          @models = create_models
        end

        it "retrieves all models and returns 200" do
          resp = client.get url
          # binding.pry
          expect(resp.status).to eq(200)
          expect(resp.body["data"].count).to eq(3)
        end

        it "supports pagination and returns 200 with limited results" do
          resp = client.get "#{url}?page=3&size=1"
          expect(resp.status).to eq(200)
          expect(resp.body["data"].count).to eq(1)
        end
      end

      context "GET /inventory/:pool_id/models for specific pool" do
        it "returns an empty list for a new pool and returns 200" do
          resp = client.get url
          expect(resp.status).to eq(200)
          expect(resp.body["data"].count).to eq(0)
        end

        it "returns paginated empty results for a new pool and returns 200" do
          resp = client.get "#{url}?page=3&size=1"
          expect(resp.status).to eq(200)
          expect(resp.body["data"].count).to eq(0)
        end
      end

      context "POST and GET /inventory/:pool_id/models with new models" do
        before :each do
          category = FactoryBot.create(:category)
          resp = create_model(client, @inventory_pool.id, "Example Model", [category.id])

          expect(resp.status).to eq(200)
          @model_id = resp.body["id"]
        end

        it "returns one model after creation and returns 200" do
          resp = client.get url
          # resp = client.get "/inventory/#{@inventory_pool.id}/models"
          expect(resp.status).to eq(200)
          expect(resp.body["data"].count).to eq(1)
        end

        context "adding another model" do
          before :each do
            category = FactoryBot.create(:category)
            resp = create_model(client, @inventory_pool.id, "Example Model2", [category.id])

            expect(resp.status).to eq(200)
            @model_id = resp.body["id"]
          end

          it "returns both models and returns 200" do
            # resp = client.get "/inventory/#{@inventory_pool.id}/models"
            resp = client.get url
            expect(resp.status).to eq(200)
            expect(resp.body["data"].count).to eq(2)
          end
        end
      end
    end
  end
end
