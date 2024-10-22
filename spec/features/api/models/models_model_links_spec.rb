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

feature "Inventory API Endpoints - model-links" do
  context "when fetching model-links", driver: :selenium_headless do
    include_context :setup_models_api

    let(:model_with_accessories) { @models.first }
    let(:model_without_accessories) { @models.third }
    let(:client) { plain_faraday_json_client }
    let(:inventory_pool_id) { @inventory_pool.id }

    ["/", "/#{@inventory_pool_id}"].each do |path|
      let(:url) { "/inventory#{path}models/#{model_without_accessories.id}/model-links" }

      context "Request against base & pool endpoints" do
        context "GET for model with model-links" do
          include_context :setup_category_model_linked_to_pool

          it "retrieves all model-links for the model and returns status 200" do
            # puts ">>> ADD inv: #{@models.third}"
            # puts "pool: #{@inventory_pool.id}"

            # binding.pry

            resp = client.get url
            expect(resp.status).to eq(200)
            expect(resp.body["pagination"]["total_records"]).to eq(1)
          end

          it "returns paginated results with status 200" do
            resp = client.get "#{url}?page=1&size=1"
            expect(resp.status).to eq(200)
            expect(resp.body["pagination"]["total_records"]).to eq(1)
          end

          it "returns results with status 200" do
            resp = client.get "#{url}?page=1&size=1"
            expect(resp.status).to eq(200)

            model_with_accessories = resp.body["data"][0]["id"]
            resp = client.get "#{url}/#{model_with_accessories}"
            expect(resp.status).to eq(200)
            expect(resp.body.count).to eq(1)
          end

          it "returns invalid results with status 200" do
            invalid_id = SecureRandom.uuid
            resp = client.get "#{url}/#{invalid_id}"
            expect(resp.status).to eq(200)
            expect(resp.body.count).to eq(0)
          end
        end

        context "GET  for model without model-links" do
          it "retrieves no model-links for the model and returns status 200" do
            resp = client.get url
            expect(resp.status).to eq(200)
            expect(resp.body["pagination"]["total_records"]).to eq(0)
          end

          it "returns paginated results with status 200" do
            resp = client.get "#{url}?page=1&size=1"
            expect(resp.status).to eq(200)
            expect(resp.body["pagination"]["total_records"]).to eq(0)
          end
        end
      end
    end
  end
end
