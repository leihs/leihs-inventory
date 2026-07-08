require "spec_helper"
require "pry"
require_relative "_shared"

describe "Swagger Inventory Endpoints - Items Create" do
  context "when creating items for an inventory pool" do
    include_context :setup_models_min_api

    before :each do
      @user_cookies, @user_cookies_str, @cookie_token = create_and_login_by(@user)

      @model = FactoryBot.create(:leihs_model,
        product: "Test Product",
        is_package: false)

      @package_model = FactoryBot.create(:package_model,
        product: "Test Package Model")

      @building = FactoryBot.create(:building, name: "Test Building")
      @room = FactoryBot.create(:room,
        name: "Test Room",
        building_id: @building.id)
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

    context "POST /inventory/:pool-id/items/" do
      it "creates an item and returns status 200" do
        retired_reason = Faker::Lorem.sentence
        last_check = "2025-11-06"

        item_data = {
          inventory_code: "TEST-#{SecureRandom.hex(4)}",
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          retired: true,
          retired_reason: retired_reason,
          last_check: last_check
        }

        resp = post_with_headers(client, url, item_data)

        expect(resp.status).to eq(200)

        expect(resp.body["inventory_code"]).to eq(item_data[:inventory_code])
        expect(resp.body["model_id"]).to eq(@model.id)
        expect(resp.body["room_id"]).to eq(@room.id)
        expect(resp.body["inventory_pool_id"]).to eq(@inventory_pool.id)
        expect(resp.body["owner_id"]).to eq(@inventory_pool.id)
        expect(resp.body["retired"]).to be true
        expect(resp.body["retired_reason"]).to eq retired_reason
        expect(resp.body["last_check"]).to eq last_check

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

        resp = post_with_headers(client, url, item_data)

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

        resp = post_with_headers(client, url, item_data)

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

        resp = post_with_headers(client, url, item_data)

        expect(resp.status).to eq(400)
        expect(resp.body["error"]).to eq("Unpermitted fields")
        expect(resp.body["unpermitted-fields"]).to include("properties_dongle_id")
      end

      it "rejects disabled fields for the inventory pool and returns status 400" do
        FactoryBot.create(:disabled_field,
          field_id: "properties_mac_address",
          inventory_pool_id: @inventory_pool.id)

        item_data = {
          inventory_code: "TEST-#{SecureRandom.hex(4)}",
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          properties_mac_address: "00:1B:44:11:3A:B7"
        }

        resp = post_with_headers(client, url, item_data)

        expect(resp.status).to eq(400)
        expect(resp.body["error"]).to eq("Unpermitted fields")
        expect(resp.body["unpermitted-fields"]).to include("properties_mac_address")
      end

      it "allows fields disabled in other pools but not current pool and returns status 200" do
        other_pool = FactoryBot.create(:inventory_pool)
        FactoryBot.create(:disabled_field,
          field_id: "properties_mac_address",
          inventory_pool_id: other_pool.id)

        item_data = {
          inventory_code: "TEST-#{SecureRandom.hex(4)}",
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          properties_mac_address: "00:1B:44:11:3A:B7"
        }

        resp = post_with_headers(client, url, item_data)

        expect(resp.status).to eq(200)
        expect(resp.body["properties_mac_address"]).to eq("00:1B:44:11:3A:B7")
      end

      it "rejects creating items for Software models and returns status 400" do
        software_model = FactoryBot.create(:leihs_model,
          product: "Test Software",
          type: "Software")

        item_data = {
          inventory_code: "TEST-#{SecureRandom.hex(4)}",
          model_id: software_model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id
        }

        resp = post_with_headers(client, url, item_data)

        expect(resp.status).to eq(400)
        expect(resp.body["error"]).to eq("Model type 'Software' is not allowed for items")
        expect(resp.body["model_id"]).to eq(software_model.id)
      end

      it "rejects duplicate inventory_code and returns status 409 with proposed_code" do
        existing_code = "DUPLICATE-CODE"
        FactoryBot.create(:item,
          inventory_code: existing_code,
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id)

        item_data = {
          inventory_code: existing_code,
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id
        }

        resp = post_with_headers(client, url, item_data)

        expect(resp.status).to eq(409)
        expect(resp.body["errors"].first["code"]).to eq("DUPLICATE_INVENTORY_CODE")
        expect(resp.body["errors"].first["proposed_code"]).to be_a(String)
      end

      it "proposes item codes considering both items and packages (shared sequence)" do
        pool_shortname = @inventory_pool.shortname

        # Create items and packages with mixed codes
        FactoryBot.create(:item,
          inventory_code: "#{pool_shortname}1",
          model_id: @model.id,
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
          inventory_code: "P-#{pool_shortname}3",
          model_id: @package_model.id,
          inventory_pool_id: @inventory_pool.id,
          owner: @inventory_pool,
          room_id: @room.id)

        # Try to create item with duplicate code to trigger proposal
        item_data = {
          inventory_code: "#{pool_shortname}1",
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id
        }

        resp = post_with_headers(client, url, item_data)

        expect(resp.status).to eq(409)
        # Should propose {shortname}4 because max existing is P-{shortname}3
        expect(resp.body["errors"].first["proposed_code"]).to eq("#{pool_shortname}4")
      end

      it "proposes correct next code when pool shortname is numeric" do
        @inventory_pool.update(shortname: "01")

        FactoryBot.create(:item,
          inventory_code: "011",
          model_id: @model.id,
          inventory_pool_id: @inventory_pool.id,
          owner: @inventory_pool,
          room_id: @room.id)

        item_data = {
          inventory_code: "011",
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id
        }

        resp = post_with_headers(client, url, item_data)

        expect(resp.status).to eq(409)
        expect(resp.body["errors"].first["proposed_code"]).to eq("012")
      end

      it "proposes first code when pool has no items" do
        @inventory_pool.update(shortname: "TEST")
        other_pool = FactoryBot.create(:inventory_pool)
        FactoryBot.create(:item,
          inventory_code: "TAKEN",
          model_id: @model.id,
          inventory_pool_id: other_pool.id,
          owner: other_pool,
          room_id: @room.id)

        item_data = {
          inventory_code: "TAKEN",
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id
        }

        resp = post_with_headers(client, url, item_data)

        expect(resp.status).to eq(409)
        expect(resp.body["errors"].first["proposed_code"]).to eq("TEST1")
      end

      it "proposes next after single item with alphabetic shortname" do
        pool_shortname = @inventory_pool.shortname
        FactoryBot.create(:item,
          inventory_code: "#{pool_shortname}3",
          model_id: @model.id,
          inventory_pool_id: @inventory_pool.id,
          owner: @inventory_pool,
          room_id: @room.id)

        item_data = {
          inventory_code: "#{pool_shortname}3",
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id
        }

        resp = post_with_headers(client, url, item_data)

        expect(resp.status).to eq(409)
        expect(resp.body["errors"].first["proposed_code"]).to eq("#{pool_shortname}4")
      end

      it "proposes max+1 regardless of creation order" do
        pool_shortname = @inventory_pool.shortname
        FactoryBot.create(:item,
          inventory_code: "#{pool_shortname}3",
          model_id: @model.id,
          inventory_pool_id: @inventory_pool.id,
          owner: @inventory_pool,
          room_id: @room.id)
        FactoryBot.create(:item,
          inventory_code: "#{pool_shortname}1",
          model_id: @model.id,
          inventory_pool_id: @inventory_pool.id,
          owner: @inventory_pool,
          room_id: @room.id)

        item_data = {
          inventory_code: "#{pool_shortname}1",
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id
        }

        resp = post_with_headers(client, url, item_data)

        expect(resp.status).to eq(409)
        # max is 3, proposes 4 (gap at 2 is not filled)
        expect(resp.body["errors"].first["proposed_code"]).to eq("#{pool_shortname}4")
      end

      it "proposes correct next after multiple items with numeric shortname" do
        @inventory_pool.update(shortname: "01")
        FactoryBot.create(:item,
          inventory_code: "011",
          model_id: @model.id,
          inventory_pool_id: @inventory_pool.id,
          owner: @inventory_pool,
          room_id: @room.id)
        FactoryBot.create(:item,
          inventory_code: "012",
          model_id: @model.id,
          inventory_pool_id: @inventory_pool.id,
          owner: @inventory_pool,
          room_id: @room.id)

        item_data = {
          inventory_code: "011",
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id
        }

        resp = post_with_headers(client, url, item_data)

        expect(resp.status).to eq(409)
        expect(resp.body["errors"].first["proposed_code"]).to eq("013")
      end

      it "proposes correct next with numeric shortname and mixed items and packages" do
        @inventory_pool.update(shortname: "01")
        FactoryBot.create(:item,
          inventory_code: "011",
          model_id: @model.id,
          inventory_pool_id: @inventory_pool.id,
          owner: @inventory_pool,
          room_id: @room.id)
        FactoryBot.create(:item,
          inventory_code: "P-012",
          model_id: @package_model.id,
          inventory_pool_id: @inventory_pool.id,
          owner: @inventory_pool,
          room_id: @room.id)

        item_data = {
          inventory_code: "011",
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id
        }

        resp = post_with_headers(client, url, item_data)

        expect(resp.status).to eq(409)
        expect(resp.body["errors"].first["proposed_code"]).to eq("013")
      end

      it "rejects duplicate serial_number and returns status 409" do
        FactoryBot.create(:item,
          inventory_code: "SERIAL-ITEM-1",
          serial_number: "SN-12345",
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id)

        item_data = {
          inventory_code: "SERIAL-ITEM-2",
          serial_number: "SN-12345",
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id
        }

        resp = post_with_headers(client, url, item_data)

        expect(resp.status).to eq(409)
        expect(resp.body["errors"].first["code"]).to eq("DUPLICATE_SERIAL_NUMBER")
      end

      [
        ["placeholder", "-"],
        ["whitespace-wrapped placeholder", " - "]
      ].each do |description, serial_number|
        it "allows creating an item when serial_number is a #{description}" do
          inventory_code = "SERIAL-#{SecureRandom.hex(4)}"
          FactoryBot.create(:item,
            inventory_code: "#{inventory_code}-1",
            serial_number: "-",
            model_id: @model.id,
            room_id: @room.id,
            inventory_pool_id: @inventory_pool.id,
            owner_id: @inventory_pool.id)

          item_data = {
            inventory_code: "#{inventory_code}-2",
            serial_number: serial_number,
            model_id: @model.id,
            room_id: @room.id,
            inventory_pool_id: @inventory_pool.id,
            owner_id: @inventory_pool.id
          }

          resp = post_with_headers(client, url, item_data)

          expect(resp.status).to eq(200)
          expect(resp.body["serial_number"]).to eq(serial_number)
        end
      end

      it "returns both errors when inventory_code and serial_number are both duplicates" do
        existing_code = "DUAL-DUP-CODE"
        existing_serial = "SN-DUAL-DUP"
        FactoryBot.create(:item,
          inventory_code: existing_code,
          serial_number: existing_serial,
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id)

        item_data = {
          inventory_code: existing_code,
          serial_number: existing_serial,
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id
        }

        resp = post_with_headers(client, url, item_data)

        expect(resp.status).to eq(409)
        codes = resp.body["errors"].map { |e| e["code"] }
        expect(codes).to include("DUPLICATE_INVENTORY_CODE", "DUPLICATE_SERIAL_NUMBER")
      end

      it "rejects case- and space-insensitive serial_number duplicate and returns status 409" do
        FactoryBot.create(:item,
          inventory_code: "SERIAL-ITEM-3",
          serial_number: "ab cd",
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id)

        item_data = {
          inventory_code: "SERIAL-ITEM-4",
          serial_number: "AB CD",
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id
        }

        resp = post_with_headers(client, url, item_data)

        expect(resp.status).to eq(409)
        expect(resp.body["errors"].first["code"]).to eq("DUPLICATE_SERIAL_NUMBER")
      end

      it "allows creating item with unique serial_number and returns status 200" do
        FactoryBot.create(:item,
          inventory_code: "SERIAL-ITEM-5",
          serial_number: "SN-UNIQUE-1",
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id)

        item_data = {
          inventory_code: "SERIAL-ITEM-6",
          serial_number: "SN-UNIQUE-2",
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id
        }

        resp = post_with_headers(client, url, item_data)

        expect(resp.status).to eq(200)
        expect(resp.body["serial_number"]).to eq("SN-UNIQUE-2")
      end

      it "allows creating with duplicate serial_number when on_conflict overwrite and returns 200" do
        FactoryBot.create(:item,
          inventory_code: "SERIAL-ITEM-7",
          serial_number: "SN-OVERWRITE",
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id)

        item_data = {
          inventory_code: "SERIAL-ITEM-8",
          serial_number: "SN-OVERWRITE",
          on_conflict: {serial_number: "overwrite"},
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id
        }

        resp = post_with_headers(client, url, item_data)

        expect(resp.status).to eq(200)
      end

      it "rejects on_conflict for inventory_code and returns 400" do
        item_data = {
          inventory_code: "SERIAL-ITEM-9",
          on_conflict: {inventory_code: "overwrite"},
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id
        }

        resp = post_with_headers(client, url, item_data)

        expect(resp.status).to eq(400)
        expect(resp.body["errors"].first["code"]).to eq("UNSUPPORTED_CONFLICT_STRATEGY")
      end
    end

    context "with count parameter (batch creation)" do
      it "creates N items with sequential codes and returns status 200" do
        item_data = {
          count: 3,
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id
        }

        resp = post_with_headers(client, url, item_data)

        expect(resp.status).to eq(200)
        expect(resp.body).to be_an(Array)
        expect(resp.body.length).to eq(3)

        # Verify all items share same properties
        resp.body.each do |item|
          expect(item["model_id"]).to eq(@model.id)
          expect(item["room_id"]).to eq(@room.id)
          expect(item["owner_id"]).to eq(@inventory_pool.id)
          expect(item["id"]).to be_a(String)
          expect(item["inventory_code"]).to be_a(String)
        end

        # Verify sequential codes
        codes = resp.body.map { |item| item["inventory_code"] }
        numbers = codes.map { |c| c.gsub(/\D/, "").to_i }
        expect(numbers).to eq(numbers.sort)
        expect(numbers.last - numbers.first).to eq(2) # count - 1
      end

      it "generates codes from highest numeric value, not latest created_at" do
        # Create item with high number (oldest)
        FactoryBot.create(:item,
          inventory_code: "#{@inventory_pool.shortname}105",
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          created_at: 2.days.ago)

        # Create newer item with lower number
        FactoryBot.create(:item,
          inventory_code: "#{@inventory_pool.shortname}103",
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          created_at: 1.day.ago)

        item_data = {
          count: 3,
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id
        }

        resp = post_with_headers(client, url, item_data)

        # Should find max(103, 105) = 105, propose 106, generate 106-108
        # No collision because we use highest numeric value, not created_at
        expect(resp.status).to eq(200)
        expect(resp.body.length).to eq(3)

        codes = resp.body.map { |item| item["inventory_code"] }
        numbers = codes.map { |c| c.gsub(/\D/, "").to_i }
        expect(numbers).to eq([106, 107, 108])
      end

      it "rejects duplicate serial_number on batch create and returns 409" do
        FactoryBot.create(:item,
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          serial_number: "SN-BATCH-DUP")

        resp = post_with_headers(client, url, {
          count: 2,
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          serial_number: "SN-BATCH-DUP"
        })

        expect(resp.status).to eq(409)
        expect(resp.body["errors"].first["code"]).to eq("DUPLICATE_SERIAL_NUMBER")
      end

      it "allows batch create when serial_number is a placeholder" do
        FactoryBot.create(:item,
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          serial_number: "-")

        resp = post_with_headers(client, url, {
          count: 2,
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          serial_number: "-"
        })

        expect(resp.status).to eq(200)
        expect(resp.body.length).to eq(2)
        expect(resp.body.map { |item| item["serial_number"] }).to eq(["-", "-"])
      end

      it "allows batch create with duplicate serial_number when on_conflict overwrite and returns 200" do
        FactoryBot.create(:item,
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          serial_number: "SN-BATCH-OVERWRITE")

        resp = post_with_headers(client, url, {
          count: 2,
          model_id: @model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          serial_number: "SN-BATCH-OVERWRITE",
          on_conflict: {serial_number: "overwrite"}
        })

        expect(resp.status).to eq(200)
        expect(resp.body.length).to eq(2)
      end
    end
  end

  context "when creating items with lending_manager role" do
    include_context :setup_models_min_api

    before :each do
      @lending_user = FactoryBot.create(:user, login: Faker::Lorem.word, password: "password")
      FactoryBot.create(:access_right,
        inventory_pool_id: @inventory_pool.id,
        user_id: @lending_user.id,
        role: "lending_manager")
      @lending_cookies, @lending_cookies_str, @lending_cookie_token = create_and_login_by(@lending_user)

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

    def post_with_headers(client, url, data)
      client.post url do |req|
        req.body = data.to_json
        req.headers["Content-Type"] = "application/json"
        req.headers["Accept"] = "application/json"
        req.headers["x-csrf-token"] = X_CSRF_TOKEN
      end
    end

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

        resp = post_with_headers(client, url, item_data)

        expect(resp.status).to eq(400)
        expect(resp.body["error"]).to eq("Unpermitted fields")
        expect(resp.body["unpermitted-fields"]).to include("is_inventory_relevant")
      end
    end
  end
end
