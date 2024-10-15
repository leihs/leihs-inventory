require "spec_helper"
require "pry"
require_relative "../_shared"

feature "Inventory API Endpoints - Compatible Models" do
  context "when fetching compatible models for an inventory pool", driver: :selenium_headless do
    include_context :setup_access_rights

    let(:client) { plain_faraday_json_client }

    context "GET /inventory/models-compatibles" do
      it "retrieves no compatible models and returns status 200" do
        resp = client.get "/inventory/models-compatibles"
        expect(resp.status).to eq(200)
        expect(resp.body["pagination"]["total_records"]).to eq(0)
      end

      it "retrieves paginated results with no compatible models and returns status 200" do
        resp = client.get "/inventory/models-compatibles?page=1&size=1"
        expect(resp.status).to eq(200)
        expect(resp.body["pagination"]["total_records"]).to eq(0)
      end
    end
  end
end
