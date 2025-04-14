require "spec_helper"
require "pry"
require_relative "../_shared"

feature "Inventory API Endpoints - Accessories" do
  context "when fetching accessories for models in an inventory pool", driver: :selenium_headless do
    include_context :setup_models_api, "inventory_manager"

    let(:model_with_accessories) { @models.first }
    let(:model_without_accessories) { @models.third }
    let(:client) { session_auth_plain_faraday_json_client(cookies: @user_cookies) }

    before :each do
      @user, @user_cookies, @user_cookies_str, @cookie_token = create_and_login(:user)
    end

    context "with path prefix '/<inventory_pool_id>'" do
      let(:path) { "/#{@inventory_pool_id}/" }

      context "GET /inventory/models/:id/accessories for a model with accessories" do
        let(:url) { "/inventory#{path}models/#{model_with_accessories.id}/accessories" }

        it "retrieves all accessories for the model and returns status 200" do
          resp = client.get url
          puts ">> url: #{url}"
          expect(resp.status).to eq(200)
          # expect(resp.body["pagination"]["total_rows"]).to eq(1)
          expect(resp.body.count).to eq(1)
        end

        # FIXME
        it "returns paginated accessory results and status 200" do
          resp = client.get "#{url}?page=1&size=1"
          expect(resp.status).to eq(200)
          # expect(resp.body["pagination"]["total_rows"]).to eq(1)
          expect(resp.body.count).to eq(1)
        end

        # FIXME
        it "retrieves details of a specific accessory and returns status 200" do
          resp = client.get "#{url}?page=1&size=1"
          expect(resp.status).to eq(200)

          # accessory_id = resp.body["data"][0]["id"]
          accessory_id = resp.body.first["id"]
          resp = client.get "#{url}/#{accessory_id}"
          expect(resp.body.count).to eq(1)
          expect(resp.status).to eq(200)
          expect(resp.body.count).to eq(1)
        end

        it "returns empty results for an invalid accessory ID with status 200" do
          invalid_id = SecureRandom.uuid
          resp = client.get "#{url}/#{invalid_id}"
          expect(resp.status).to eq(200)
          expect(resp.body.count).to eq(0)
        end
      end

      context "GET /inventory/models/:id/accessories for a model without accessories" do
        let(:url) { "/inventory#{path}models/#{model_without_accessories.id}/accessories" }

        it "retrieves no accessories for the model and returns status 200" do
          resp = client.get url
          expect(resp.status).to eq(200)
          expect(resp.body.count).to eq(0)
        end

        # FIXME
        it "returns paginated empty results with status 200" do
          resp = client.get "#{url}?page=1&size=1"
          expect(resp.status).to eq(200)
          expect(resp.body.count).to eq(0)
          # expect(resp.body["pagination"]["total_rows"]).to eq(0)
        end
      end
    end
  end
end
