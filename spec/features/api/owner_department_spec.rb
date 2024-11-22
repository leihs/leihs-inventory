require "spec_helper"
require "pry"
require "#{File.dirname(__FILE__)}/_shared"

feature "Inventory API Endpoints - Departments and Owners" do
  context "when fetching departments and owners for an inventory pool", driver: :selenium_headless do
    include_context :setup_access_rights

    let(:client) { plain_faraday_json_client }

    ["/inventory/departments", "/inventory/owners"].each do |url|
      context "GET #{url}" do
        it "retrieves all records and returns status 200" do
          resp = client.get url
          expect(resp.status).to eq(200)
          expect(resp.body.count).to eq(4)
        end

        it "retrieves paginated results and returns status 200" do
          resp = client.get "#{url}?page=3&size=1"
          expect(resp.status).to eq(200)
          expect(resp.body["pagination"]["total_records"]).to eq(4)

          id = resp.body["data"][0]["id"]
          resp = client.get "#{url}/#{id}"
          expect(resp.status).to eq(200)
          expect(resp.body.count).to eq(1)
        end
      end
    end
  end
end
