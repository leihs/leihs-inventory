require "spec_helper"
require "pry"
require_relative "_shared"

describe "Swagger Inventory Endpoints - Packages Create" do
  context "when creating packages for an inventory pool" do
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

      @item = FactoryBot.create(:item,
        model_id: @regular_model.id,
        inventory_pool_id: @inventory_pool.id,
        owner: @inventory_pool,
        room_id: @room.id)
    end

    let(:client) { session_auth_plain_faraday_json_client(cookies: @user_cookies) }
    let(:inventory_pool_id) { @inventory_pool.id }
    let(:url) { "/inventory/#{inventory_pool_id}/items/" }

    def post_with_headers(client, url, data)
      client.post url do |req|
        req.body = data.to_json
        req.headers["Content-Type"] = "application/json"
        req.headers["Accept"] = "application/json"
        req.headers["x-csrf-token"] = X_CSRF_TOKEN
      end
    end

    context "POST /inventory/:pool-id/items/ with type=package" do
      it "creates a package with item and returns status 200" do
        package_data = {
          inventory_code: "P-TEST-#{SecureRandom.hex(4)}",
          model_id: @package_model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          type: "package",
          item_ids: [@item.id]
        }

        resp = post_with_headers(client, url, package_data)

        expect(resp.status).to eq(200)
        expect(resp.body["inventory_code"]).to eq(package_data[:inventory_code])
        expect(resp.body["model_id"]).to eq(@package_model.id)
        expect(resp.body["id"]).not_to be_nil
        expect(resp.body["item_ids"]).to eq([@item.id])

        # Verify item was assigned to package
        @item.reload
        expect(@item.parent_id).to eq(resp.body["id"])
      end
    end
  end
end
