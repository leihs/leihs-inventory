require "spec_helper"
require "pry"
require "#{File.dirname(__FILE__)}/_shared"

feature "Inventory API Endpoints - Model Links" do
  context "when fetching model links for a specific inventory pool", driver: :selenium_headless do
    include_context :setup_models_api

    let(:url) { "/inventory/#{@inventory_pool.id}/entitlement-groups" }
    let(:client) { plain_faraday_json_client }
    let(:resp) { client.get url }
    let(:model_id) { resp.body[0]["id"] }

    context "GET /inventory/models-compatibles" do
      it "retrieves all compatible models and returns status 200" do
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(1)
      end

      it "retrieves paginated compatible models with status 200" do
        resp = client.get "#{url}?page=1&size=1"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(1)
      end

      it "retrieves a specific compatible model by ID and returns status 200" do
        resp = client.get "#{url}/#{model_id}"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(1)
        expect(resp.body[0]["id"]).to eq(model_id)
      end
    end
  end
end