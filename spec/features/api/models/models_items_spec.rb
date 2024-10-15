require "spec_helper"
require "pry"
require_relative "../_shared"

feature "Inventory API Endpoints - Items" do
  context "when fetching items for a specific model in an inventory pool", driver: :selenium_headless do
    include_context :setup_models_api

    let(:model_with_items) { @models.first }
    let(:model_without_items) { @models.third }
    let(:client) { plain_faraday_json_client }

    ["/", "/#{@inventory_pool_id}"].each do |path|
      let(:url) { "/inventory#{path}models/#{model_with_items.id}/items" }

      context "GET /inventory/models/:id/items for a model with items" do
        it "retrieves all items for the model and returns status 200" do
          resp = client.get url
          expect(resp.status).to eq(200)
          expect(resp.body["pagination"]["total_records"]).to eq(1)
        end

        it "retrieves paginated item results and returns status 200" do
          resp = client.get "#{url}?page=1&size=1"
          expect(resp.status).to eq(200)
          expect(resp.body["pagination"]["total_records"]).to eq(1)
        end

        it "retrieves specific item details by ID and returns status 200" do
          resp = client.get "#{url}?page=1&size=1"
          expect(resp.status).to eq(200)

          item_id = resp.body["data"][0]["id"]
          resp = client.get "#{url}/#{item_id}"
          expect(resp.status).to eq(200)
          expect(resp.body.count).to eq(1)
        end

        it "returns no results for an invalid item ID with status 200" do
          invalid_id = SecureRandom.uuid
          resp = client.get "#{url}/#{invalid_id}"
          expect(resp.status).to eq(200)
          expect(resp.body.count).to eq(0)
        end
      end

      context "GET /inventory/models/:id/items for a model without items" do
        let(:url) { "/inventory#{path}models/#{model_without_items.id}/items" }

        it "retrieves no items for the model and returns status 200" do
          resp = client.get url
          expect(resp.status).to eq(200)
          expect(resp.body["pagination"]["total_records"]).to eq(0)
        end

        it "retrieves paginated results with no items and returns status 200" do
          resp = client.get "#{url}?page=1&size=1"
          expect(resp.status).to eq(200)
          expect(resp.body["pagination"]["total_records"]).to eq(0)
        end
      end
    end
  end
end
