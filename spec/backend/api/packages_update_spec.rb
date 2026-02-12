require "spec_helper"
require "pry"
require_relative "_shared"

describe "Swagger Inventory Endpoints - Packages Update" do
  context "when updating packages for an inventory pool" do
    include_context :setup_models_min_api

    before :each do
      @user, @user_cookies, @user_cookies_str, @cookie_token = create_and_login(:user)
      FactoryBot.create(:access_right,
        inventory_pool_id: @inventory_pool.id,
        user_id: @user.id,
        role: "inventory_manager")

      @package_model = FactoryBot.create(:package_model,
        product: "Test Package Model")

      @regular_model = FactoryBot.create(:leihs_model,
        product: "Regular Item Model",
        is_package: false)

      @building = FactoryBot.create(:building, name: "Test Building")
      @room = FactoryBot.create(:room,
        name: "Test Room",
        building_id: @building.id)

      @item1 = FactoryBot.create(:item,
        model_id: @regular_model.id,
        inventory_pool_id: @inventory_pool.id,
        owner: @inventory_pool,
        room_id: @room.id)

      @item2 = FactoryBot.create(:item,
        model_id: @regular_model.id,
        inventory_pool_id: @inventory_pool.id,
        owner: @inventory_pool,
        room_id: @room.id)

      @item3 = FactoryBot.create(:item,
        model_id: @regular_model.id,
        inventory_pool_id: @inventory_pool.id,
        owner: @inventory_pool,
        room_id: @room.id)

      @package = FactoryBot.create(:item,
        model_id: @package_model.id,
        inventory_pool_id: @inventory_pool.id,
        owner: @inventory_pool,
        room_id: @room.id)

      @item1.update(parent_id: @package.id)
    end

    let(:client) { session_auth_plain_faraday_json_client(cookies: @user_cookies) }
    let(:inventory_pool_id) { @inventory_pool.id }
    let(:url) { "/inventory/#{inventory_pool_id}/items/#{@package.id}" }

    def patch_with_headers(client, url, data)
      client.patch url do |req|
        req.body = data.to_json
        req.headers["Content-Type"] = "application/json"
        req.headers["Accept"] = "application/json"
        req.headers["x-csrf-token"] = X_CSRF_TOKEN
      end
    end

    context "PATCH /inventory/:pool-id/items/:item-id with item_ids" do
      it "updates package items (full replacement) and returns status 200" do
        update_data = {
          item_ids: [@item2.id, @item3.id]
        }

        resp = patch_with_headers(client, url, update_data)

        expect(resp.status).to eq(200)
        expect(resp.body["id"]).to eq(@package.id)
        expect(resp.body["item_ids"]).to match_array([@item2.id, @item3.id])

        # Verify old item removed from package
        @item1.reload
        expect(@item1.parent_id).to be_nil

        # Verify new items assigned to package
        @item2.reload
        @item3.reload
        expect(@item2.parent_id).to eq(@package.id)
        expect(@item3.parent_id).to eq(@package.id)
      end
    end
  end
end
