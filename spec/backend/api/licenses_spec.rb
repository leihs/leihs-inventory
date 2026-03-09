require "spec_helper"
require "pry"
require_relative "_shared"

describe "Swagger Inventory Endpoints - Licenses" do
  context "when managing licenses (Software model items)" do
    include_context :setup_models_min_api

    before :each do
      @user, @user_cookies, @user_cookies_str, @cookie_token = create_and_login(:user)
      FactoryBot.create(:access_right,
        inventory_pool_id: @inventory_pool.id,
        user_id: @user.id,
        role: "inventory_manager")

      @software_model = FactoryBot.create(:leihs_model,
        product: "Test Software",
        type: "Software")

      @regular_model = FactoryBot.create(:leihs_model,
        product: "Test Product",
        is_package: false)

      @building = FactoryBot.create(:building, name: "Test Building")
      @room = FactoryBot.create(:room,
        name: "Test Room",
        building_id: @building.id)

      @license = FactoryBot.create(:item,
        inventory_code: "LIC-ORIGINAL",
        model_id: @software_model.id,
        room_id: @room.id,
        inventory_pool_id: @inventory_pool.id,
        owner_id: @inventory_pool.id)
    end

    let(:client) { session_auth_plain_faraday_json_client(cookies: @user_cookies) }
    let(:inventory_pool_id) { @inventory_pool.id }
    let(:items_url) { "/inventory/#{inventory_pool_id}/items/" }

    def post_with_headers(client, url, data)
      client.post url do |req|
        req.body = data.to_json
        req.headers["Content-Type"] = "application/json"
        req.headers["Accept"] = "application/json"
        req.headers["x-csrf-token"] = X_CSRF_TOKEN
      end
    end

    def patch_with_headers(client, url, data)
      client.patch url do |req|
        req.body = data.to_json
        req.headers["Content-Type"] = "application/json"
        req.headers["Accept"] = "application/json"
        req.headers["x-csrf-token"] = X_CSRF_TOKEN
      end
    end

    context "POST /inventory/:pool-id/items/ with type=license" do
      it "creates a license with Software model and returns 200" do
        data = {
          type: "license",
          inventory_code: "LIC-#{SecureRandom.hex(4)}",
          model_id: @software_model.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id
        }

        resp = post_with_headers(client, items_url, data)

        expect(resp.status).to eq(200)
        expect(resp.body["model_id"]).to eq(@software_model.id)
        expect(resp.body["id"]).not_to be_nil
      end

      it "rejects type=license with non-Software model and returns 400" do
        data = {
          type: "license",
          inventory_code: "LIC-#{SecureRandom.hex(4)}",
          model_id: @regular_model.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id
        }

        resp = post_with_headers(client, items_url, data)

        expect(resp.status).to eq(400)
        expect(resp.body["error"]).to eq("Model must be Software type for licenses")
        expect(resp.body["model_id"]).to eq(@regular_model.id)
      end

      it "allows license-specific fields (e.g. properties_dongle_id) and returns 200" do
        data = {
          type: "license",
          inventory_code: "LIC-#{SecureRandom.hex(4)}",
          model_id: @software_model.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          properties_dongle_id: "DONGLE-123"
        }

        resp = post_with_headers(client, items_url, data)

        expect(resp.status).to eq(200)
        expect(resp.body["properties_dongle_id"]).to eq("DONGLE-123")
      end

      it "rejects item-specific fields (e.g. properties_mac_address) when type=license and returns 400" do
        data = {
          type: "license",
          inventory_code: "LIC-#{SecureRandom.hex(4)}",
          model_id: @software_model.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          properties_mac_address: "00:1B:44:11:3A:B7"
        }

        resp = post_with_headers(client, items_url, data)

        expect(resp.status).to eq(400)
        expect(resp.body["error"]).to eq("Unpermitted fields")
        expect(resp.body["unpermitted-fields"]).to include("properties_mac_address")
      end
    end

    context "GET /inventory/:pool-id/items/:id" do
      it "auto-detects license from Software model and returns fields" do
        url = "/inventory/#{inventory_pool_id}/items/#{@license.id}"

        resp = client.get(url) { |req| req.headers["Accept"] = "application/json" }

        expect(resp.status).to eq(200)
        expect(resp.body["id"]).to eq(@license.id)
        expect(resp.body["model_id"]).to eq(@software_model.id)
        expect(resp.body["fields"]).to be_an(Array)
      end

      it "returns item fields for regular (non-Software) model item (no regression)" do
        regular_item = FactoryBot.create(:item,
          inventory_code: "REG-ITEM",
          model_id: @regular_model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id)

        url = "/inventory/#{inventory_pool_id}/items/#{regular_item.id}"

        resp = client.get(url) { |req| req.headers["Accept"] = "application/json" }

        expect(resp.status).to eq(200)
        expect(resp.body["id"]).to eq(regular_item.id)
        expect(resp.body["fields"]).to be_an(Array)
      end
    end

    context "PATCH /inventory/:pool-id/items/:id (update license)" do
      it "auto-detects license from model type and updates successfully" do
        url = "/inventory/#{inventory_pool_id}/items/#{@license.id}"
        data = {
          inventory_code: "LIC-UPDATED",
          model_id: @software_model.id,
          owner_id: @inventory_pool.id
        }

        resp = patch_with_headers(client, url, data)

        expect(resp.status).to eq(200)
        expect(resp.body["id"]).to eq(@license.id)
        expect(resp.body["inventory_code"]).to eq("LIC-UPDATED")
      end

      it "allows license-specific fields on patch without explicit type=license" do
        url = "/inventory/#{inventory_pool_id}/items/#{@license.id}"
        data = {
          inventory_code: @license.inventory_code,
          model_id: @software_model.id,
          owner_id: @inventory_pool.id,
          properties_dongle_id: "DONGLE-UPDATED"
        }

        resp = patch_with_headers(client, url, data)

        expect(resp.status).to eq(200)
        expect(resp.body["properties_dongle_id"]).to eq("DONGLE-UPDATED")
      end

      it "rejects item-specific fields on license patch and returns 400" do
        url = "/inventory/#{inventory_pool_id}/items/#{@license.id}"
        data = {
          inventory_code: @license.inventory_code,
          model_id: @software_model.id,
          owner_id: @inventory_pool.id,
          properties_mac_address: "00:1B:44:11:3A:B7"
        }

        resp = patch_with_headers(client, url, data)

        expect(resp.status).to eq(400)
        expect(resp.body["error"]).to eq("Unpermitted fields")
        expect(resp.body["unpermitted-fields"]).to include("properties_mac_address")
      end
    end
  end
end
