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

      it "creates a package with multiple items and returns status 200" do
        item2 = FactoryBot.create(:item,
          model_id: @regular_model.id,
          inventory_pool_id: @inventory_pool.id,
          owner: @inventory_pool,
          room_id: @room.id)

        item3 = FactoryBot.create(:item,
          model_id: @regular_model.id,
          inventory_pool_id: @inventory_pool.id,
          owner: @inventory_pool,
          room_id: @room.id)

        package_data = {
          inventory_code: "P-TEST-#{SecureRandom.hex(4)}",
          model_id: @package_model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          type: "package",
          item_ids: [@item.id, item2.id, item3.id]
        }

        resp = post_with_headers(client, url, package_data)

        expect(resp.status).to eq(200)
        expect(resp.body["item_ids"]).to eq([@item.id, item2.id, item3.id])

        # Verify all items were assigned to package
        [@item, item2, item3].each do |item|
          item.reload
          expect(item.parent_id).to eq(resp.body["id"])
        end
      end

      it "rejects package creation with non-package model and returns status 500" do
        package_data = {
          inventory_code: "P-TEST-#{SecureRandom.hex(4)}",
          model_id: @regular_model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          type: "package",
          item_ids: [@item.id]
        }

        resp = post_with_headers(client, url, package_data)

        expect(resp.status).to eq(400)
        expect(resp.body["details"]).to include("Model must have is_package=true")
      end

      it "rejects adding already-assigned items to package and returns status 500" do
        existing_package = FactoryBot.create(:item,
          model_id: @package_model.id,
          inventory_pool_id: @inventory_pool.id,
          owner: @inventory_pool,
          room_id: @room.id)

        @item.update(parent_id: existing_package.id)

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

        expect(resp.status).to eq(400)
        expect(resp.body["details"]).to include("Cannot add packages or already assigned items")
      end

      it "rejects adding package items to another package and returns status 500" do
        package_item = FactoryBot.create(:item,
          model_id: @package_model.id,
          inventory_pool_id: @inventory_pool.id,
          owner: @inventory_pool,
          room_id: @room.id)

        package_data = {
          inventory_code: "P-TEST-#{SecureRandom.hex(4)}",
          model_id: @package_model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          type: "package",
          item_ids: [package_item.id]
        }

        resp = post_with_headers(client, url, package_data)

        expect(resp.status).to eq(400)
        expect(resp.body["details"]).to include("Cannot add packages or already assigned items")
      end

      it "rejects creating multiple packages at once and returns status 400" do
        package_data = {
          count: 3,
          model_id: @package_model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          type: "package",
          item_ids: [@item.id]
        }

        resp = post_with_headers(client, url, package_data)

        expect(resp.status).to eq(400)
        expect(resp.body["error"]).to eq("Cannot create multiple packages at once")
      end

      it "rejects package without items and returns status 500" do
        package_data = {
          inventory_code: "P-TEST-#{SecureRandom.hex(4)}",
          model_id: @package_model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          type: "package",
          item_ids: []
        }

        resp = post_with_headers(client, url, package_data)

        expect(resp.status).to eq(400)
        expect(resp.body["details"]).to include("Package must have at least one item")
      end

      it "rejects duplicate inventory_code and returns 409 with P-prefixed proposed_code" do
        existing_code = "P-DUPLICATE-CODE"
        FactoryBot.create(:item,
          inventory_code: existing_code,
          model_id: @package_model.id,
          inventory_pool_id: @inventory_pool.id,
          owner: @inventory_pool,
          room_id: @room.id)

        package_data = {
          inventory_code: existing_code,
          model_id: @package_model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          type: "package",
          item_ids: [@item.id]
        }

        resp = post_with_headers(client, url, package_data)

        expect(resp.status).to eq(409)
        expect(resp.body["error"]).to eq("Inventory code already exists")
        expect(resp.body["proposed_code"]).to be_a(String)
        expect(resp.body["proposed_code"]).to start_with("P-")
      end

      it "proposes package codes considering both items and packages (shared sequence)" do
        pool_shortname = @inventory_pool.shortname

        # Create items and packages with mixed codes
        FactoryBot.create(:item,
          inventory_code: "#{pool_shortname}1",
          model_id: @regular_model.id,
          inventory_pool_id: @inventory_pool.id,
          owner: @inventory_pool,
          room_id: @room.id)

        FactoryBot.create(:item,
          inventory_code: "P-#{pool_shortname}2",
          model_id: @package_model.id,
          inventory_pool_id: @inventory_pool.id,
          owner: @inventory_pool,
          room_id: @room.id)

        FactoryBot.create(:item,
          inventory_code: "#{pool_shortname}3",
          model_id: @regular_model.id,
          inventory_pool_id: @inventory_pool.id,
          owner: @inventory_pool,
          room_id: @room.id)

        # Try to create package with duplicate code to trigger proposal
        package_data = {
          inventory_code: "P-#{pool_shortname}2",
          model_id: @package_model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          type: "package",
          item_ids: [@item.id]
        }

        resp = post_with_headers(client, url, package_data)

        expect(resp.status).to eq(409)
        # Should propose P-{shortname}4 because max existing is 3 (from regular item)
        expect(resp.body["proposed_code"]).to eq("P-#{pool_shortname}4")
      end
    end
  end
end
