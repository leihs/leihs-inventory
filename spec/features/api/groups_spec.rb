require "spec_helper"
require "pry"
require "#{File.dirname(__FILE__)}/_shared"

feature "Inventory API Endpoints - Group Links" do
  context "when fetching group links for a specific inventory pool", driver: :selenium_headless do
    include_context :setup_access_rights

    let(:url) { "/inventory/#{@inventory_pool.id}/groups" }
    let(:client) { plain_faraday_json_client }
    let(:resp) { client.get url }
    let(:group_id) { resp.body[0]["id"] }

    context "GET /inventory/groups" do
      it "retrieves all groups and returns status 200" do
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(1)
      end

      it "retrieves paginated group results and returns status 200" do
        resp = client.get "#{url}/#{group_id}"

        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(1)
        expect(resp.body[0]["id"]).to eq(group_id)
      end
    end
  end
end
