require "spec_helper"
require "pry"
require_relative "_shared"

describe "Swagger Inventory Endpoints - Items Create" do
  context "when creating items for an inventory pool" do
    include_context :setup_models_min_api

    before :each do
      @user, @user_cookies, @user_cookies_str, @cookie_token = create_and_login(:user)
      FactoryBot.create(:access_right,
        inventory_pool_id: @inventory_pool.id,
        user_id: @user.id,
        role: "inventory_manager")

      @model = FactoryBot.create(:leihs_model,
        product: "Test Product",
        is_package: false)

      @building = FactoryBot.create(:building, name: "Test Building")
      @room = FactoryBot.create(:room,
        name: "Test Room",
        building_id: @building.id)
    end

    let(:client) { session_auth_plain_faraday_json_client(cookies: @user_cookies) }
    let(:inventory_pool_id) { @inventory_pool.id }
    let(:url) { "/inventory/#{inventory_pool_id}/items/" }

    context "POST /inventory/:pool-id/items/" do
      it "creates an item with only required fields and returns status 200" do
        item_data = {
          inventory_code: "TEST-#{SecureRandom.hex(4)}",
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id
        }

        resp = client.post url do |req|
          req.body = item_data.to_json
          req.headers["Content-Type"] = "application/json"
          req.headers["Accept"] = "application/json"
          req.headers["x-csrf-token"] = X_CSRF_TOKEN
        end

        expect(resp.status).to eq(200)
        expect(resp.body["inventory_code"]).to eq(item_data[:inventory_code])
        expect(resp.body["model_id"]).to eq(@model.id)
        expect(resp.body["room_id"]).to eq(@room.id)
        expect(resp.body["inventory_pool_id"]).to eq(@inventory_pool.id)
        expect(resp.body["owner_id"]).to eq(@inventory_pool.id)
        expect(resp.body["id"]).not_to be_nil
      end

      it "creates an item with properties fields stored in JSONB and returns status 200" do
        item_data = {
          inventory_code: "TEST-#{SecureRandom.hex(4)}",
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          properties_mac_address: "00:1B:44:11:3A:B7",
          properties_imei_number: "123456789012345"
        }

        resp = client.post url do |req|
          req.body = item_data.to_json
          req.headers["Content-Type"] = "application/json"
          req.headers["Accept"] = "application/json"
          req.headers["x-csrf-token"] = X_CSRF_TOKEN
        end

        expect(resp.status).to eq(200)
        expect(resp.body["inventory_code"]).to eq(item_data[:inventory_code])
        expect(resp.body["properties"]).to be_nil
        expect(resp.body["properties_mac_address"]).to eq("00:1B:44:11:3A:B7")
        expect(resp.body["properties_imei_number"]).to eq("123456789012345")
        expect(resp.body["id"]).not_to be_nil
      end

      it "rejects unpermitted fields based on user role and returns status 400" do
        # Update an existing field to be inactive
        inactive_field = Field.find(id: "properties_mac_address")
        inactive_field.update(active: false)

        item_data = {
          inventory_code: "TEST-#{SecureRandom.hex(4)}",
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          properties_mac_address: "00:1B:44:11:3A:B7"
        }

        resp = client.post url do |req|
          req.body = item_data.to_json
          req.headers["Content-Type"] = "application/json"
          req.headers["Accept"] = "application/json"
          req.headers["x-csrf-token"] = X_CSRF_TOKEN
        end

        expect(resp.status).to eq(400)
        expect(resp.body["error"]).to eq("Unpermitted fields")
        expect(resp.body["unpermitted-fields"]).to include("properties_mac_address")
      end

      it "rejects license-specific fields when creating items and returns status 400" do
        item_data = {
          inventory_code: "TEST-#{SecureRandom.hex(4)}",
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          properties_dongle_id: "DONGLE-12345"
        }

        resp = client.post url do |req|
          req.body = item_data.to_json
          req.headers["Content-Type"] = "application/json"
          req.headers["Accept"] = "application/json"
          req.headers["x-csrf-token"] = X_CSRF_TOKEN
        end

        expect(resp.status).to eq(400)
        expect(resp.body["error"]).to eq("Unpermitted fields")
        expect(resp.body["unpermitted-fields"]).to include("properties_dongle_id")
      end
    end
  end

  context "when creating items with lending_manager role" do
    include_context :setup_models_min_api

    before :each do
      @lending_user, @lending_cookies, @lending_cookies_str, @lending_cookie_token = create_and_login(:user)
      FactoryBot.create(:access_right,
        inventory_pool_id: @inventory_pool.id,
        user_id: @lending_user.id,
        role: "lending_manager")

      @model = FactoryBot.create(:leihs_model,
        product: "Test Product",
        is_package: false)

      @building = FactoryBot.create(:building, name: "Test Building")
      @room = FactoryBot.create(:room,
        name: "Test Room",
        building_id: @building.id)
    end

    let(:client) { session_auth_plain_faraday_json_client(cookies: @lending_cookies) }
    let(:inventory_pool_id) { @inventory_pool.id }
    let(:url) { "/inventory/#{inventory_pool_id}/items/" }

    context "POST /inventory/:pool-id/items/" do
      it "rejects fields not permitted for lending_manager role and returns status 400" do
        item_data = {
          inventory_code: "TEST-#{SecureRandom.hex(4)}",
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          is_inventory_relevant: true
        }

        resp = client.post url do |req|
          req.body = item_data.to_json
          req.headers["Content-Type"] = "application/json"
          req.headers["Accept"] = "application/json"
          req.headers["x-csrf-token"] = X_CSRF_TOKEN
        end

        expect(resp.status).to eq(400)
        expect(resp.body["error"]).to eq("Unpermitted fields")
        expect(resp.body["unpermitted-fields"]).to include("is_inventory_relevant")
      end
    end
  end
end
