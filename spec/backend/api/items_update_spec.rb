require "spec_helper"
require "pry"
require_relative "_shared"

describe "Swagger Inventory Endpoints - Items Update" do
  context "when updating items for an inventory pool" do
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

      @item = FactoryBot.create(:item,
        inventory_code: "TEST-ORIGINAL",
        model_id: @model.id,
        room_id: @room.id,
        inventory_pool_id: @inventory_pool.id,
        owner_id: @inventory_pool.id)
    end

    let(:client) { session_auth_plain_faraday_json_client(cookies: @user_cookies) }
    let(:inventory_pool_id) { @inventory_pool.id }
    let(:url) { "/inventory/#{inventory_pool_id}/items/" }

    context "PUT /inventory/:pool-id/items/" do
      it "updates an item with basic fields and returns status 200" do
        update_data = {
          id: @item.id,
          inventory_code: "TEST-UPDATED",
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id
        }

        resp = client.put url do |req|
          req.body = update_data.to_json
          req.headers["Content-Type"] = "application/json"
          req.headers["Accept"] = "application/json"
          req.headers["x-csrf-token"] = X_CSRF_TOKEN
        end

        expect(resp.status).to eq(200)
        expect(resp.body["id"]).to eq(@item.id)
        expect(resp.body["inventory_code"]).to eq("TEST-UPDATED")
        expect(resp.body["model_id"]).to eq(@model.id)
      end

      it "updates an item with properties fields and returns status 200" do
        update_data = {
          id: @item.id,
          inventory_code: @item.inventory_code,
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          properties_mac_address: "00:1B:44:11:3A:B7",
          properties_imei_number: "123456789012345"
        }

        resp = client.put url do |req|
          req.body = update_data.to_json
          req.headers["Content-Type"] = "application/json"
          req.headers["Accept"] = "application/json"
          req.headers["x-csrf-token"] = X_CSRF_TOKEN
        end

        expect(resp.status).to eq(200)
        expect(resp.body["id"]).to eq(@item.id)
        expect(resp.body["properties_mac_address"]).to eq("00:1B:44:11:3A:B7")
        expect(resp.body["properties_imei_number"]).to eq("123456789012345")
      end
    end
  end
end
