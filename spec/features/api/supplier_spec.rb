require "spec_helper"
require "pry"
require "#{File.dirname(__FILE__)}/_shared"

feature "Inventory API Endpoints - Supplier" do
  context "when fetching suppliers for a specific inventory pool", driver: :selenium_headless do
    include_context :setup_access_rights

    let(:url) { "/inventory/supplier" }
    let(:client) { plain_faraday_json_client }
    let(:resp) { client.get url }
    let(:supplier_id) { resp.body[0]["id"] }

    context "GET /inventory/supplier" do
      it "retrieves all suppliers and returns status 200" do
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(1)
      end

      it "retrieves paginated supplier results and returns status 200" do
        resp = client.get "#{url}?page=1&size=1"
        expect(resp.status).to eq(200)
        expect(resp.body["data"].count).to eq(1)
      end

      it "retrieves specific supplier details by ID and returns status 200" do
        resp = client.get "#{url}/#{supplier_id}"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(1)
        expect(resp.body[0]["id"]).to eq(supplier_id)
      end
    end
  end
end
