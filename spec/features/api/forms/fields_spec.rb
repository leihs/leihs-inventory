require "spec_helper"
require "pry"
require_relative "../_shared"

feature "Fetching Fields" do
  context "when searching for fields in a specific inventory pool", driver: :selenium_headless do
    include_context :setup_models_api

    let(:client) { plain_faraday_json_client }
    let(:pool_id) { @inventory_pool.id }

    {
      "customer" => 11,
      "lending_manager" => 25,
      "group_manager" => 11,
      "inventory_manager" => 32
    }.each do |role, expected_count|
      context "GET /inventory/:pool_id/fields for role #{role}" do
        let(:url) { "/inventory/fields?role=#{role}" }

        it "returns #{expected_count} fields for role #{role} and status 200" do
          resp = client.get url

          expect(resp.status).to eq(200)
          expect(resp.body["pagination"]["total_records"]).to eq(expected_count)
          expect(resp.body["data"].count).to eq(10)
        end
      end
    end

    context "GET /inventory/:pool_id/fields without specifying a role" do
      let(:url) { "/inventory/fields" }

      it "returns 46 total records and status 200" do
        resp = client.get url

        expect(resp.status).to eq(200)
        expect(resp.body["pagination"]["total_records"]).to eq(46)
        expect(resp.body["data"].count).to eq(10)
      end
    end

    context "GET /inventory/:pool_id/fields and retrieve specific field details" do
      let(:url) { "/inventory/fields" }

      it "returns the details of a specific field with status 200" do
        resp = client.get url
        id = resp.body["data"][0]["id"]

        resp = client.get "#{url}/#{id}"
        expect(resp.status).to eq(200)
        expect(resp.body[0]["id"]).to eq(id)
      end
    end
  end
end
