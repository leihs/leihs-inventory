require "spec_helper"
require "pry"
require_relative "../_shared"

feature "Inventory API Endpoints - Entitlements" do
  context "when fetching entitlements for a specific model in an inventory pool", driver: :selenium_headless do
    include_context :setup_models_api

    let(:model_with_entitlements) { @models.first }
    let(:model_without_entitlements) { @models.third }

    let(:client) { plain_faraday_json_client }

    ["/", "/#{@inventory_pool_id}"].each do |path|
      context "GET /inventory/models/:id/entitlements for a model with entitlements" do
        let(:url) { "/inventory#{path}models/#{model_with_entitlements.id}/entitlements" }

        it "retrieves all entitlements for the model and returns status 200" do
          resp = client.get url
          expect(resp.status).to eq(200)
          expect(resp.body["pagination"]["total_records"]).to eq(1)
        end

        it "retrieves paginated entitlements with status 200" do
          resp = client.get "#{url}?page=1&size=1"
          expect(resp.status).to eq(200)
          expect(resp.body["pagination"]["total_records"]).to eq(1)
        end

        it "retrieves specific entitlement details and returns status 200" do
          resp = client.get "#{url}?page=1&size=1"
          expect(resp.status).to eq(200)

          entitlement_id = resp.body["data"][0]["id"]
          resp = client.get "#{url}/#{entitlement_id}"
          expect(resp.status).to eq(200)
          expect(resp.body.count).to eq(1)
        end

        it "returns no results for an invalid entitlement ID with status 200" do
          invalid_id = SecureRandom.uuid
          resp = client.get "#{url}/#{invalid_id}"
          expect(resp.status).to eq(200)
          expect(resp.body.count).to eq(0)
        end
      end

      context "GET /inventory/models/:id/entitlements for a model without entitlements" do
        let(:url) { "/inventory#{path}models/#{model_without_entitlements.id}/entitlements" }

        it "retrieves no entitlements for the model and returns status 200" do
          resp = client.get url
          expect(resp.status).to eq(200)
          expect(resp.body["pagination"]["total_records"]).to eq(0)
        end

        it "retrieves paginated results with no entitlements and status 200" do
          resp = client.get "#{url}?page=1&size=1"
          expect(resp.status).to eq(200)
          expect(resp.body["pagination"]["total_records"]).to eq(0)
        end
      end
    end
  end
end
