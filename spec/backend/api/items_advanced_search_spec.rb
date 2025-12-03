require "spec_helper"
require "pry"
require_relative "_shared"

describe "Swagger Inventory Endpoints - Items Filtering" do
  context "when filtering items for an inventory pool" do
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
    let(:base_url) { "/inventory/#{inventory_pool_id}/items-filter/" }

    def post_with_headers(client, url, data)
      client.post url do |req|
        req.body = data.to_json
        req.headers["Content-Type"] = "application/json"
        req.headers["Accept"] = "application/json"
        req.headers["x-csrf-token"] = X_CSRF_TOKEN
      end
    end

    context "POST /inventory/:pool-id/items/" do
      let(:entry) {
        db = defined?(Sequel::Model) ? Sequel::Model.db : database

        entry_id = db[:items].insert(
          inventory_code: "ITZ21122",
          model_id: "21c2b7ac-0645-5d7e-8b17-ad8047ca89da",
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          retired: false,
          retired_reason: "test",
          properties: {
            reference: "invoice",
            ampere: "12A",
            imei_number: "C-Band",
            mac_address: "00:1B:44:11:3A:B7",
            contract_expiration: "2040-11-06",
            warranty_expiration: "2030-11-06"
          }.to_json,
          last_check: "2025-11-06"
        )

        db[:items][id: entry_id]
      }

      describe "filtering by properties" do
        it "filters by properties_reference=invoice (exact match)" do
          resp = client.get("#{base_url}?filter=properties_reference~invoice")
          expect(resp.status).to eq(200)
        end

        it "filters by properties_reference~inv (partial match)" do
          resp = client.get("#{base_url}?filter=properties_reference~~inv")
          expect(resp.status).to eq(200)
        end

        it "filters by properties_imei_number=C-Band (exact match)" do
          resp = client.get("#{base_url}?filter=properties_imei_number~C-Band")
          expect(resp.status).to eq(200)
        end
      end

      describe "filtering by dates" do
        it "filters by invoice_date=2013-09-19 (exact match)" do
          resp = client.get("#{base_url}?filter=invoice_date~2013-09-19")
          expect(resp.status).to eq(200)
        end

        it "filters by invoice_date>2013-09-19 (greater than)" do
          resp = client.get("#{base_url}?filter=invoice_date>2013-09-19")
          expect(resp.status).to eq(200)
        end

        it "filters by invoice_date<2024-12-31 (less than)" do
          resp = client.get("#{base_url}?filter=invoice_date<2024-12-31")
          expect(resp.status).to eq(200)
        end

        it "filters by invoice_date>=2013-09-19 (greater than or equal)" do
          resp = client.get("#{base_url}?filter=invoice_date>=2013-09-19")
          expect(resp.status).to eq(200)
        end

        it "filters by invoice_date<=2024-12-31 (less than or equal)" do
          resp = client.get("#{base_url}?filter=invoice_date<=2024-12-31")
          expect(resp.status).to eq(200)
        end

        it "filters by invoice_date range 2013-09-19..2020-12-31" do
          resp = client.get("#{base_url}?filter=invoice_date~2013-09-19;2020-12-31")
          expect(resp.status).to eq(200)
        end

        it "filters by last_check>2024-01-01 (greater than)" do
          resp = client.get("#{base_url}?filter=last_check>2024-01-01")
          expect(resp.status).to eq(200)
        end
      end

      describe "filtering by boolean fields" do
        it "filters by is_borrowable=false" do
          resp = client.get("#{base_url}?filter=is_borrowable~false")
          expect(resp.status).to eq(200)
        end

        it "filters by is_incomplete=false" do
          resp = client.get("#{base_url}?filter=is_incomplete~false")
          expect(resp.status).to eq(200)
        end
      end

      describe "filtering by identifiers" do
        it "filters by inventory_code=ITZ21122 (exact match)" do
          resp = client.get("#{base_url}?filter=inventory_code~ITZ21122")
          expect(resp.status).to eq(200)
        end

        it "filters by inventory_code~ITZ211 (partial match)" do
          resp = client.get("#{base_url}?filter=inventory_code~~ITZ211")
          expect(resp.status).to eq(200)
        end

        it "filters by supplier_id" do
          resp = client.get(
            "#{base_url}?filter=supplier_id~d20338d8-1182-5ea6-91da-ea96c3a0a76a"
          )
          expect(resp.status).to eq(200)
        end

        it "filters by model_id" do
          resp = client.get(
            "#{base_url}?filter=model_id~21c2b7ac-0645-5d7e-8b17-ad8047ca89da"
          )
          expect(resp.status).to eq(200)
        end

        it "filters by user_name~itz (partial match)" do
          resp = client.get("#{base_url}?filter=user_name~~itz")
          expect(resp.status).to eq(200)
        end
      end

      describe "invalid keys: " do
        it "invalid properties_" do
          resp = client.get(
            "#{base_url}?filter=properties_abc~12"
          )
          expect(resp.status).to eq(400)
        end

        it "invalid key" do
          resp = client.get(
            "#{base_url}?filter=invalid_key~value"
          )
          expect(resp.status).to eq(400)
        end

        it "invalid op" do
          resp = client.get(
            "#{base_url}?filter=properties_reference=invoice"
          )
          expect(resp.status).to eq(400)
        end
      end

      describe "filter-combinations" do
        # it "filters with AND: properties_reference=invoice | properties_ampere=12A" do
        #   resp = client.get(
        #     "#{base_url}?filter=properties_reference~invoice|properties_ampere~12A"
        #   )
        #   expect(resp.status).to eq(200)
        # end

        it "filters with OR: properties_reference=invoice || properties_reference=creditnote" do
          resp = client.get(
            "#{base_url}?filter=properties_reference~invoice||properties_reference~creditnote"
          )
          expect(resp.status).to eq(200)
        end

        it "filters with AND: inventory_code=ITZ21122 | model_id=21c2b7ac-0645-5d7e-8b17-ad8047ca89da" do
          resp = client.get(
            "#{base_url}?filter=inventory_code~ITZ21122|model_id~21c2b7ac-0645-5d7e-8b17-ad8047ca89da"
          )
          expect(resp.status).to eq(200)
        end

        it "filters with OR: user_name~itz || user_name~admin" do
          resp = client.get(
            "#{base_url}?filter=user_name~~itz||user_name~~admin"
          )
          expect(resp.status).to eq(200)
        end

        it "filters with date range OR: invoice_date>2013-09-19 || invoice_date<2020-01-01" do
          resp = client.get(
            "#{base_url}?filter=invoice_date>2013-09-19||invoice_date<2020-01-01"
          )
          expect(resp.status).to eq(200)
        end
      end
    end
  end
end
