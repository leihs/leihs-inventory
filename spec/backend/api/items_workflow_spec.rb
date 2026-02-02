require "spec_helper"
require "cgi"
require_relative "_shared"

describe "Swagger Inventory Endpoints - Items Workflow (fields, filter_q/search_term, PATCH)" do
  # End-to-end workflows: create 5 items, filter/search to get 3, then update those entries.
  # Szenario1: GET /fields?target_type=item → GET /items?filter_q= → PATCH /items/{id}
  # Szenario2: GET /items?search_term= (inventory_code / item columns) → PATCH /items/{id}
  #
  context "when managing items in an inventory pool" do
    include_context :setup_models_min_api

    before :each do
      @user, @user_cookies, @user_cookies_str, @cookie_token = create_and_login(:user)
      FactoryBot.create(:access_right,
        inventory_pool_id: @inventory_pool.id,
        user_id: @user.id,
        role: "inventory_manager")

      @model = FactoryBot.create(:leihs_model, product: "Workflow Model", is_package: false)
      @building = FactoryBot.create(:building, name: "Test Building")
      @room = FactoryBot.create(:room, name: "Test Room", building_id: @building.id)

      # 5 items: WF-A01, WF-A02, WF-A03 (match "WF-A"), WF-B01, WF-B02 (do not match)
      @items = [
        FactoryBot.create(:item,
          inventory_code: "WF-A01",
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          status_note: nil),
        FactoryBot.create(:item,
          inventory_code: "WF-A02",
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          status_note: nil),
        FactoryBot.create(:item,
          inventory_code: "WF-A03",
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          status_note: nil),
        FactoryBot.create(:item,
          inventory_code: "WF-B01",
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          status_note: nil),
        FactoryBot.create(:item,
          inventory_code: "WF-B02",
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          status_note: nil)
      ]
    end

    let(:client) { session_auth_plain_faraday_json_client(cookies: @user_cookies) }
    let(:inventory_pool_id) { @inventory_pool.id }
    let(:items_url) { "/inventory/#{inventory_pool_id}/items/" }
    let(:fields_url) { "/inventory/#{inventory_pool_id}/fields/" }

    def patch_item(item_id, data)
      client.patch "/inventory/#{inventory_pool_id}/items/#{item_id}" do |req|
        req.body = data.to_json
        req.headers["Content-Type"] = "application/json"
        req.headers["Accept"] = "application/json"
        req.headers["x-csrf-token"] = X_CSRF_TOKEN
      end
    end

    context "Szenario1: GET /fields?target_type=item → GET /items?filter_q= → PATCH /items/{itemId}" do
      it "fetches item fields, filters 3 items by filter_q, then updates each of the 3" do
        # 1. GET /fields?target_type=item
        fields_resp = client.get fields_url do |req|
          req.params["target_type"] = "item"
          req.headers["Accept"] = "application/json"
        end
        expect(fields_resp.status).to eq(200)
        expect(fields_resp.body).to have_key("fields")
        expect(fields_resp.body["fields"]).to be_an(Array)

        # 2. GET /items?filter_q= (EDN: inventory_code matches WF-A* → 3 items)
        filter_edn = '{:inventory_code {:$ilike "WF-A"}}'
        items_resp = client.get("#{items_url}?filter_q=#{CGI.escape(filter_edn)}")
        expect(items_resp.status).to eq(200)
        expect(items_resp.body).to be_an(Array)
        filtered = items_resp.body
        expect(filtered.size).to eq(3), "filter_q should return exactly 3 items (WF-A01, WF-A02, WF-A03), got #{filtered.size}: #{filtered.map { |i| i["inventory_code"] }}"
        ids = filtered.map { |i| i["id"].to_s }
        expect(ids).to contain_exactly(@items[0].id.to_s, @items[1].id.to_s, @items[2].id.to_s)

        # 3. PATCH each of the 3 items (only send the field being modified)
        filtered.each_with_index do |item, idx|
          patch_resp = patch_item(item["id"], {
            status_note: "updated-by-workflow-#{idx}"
          })
          expect(patch_resp.status).to eq(200), "PATCH item #{item["id"]}: #{patch_resp.body}"
          expect(patch_resp.body["status_note"]).to eq("updated-by-workflow-#{idx}")
        end
      end
    end

    context "Szenario2: GET /items?search_term= (inventory_code) → PATCH /items/{itemId}" do
      it "searches items by search_term (inventory_code), gets 3, then updates each of the 3" do
        # 1. GET /items?search_term=WF-A (matches inventory_code, serial_number, etc. → 3 items)
        search_resp = client.get items_url do |req|
          req.params["search_term"] = "WF-A"
          req.headers["Accept"] = "application/json"
        end
        expect(search_resp.status).to eq(200)
        expect(search_resp.body).to be_an(Array)
        searched = search_resp.body
        expect(searched.size).to eq(3), "search_term=WF-A should return 3 items, got #{searched.size}: #{searched.map { |i| i["inventory_code"] }}"
        ids = searched.map { |i| i["id"].to_s }
        expect(ids).to contain_exactly(@items[0].id.to_s, @items[1].id.to_s, @items[2].id.to_s)

        # 2. PATCH each of the 3 items (only send the field being modified)
        searched.each_with_index do |item, idx|
          patch_resp = patch_item(item["id"], {
            note: "szenario2-update-#{idx}"
          })
          expect(patch_resp.status).to eq(200), "PATCH item #{item["id"]}: #{patch_resp.body}"
          expect(patch_resp.body["note"]).to eq("szenario2-update-#{idx}")
        end
      end
    end
  end
end
