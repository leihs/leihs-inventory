require "spec_helper"
require "pry"
require_relative "../_shared"

feature "Inventory API Endpoints - model-links" do
  context "when fetching model-links for a model in an inventory pool", driver: :selenium_headless do
    include_context :setup_models_api

    let(:model_with_links) { @models.first }
    let(:model_without_links) { @models.third }
    let(:client) { plain_faraday_json_client }
    let(:inventory_pool_id) { @inventory_pool.id }
    let(:path) { "/#{inventory_pool_id}/" }

    context "Request against endpoint" do
      let(:url) { "/inventory#{path}models/#{model_without_links.id}/model-links" }

      context "GET /inventory{path}models/:id/model-links for a model with model-links" do
        include_context :setup_category_model_linked_to_pool

        it "retrieves all model-links for the model and returns status 200" do
          resp = client.get url
          expect(resp.status).to eq(200)
          expect(resp.body["pagination"]["total_records"]).to eq(1)
        end

        it "retrieves paginated model-link results and returns status 200" do
          resp = client.get "#{url}?page=1&size=1"
          expect(resp.status).to eq(200)
          expect(resp.body["pagination"]["total_records"]).to eq(1)
        end

        it "retrieves specific model-link details and returns status 200" do
          resp = client.get "#{url}?page=1&size=1"
          expect(resp.status).to eq(200)

          model_link_id = resp.body["data"][0]["id"]
          resp = client.get "#{url}/#{model_link_id}"
          expect(resp.status).to eq(200)
          expect(resp.body.count).to eq(2)
        end

        it "returns no results for an invalid model-link ID and status 200" do
          invalid_id = SecureRandom.uuid
          resp = client.get "#{url}/#{invalid_id}"
          expect(resp.status).to eq(200)
          expect(resp.body.count).to eq(2)  # FIXME
        end
      end

      context "GET /inventory{path}models/:id/model-links for a model without model-links" do
        it "retrieves no model-links for the model and returns status 200" do
          resp = client.get url
          expect(resp.status).to eq(200)
          expect(resp.body["pagination"]["total_records"]).to eq(0)
        end

        it "retrieves paginated results with no model-links and returns status 200" do
          resp = client.get "#{url}?page=1&size=1"
          expect(resp.status).to eq(200)
          expect(resp.body["pagination"]["total_records"]).to eq(0)
        end
      end
      # end
    end
  end
end
