require "spec_helper"
require "pry"
require_relative "../_shared"

describe "Inventory API Endpoints - Model Properties" do
  context "when fetching properties for a specific model in an inventory pool" do
    include_context :setup_models_api, "inventory_manager"

    let(:model_with_properties) { @models.first }
    let(:model_without_properties) { @models.third }

    before :each do
      @user, @user_cookies, @user_cookies_str, @cookie_token = create_and_login(:user)
      @path = "/#{@inventory_pool_id}/"
    end

    let(:client) { session_auth_plain_faraday_json_client(cookies: @user_cookies) }

    context "GET /inventory/models/:id/properties for a model with properties" do
      let(:url) { "/inventory#{@path}models/#{model_with_properties.id}/properties" }

      it "retrieves all properties for the model and returns status 200" do
        resp = client.get url
        expect(resp.status).to eq(200)
        expect(resp.body["pagination"]["total_rows"]).to eq(1)
      end

      it "retrieves paginated property results and returns status 200" do
        resp = client.get "#{url}?page=1&size=1"
        expect(resp.status).to eq(200)
        expect(resp.body["pagination"]["total_rows"]).to eq(1)
      end

      it "retrieves specific property details and returns status 200" do
        resp = client.get "#{url}?page=1&size=1"
        expect(resp.status).to eq(200)

        property_id = resp.body["data"][0]["id"]
        resp = client.get "#{url}/#{property_id}"
        expect(resp.status).to eq(200)
        expect(resp.body["data"].count).to eq(1)
      end

      it "returns no results for an invalid property ID and status 200" do
        invalid_id = SecureRandom.uuid
        resp = client.get "#{url}/#{invalid_id}"
        expect(resp.status).to eq(200)
        expect(resp.body["data"].count).to eq(0)
      end
    end

    context "GET /inventory/models/:id/properties for a model without properties" do
      let(:url) { "/inventory#{@path}models/#{model_without_properties.id}/properties" }

      it "retrieves no properties for the model and returns status 200" do
        resp = client.get url
        expect(resp.status).to eq(200)
        expect(resp.body["pagination"]["total_rows"]).to eq(0)
      end

      it "retrieves paginated results with no properties and returns status 200" do
        resp = client.get "#{url}?page=1&size=1"
        expect(resp.status).to eq(200)
        expect(resp.body["pagination"]["total_rows"]).to eq(0)
      end
    end
  end
end
