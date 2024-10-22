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

    let(:url) { "/inventory/#{@inventory_pool.id}/entitlement-groups" }
    let(:client) { plain_faraday_json_client }
    let(:resp) { client.get url }
    let(:image_id) { resp.body[0]["id"] }

    context "GET /inventory/models-compatibles" do
      it "retrieves all compatible models and returns 200" do
        binding.pry
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(1)
      end

      it "returns paginated results with status 200" do
        resp = client.get "#{url}?page=1&size=1"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(1)
      end

      it "returns paginated results with status 200" do
        resp = client.get "#{url}/#{image_id}"

        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(1)
        expect(resp.body[0]["id"]).to eq(image_id)
      end
    end
  end
end
