require "spec_helper"
require "cgi"
require_relative "_shared"

# =============================================================================
# ITEMS LIST PAGINATION & FILTERING TESTS
# =============================================================================
#
# This spec provides comprehensive test coverage for:
#   - GET /inventory/:pool-id/items?page=1&filter_q=<EDN>  (EDN-based filtering)
#   - GET /inventory/:pool-id/items?page=1&search=<term>   (text search)
#
# COVERAGE SUMMARY (118+ tests):
#   - UUID fields (7):     id, inventory_pool_id, owner_id, supplier_id,
#                          model_id, room_id, building_id
#   - Numeric fields (1):  price ($eq, $gte, $lte)
#   - Boolean fields (6):  is_borrowable, is_broken, is_incomplete,
#                          is_inventory_relevant, needs_permission, retired
#   - Date fields (2):     invoice_date, last_check ($eq, $gte, $lte)
#   - String fields (12):  inventory_code, serial_number, shelf, user_name,
#                          retired_reason, status_note, note, invoice_number,
#                          name, insurance_number, item_version, responsible
#   - Properties fields:   properties_reference, properties_imei_number,
#                          properties_warranty_expiration, etc.
#   - Logical operators:   $and, $or (including nested combinations)
#   - Search parameter:    text search across multiple fields
#
# SUPPORTED OPERATORS:
#   $eq, $gte, $lte, $ilike, $and, $or
#
# REMOVED OPERATORS (design decision - not required by frontend UI):
#   $gt, $lt   - UI uses ranges with $gte/$lte only (inclusive boundaries)
#   $ne        - UI doesn't need "not equal" filtering
#   $exists    - UI doesn't filter by field presence/absence
#
# PROPERTIES FIELD LIMITATION:
#   Properties fields (properties_*) are validated against the /fields API.
#   Only fields configured in /fields will work; others return 400.
#   Some properties fields are commented out as they require /fields config.
#
# LICENSE FIELDS:
#   License-specific filtering should be in licenses_list_pagination_spec.rb
#   as licenses have a different endpoint (/inventory/:pool-id/licenses/).
#
# =============================================================================

describe "Swagger Inventory Endpoints - Items List (pagination)" do
  # GET /inventory/:pool-id/items?page=1&filter_q=... returns paginated response:
  # { "data" => [...], "pagination" => { "page", "size", "total_rows", "total_pages" } }
  # Same tests as items_filter_q_spec but using ?page=1 so the backend returns the paginated shape.
  # Filter: MQL-style EDN when filter_q is present.
  #
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
      @supplier = FactoryBot.create(:supplier, id: "d20338d8-1182-5ea6-91da-ea96c3a0a76a")
    end

    let(:client) { session_auth_plain_faraday_json_client(cookies: @user_cookies) }
    let(:inventory_pool_id) { @inventory_pool.id }
    let(:url) { "/inventory/#{inventory_pool_id}/items/" }
    let(:base_url) { "/inventory/#{inventory_pool_id}/items/" }

    def filter_url(edn_str)
      "#{base_url}?page=1&filter_q=#{CGI.escape(edn_str)}"
    end

    # GET items?page=1&filter_q= with EDN, expect 200, return data array (from paginated response)
    def fetch_filtered(edn_str)
      resp = client.get(filter_url(edn_str))
      expect(resp.status).to eq(200), "expected 200 but got #{resp.status}: #{resp.body}"
      expect(resp.body).to be_a(Hash)
      expect(resp.body).to have_key("data")
      expect(resp.body).to have_key("pagination")
      expect(resp.body["data"]).to be_an(Array)
      resp.body["data"]
    end

    context "GET /inventory/:pool-id/items?page=1&filter_q= (EDN filter)" do
      # Entry: is_borrowable=false, price=100.0, retired_reason=nil
      # Contains ALL properties fields for comprehensive testing
      let!(:entry) {
        model = FactoryBot.create(:leihs_model, id: SecureRandom.uuid, product: "Abc Model")
        FactoryBot.create(:item,
          inventory_code: "ITZ21122",
          model_id: model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          supplier_id: @supplier.id,
          retired: nil,
          retired_reason: nil,
          invoice_date: "2013-09-20",
          last_check: "2025-11-06",
          is_borrowable: false,
          is_broken: false,
          is_incomplete: false,
          user_name: "itz",
          responsible: "IT Department",
          price: 100.0,
          # Additional fields for comprehensive filter testing
          serial_number: "SN-12345-ABC",
          shelf: "Shelf-A1",
          status_note: "Available for loan",
          note: "Test item note",
          invoice_number: "INV-2013-001",
          name: "Test Item Name",
          insurance_number: "INS-98765",
          item_version: "v1.0",
          properties: {
            # All Item properties fields from AS_WORKFLOW.md
            reference: "invoice",
            ampere: "12A",
            ankunftsdatum: "2024-01-15",
            ankunftsnotiz: "Arrived in good condition",
            ankunftszustand: "OK",
            anschaffungskategorie: "IT Equipment",
            contract_expiration: "2040-11-06",
            dezentrales_budget: true,
            electrical_power: "500W",
            imei_number: "C-Band",
            installation_status: "installed",
            mac_address: "00:1B:44:11:3A:B7",
            umzug: "Building A",
            warranty_expiration: "2030-11-06",
            zielraum: "Room 101"
          }.to_json)
      }

      # Second item: is_borrowable=true, price=250.0, retired_reason set (contrary values)
      # Has EMPTY properties (for testing nil/empty properties case)
      let!(:entry_borrowable) {
        model = FactoryBot.create(:leihs_model, id: SecureRandom.uuid, product: "Abc Model 2")
        FactoryBot.create(:item,
          inventory_code: "ITZ21123",
          model_id: model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          supplier_id: @supplier.id,
          retired: nil,
          retired_reason: "end of life",
          invoice_date: "2013-09-19",
          last_check: "2025-11-06",
          is_borrowable: true,
          is_broken: true,
          is_incomplete: true,
          user_name: "borrowable_user",
          price: 250.0,
          properties: {}.to_json)
      }

      # Third item: is_borrowable=false, price=50.0 (for numeric range tests)
      # Has NO properties set (nil) - for testing nil properties case
      let!(:entry_low_price) {
        model = FactoryBot.create(:leihs_model, id: SecureRandom.uuid, product: "Abc Model 3")
        FactoryBot.create(:item,
          inventory_code: "ITZ21124",
          model_id: model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          supplier_id: @supplier.id,
          retired: nil,
          retired_reason: nil,
          invoice_date: "2013-09-18",
          last_check: "2025-11-06",
          is_borrowable: false,
          is_broken: false,
          is_incomplete: false,
          user_name: "low_price_user",
          price: 50.0,
          properties: nil)
      }

      # Alias for backward compatibility with existing tests
      let!(:entry_with_retired_reason) { entry_borrowable }

      describe "filtering by properties" do
        # Test all properties fields from AS_WORKFLOW.md
        # Note: Only fields configured in /fields API will work.
        # Fields not in /fields API will return 400 - these are skipped.

        it "filters by properties_reference (:$eq)" do
          data = fetch_filtered('{:properties_reference {:$eq "invoice"}}')
          expect(data.map { |i| i["id"] }).to include(entry[:id].to_s)
          expect(data.size).to be >= 1
        end

        it "filters by properties_reference (:$ilike partial match)" do
          data = fetch_filtered('{:properties_reference {:$ilike "inv"}}')
          expect(data.map { |i| i["id"] }).to include(entry[:id].to_s)
          expect(data.size).to be >= 1
        end

        it "filters by properties_imei_number (:$eq)" do
          data = fetch_filtered('{:properties_imei_number {:$eq "C-Band"}}')
          expect(data.map { |i| i["id"] }).to include(entry[:id].to_s)
          expect(data.size).to be >= 1
        end

        it "filters by properties_warranty_expiration (:$ilike)" do
          data = fetch_filtered('{:properties_warranty_expiration {:$ilike "2030"}}')
          expect(data.map { |i| i["id"] }).to include(entry[:id].to_s)
        end

        it "filters by properties_contract_expiration (:$ilike)" do
          data = fetch_filtered('{:properties_contract_expiration {:$ilike "2040"}}')
          expect(data.map { |i| i["id"] }).to include(entry[:id].to_s)
        end

        it "filters by properties_mac_address (:$ilike)" do
          data = fetch_filtered('{:properties_mac_address {:$ilike "00:1B"}}')
          expect(data.map { |i| i["id"] }).to include(entry[:id].to_s)
        end

        # Note: The following properties fields are in the test data but may not be
        # configured in /fields API. Uncomment when fields are added to /fields API:
        # - properties_ampere
        # - properties_ankunftsdatum
        # - properties_ankunftsnotiz
        # - properties_ankunftszustand
        # - properties_anschaffungskategorie
        # - properties_electrical_power
        # - properties_installation_status
        # - properties_umzug
        # - properties_zielraum
      end

      describe "filtering items with nil/empty properties" do
        it "filter excludes items with nil properties when property has value" do
          # entry has properties.reference = "invoice"
          # entry_borrowable has properties = {} (empty)
          # entry_low_price has properties = nil
          data = fetch_filtered('{:properties_reference {:$eq "invoice"}}')
          ids = data.map { |i| i["id"] }
          expect(ids).to include(entry[:id].to_s)
          expect(ids).not_to include(entry_borrowable[:id].to_s)
          expect(ids).not_to include(entry_low_price[:id].to_s)
        end

        it "filter by ilike excludes items without matching property" do
          data = fetch_filtered('{:properties_imei_number {:$ilike "C-Band"}}')
          ids = data.map { |i| i["id"] }
          expect(ids).to include(entry[:id].to_s)
          # Items with empty/nil properties should not match
          expect(ids).not_to include(entry_borrowable[:id].to_s)
          expect(ids).not_to include(entry_low_price[:id].to_s)
        end
      end

      describe "filtering by dates" do
        it "filters by invoice_date (exact :$eq)" do
          data = fetch_filtered('{:invoice_date "2013-09-20"}')
          expect(data.map { |i| i["id"] }).to include(entry[:id].to_s)
        end

        # Note: $gt and $lt operators removed - not required by UI (see TODO.md)

        it "filters by invoice_date (:$gte)" do
          data = fetch_filtered('{:invoice_date {:$gte "2013-09-19"}}')
          expect(data.map { |i| i["id"] }).to include(entry[:id].to_s)
        end

        it "filters by invoice_date (:$lte)" do
          data = fetch_filtered('{:invoice_date {:$lte "2024-12-31"}}')
          expect(data.map { |i| i["id"] }).to include(entry[:id].to_s)
        end

        it "filters by invoice_date range (:$gte and :$lte)" do
          data = fetch_filtered('{:invoice_date {:$gte "2013-09-19" :$lte "2020-12-31"}}')
          expect(data.map { |i| i["id"] }).to include(entry[:id].to_s)
        end

        it "filters by last_check (:$gte)" do
          data = fetch_filtered('{:last_check {:$gte "2024-01-01"}}')
          expect(data.map { |i| i["id"] }).to include(entry[:id].to_s)
        end

        it "filters by date range using :$and (invoice_date from/to)" do
          data = fetch_filtered('{:$and [{:invoice_date {:$gte "2013-01-01"}} {:invoice_date {:$lte "2013-12-31"}}]}')
          ids = data.map { |i| i["id"] }
          expect(ids).to include(entry[:id].to_s)
          expect(ids).to include(entry_borrowable[:id].to_s)
          expect(ids).to include(entry_low_price[:id].to_s)
        end

        it "filters by date range using :$and excludes out-of-range items" do
          data = fetch_filtered('{:$and [{:invoice_date {:$gte "2014-01-01"}} {:invoice_date {:$lte "2014-12-31"}}]}')
          ids = data.map { |i| i["id"] }
          expect(ids).not_to include(entry[:id].to_s)
          expect(ids).not_to include(entry_borrowable[:id].to_s)
          expect(ids).not_to include(entry_low_price[:id].to_s)
        end
      end

      describe "filtering by boolean fields" do
        describe "is_borrowable" do
          it "filters by is_borrowable = false" do
            data = fetch_filtered("{:is_borrowable false}")
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).to include(entry_low_price[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
          end

          it "filters by is_borrowable = true" do
            data = fetch_filtered("{:is_borrowable true}")
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry_borrowable[:id].to_s)
            expect(ids).not_to include(entry[:id].to_s)
            expect(ids).not_to include(entry_low_price[:id].to_s)
          end
        end

        describe "is_broken" do
          it "filters by is_broken = false" do
            data = fetch_filtered("{:is_broken false}")
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).to include(entry_low_price[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
          end

          it "filters by is_broken = true" do
            data = fetch_filtered("{:is_broken true}")
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry_borrowable[:id].to_s)
            expect(ids).not_to include(entry[:id].to_s)
          end
        end

        describe "is_incomplete" do
          it "filters by is_incomplete = false" do
            data = fetch_filtered("{:is_incomplete false}")
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).to include(entry_low_price[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
          end

          it "filters by is_incomplete = true" do
            data = fetch_filtered("{:is_incomplete true}")
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry_borrowable[:id].to_s)
            expect(ids).not_to include(entry[:id].to_s)
          end
        end
      end

      describe "filtering by numeric fields (price)" do
        it "filters by price :$eq" do
          data = fetch_filtered("{:price {:$eq 100.0}}")
          ids = data.map { |i| i["id"] }
          expect(ids).to include(entry[:id].to_s)
          expect(ids).not_to include(entry_borrowable[:id].to_s)
          expect(ids).not_to include(entry_low_price[:id].to_s)
        end

        # Note: $gt and $lt operators removed - not required by UI (see TODO.md)

        it "filters by price :$gte (greater than or equal)" do
          data = fetch_filtered("{:price {:$gte 100}}")
          ids = data.map { |i| i["id"] }
          expect(ids).to include(entry[:id].to_s)
          expect(ids).to include(entry_borrowable[:id].to_s)
          expect(ids).not_to include(entry_low_price[:id].to_s)
        end

        it "filters by price :$lte (less than or equal)" do
          data = fetch_filtered("{:price {:$lte 100}}")
          ids = data.map { |i| i["id"] }
          expect(ids).to include(entry[:id].to_s)
          expect(ids).to include(entry_low_price[:id].to_s)
          expect(ids).not_to include(entry_borrowable[:id].to_s)
        end

        it "filters by price range (:$and with :$gte and :$lte)" do
          data = fetch_filtered("{:$and [{:price {:$gte 50}} {:price {:$lte 100}}]}")
          ids = data.map { |i| i["id"] }
          expect(ids).to include(entry[:id].to_s)
          expect(ids).to include(entry_low_price[:id].to_s)
          expect(ids).not_to include(entry_borrowable[:id].to_s)
        end

        # Note: $gt/$lt range test removed - operators not required by UI (see TODO.md)
      end

      describe "filtering by identifiers" do
        it "filters by inventory_code (:$eq)" do
          data = fetch_filtered('{:inventory_code "ITZ21122"}')
          expect(data.map { |i| i["id"] }).to include(entry[:id].to_s)
        end

        it "filters by inventory_code (:$ilike partial)" do
          data = fetch_filtered('{:inventory_code {:$ilike "ITZ211"}}')
          expect(data.map { |i| i["id"] }).to include(entry[:id].to_s)
        end

        it "filters by supplier_id (:$eq)" do
          data = fetch_filtered('{:supplier_id {:$eq "d20338d8-1182-5ea6-91da-ea96c3a0a76a"}}')
          expect(data.map { |i| i["id"] }).to include(entry[:id].to_s)
        end

        it "supplier_id: returned items have that supplier_id and wrong supplier returns none" do
          data = fetch_filtered('{:supplier_id {:$eq "d20338d8-1182-5ea6-91da-ea96c3a0a76a"}}')
          expect(data).not_to be_empty
          data.each { |i| expect(i["supplier_id"]).to eq("d20338d8-1182-5ea6-91da-ea96c3a0a76a") }
          other = fetch_filtered('{:supplier_id {:$eq "00000000-0000-0000-0000-000000000000"}}')
          expect(other).to be_empty,
            "filter_q must restrict results. Got #{other.size} item(s). " \
            "On CI: ensure backend applies filter (create-filter-query-and-validate! returns modified query)."
        end

        it "filters by model_id (:$eq)" do
          data = fetch_filtered("{:model_id {:$eq \"#{entry[:model_id]}\"}}")
          expect(data.map { |i| i["id"] }).to include(entry[:id].to_s)
        end

        it "filters by user_name (:$ilike)" do
          data = fetch_filtered('{:user_name {:$ilike "itz"}}')
          expect(data.map { |i| i["id"] }).to include(entry[:id].to_s)
        end
      end

      describe "invalid filters (should return 400)" do
        it "rejects invalid properties_ field" do
          resp = client.get(filter_url('{:properties_abc {:$eq "12"}}'))
          expect(resp.status).to eq(400)
        end

        it "rejects invalid key" do
          resp = client.get(filter_url('{:invalid_key {:$eq "value"}}'))
          expect(resp.status).to eq(400)
        end

        it "rejects invalid EDN" do
          resp = client.get("#{base_url}?page=1&filter_q=not-edn-at-all")
          expect(resp.status).to eq(400)
        end
      end

      # Comprehensive tests for all supported fields and operators
      # Fields: UUID, numeric, boolean, date, string
      # Operators: $eq, $gte, $lte, $ilike, $ilike, $and, $or
      describe "comprehensive field and operator coverage" do
        # === UUID FIELDS ($eq only) ===
        describe "UUID fields" do
          it "id (:$eq)" do
            data = fetch_filtered("{:id {:$eq \"#{entry[:id]}\"}}")
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
          end

          it "inventory_pool_id (:$eq)" do
            data = fetch_filtered("{:inventory_pool_id {:$eq \"#{inventory_pool_id}\"}}")
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).to include(entry_borrowable[:id].to_s)
          end

          it "owner_id (:$eq)" do
            data = fetch_filtered("{:owner_id {:$eq \"#{@inventory_pool.id}\"}}")
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).to include(entry_borrowable[:id].to_s)
          end

          it "supplier_id (:$eq)" do
            data = fetch_filtered("{:supplier_id {:$eq \"#{@supplier.id}\"}}")
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).to include(entry_borrowable[:id].to_s)
          end

          it "model_id (:$eq)" do
            data = fetch_filtered("{:model_id {:$eq \"#{entry[:model_id]}\"}}")
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
          end

          it "room_id (:$eq)" do
            data = fetch_filtered("{:room_id {:$eq \"#{@room.id}\"}}")
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).to include(entry_borrowable[:id].to_s)
          end

          it "building_id (:$eq) — resolved via rooms.building_id" do
            data = fetch_filtered("{:building_id {:$eq \"#{@building.id}\"}}")
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).to include(entry_borrowable[:id].to_s)
          end
        end

        # === NUMERIC FIELDS ($eq, $gte, $lte) ===
        describe "numeric fields" do
          it "price (:$eq)" do
            data = fetch_filtered("{:price {:$eq 100}}")
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
            expect(ids).not_to include(entry_low_price[:id].to_s)
          end

          it "price (:$gte)" do
            data = fetch_filtered("{:price {:$gte 100}}")
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)         # price=100
            expect(ids).to include(entry_borrowable[:id].to_s) # price=250
            expect(ids).not_to include(entry_low_price[:id].to_s) # price=50
          end

          it "price (:$lte)" do
            data = fetch_filtered("{:price {:$lte 100}}")
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)         # price=100
            expect(ids).to include(entry_low_price[:id].to_s)  # price=50
            expect(ids).not_to include(entry_borrowable[:id].to_s) # price=250
          end

          it "price range (:$gte and :$lte combined)" do
            data = fetch_filtered("{:$and [{:price {:$gte 75}} {:price {:$lte 150}}]}")
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)         # price=100, in range
            expect(ids).not_to include(entry_borrowable[:id].to_s) # price=250, out of range
            expect(ids).not_to include(entry_low_price[:id].to_s)  # price=50, out of range
          end
        end

        # === BOOLEAN FIELDS ($eq true/false) ===
        describe "boolean fields" do
          it "is_borrowable (:$eq true)" do
            data = fetch_filtered("{:is_borrowable true}")
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry_borrowable[:id].to_s)
            expect(ids).not_to include(entry[:id].to_s)
            expect(ids).not_to include(entry_low_price[:id].to_s)
          end

          it "is_borrowable (:$eq false)" do
            data = fetch_filtered("{:is_borrowable false}")
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).to include(entry_low_price[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
          end

          it "is_broken (:$eq true)" do
            data = fetch_filtered("{:is_broken true}")
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry_borrowable[:id].to_s)
            expect(ids).not_to include(entry[:id].to_s)
            expect(ids).not_to include(entry_low_price[:id].to_s)
          end

          it "is_broken (:$eq false)" do
            data = fetch_filtered("{:is_broken false}")
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).to include(entry_low_price[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
          end

          it "is_incomplete (:$eq true)" do
            data = fetch_filtered("{:is_incomplete true}")
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry_borrowable[:id].to_s)
            expect(ids).not_to include(entry[:id].to_s)
            expect(ids).not_to include(entry_low_price[:id].to_s)
          end

          it "is_incomplete (:$eq false)" do
            data = fetch_filtered("{:is_incomplete false}")
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).to include(entry_low_price[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
          end

          it "is_inventory_relevant (:$eq true)" do
            data = fetch_filtered("{:is_inventory_relevant true}")
            ids = data.map { |i| i["id"] }
            # All test items have is_inventory_relevant=false by default
            expect(ids).not_to include(entry[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
            expect(ids).not_to include(entry_low_price[:id].to_s)
          end

          it "is_inventory_relevant (:$eq false)" do
            data = fetch_filtered("{:is_inventory_relevant false}")
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).to include(entry_borrowable[:id].to_s)
            expect(ids).to include(entry_low_price[:id].to_s)
          end

          it "needs_permission (:$eq true)" do
            data = fetch_filtered("{:needs_permission true}")
            ids = data.map { |i| i["id"] }
            # All test items have needs_permission=false by default
            expect(ids).not_to include(entry[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
          end

          it "needs_permission (:$eq false)" do
            data = fetch_filtered("{:needs_permission false}")
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).to include(entry_borrowable[:id].to_s)
          end

          # retired field logic:
          #   Client sends true  → returns items where db.retired date IS SET (IS NOT NULL)
          #   Client sends false → returns items where db.retired IS NULL (not retired)
          it "retired (:$eq true) returns items with retirement date set" do
            data = fetch_filtered("{:retired true}")
            ids = data.map { |i| i["id"] }
            # All test items have retired=nil, so none should match retired=true
            expect(ids).not_to include(entry[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
            expect(ids).not_to include(entry_low_price[:id].to_s)
          end

          it "retired (:$eq false) returns items without retirement date" do
            data = fetch_filtered("{:retired false}")
            ids = data.map { |i| i["id"] }
            # All test items have retired=nil, so all should match retired=false
            expect(ids).to include(entry[:id].to_s)
            expect(ids).to include(entry_borrowable[:id].to_s)
            expect(ids).to include(entry_low_price[:id].to_s)
          end

          # Map boolean form: {:retired {:$eq true/false}}
          it "retired ({:$eq true}) map form returns items with retirement date set" do
            data = fetch_filtered("{:retired {:$eq true}}")
            ids = data.map { |i| i["id"] }
            # All test items have retired=nil, so none should match
            expect(ids).not_to include(entry[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
            expect(ids).not_to include(entry_low_price[:id].to_s)
          end

          it "retired ({:$eq false}) map form returns items without retirement date" do
            data = fetch_filtered("{:retired {:$eq false}}")
            ids = data.map { |i| i["id"] }
            # All test items have retired=nil, so all should match
            expect(ids).to include(entry[:id].to_s)
            expect(ids).to include(entry_borrowable[:id].to_s)
            expect(ids).to include(entry_low_price[:id].to_s)
          end

          # Date operators on retired are rejected — only boolean predicates allowed
          it "retired (:$gte date) returns 400 — date operators not supported" do
            resp = client.get(filter_url('{:retired {:$gte "2024-01-01"}}'))
            expect(resp.status).to eq(400)
          end

          it "retired (:$lte date) returns 400 — date operators not supported" do
            resp = client.get(filter_url('{:retired {:$lte "2025-12-31"}}'))
            expect(resp.status).to eq(400)
          end

          it "retired (:$eq date) returns 400 — date operators not supported" do
            resp = client.get(filter_url('{:retired {:$eq "2024-06-15"}}'))
            expect(resp.status).to eq(400)
          end

          it "retired (:$ilike string) returns 400 — not a valid operator for retired" do
            resp = client.get(filter_url('{:retired {:$ilike "2024"}}'))
            expect(resp.status).to eq(400)
          end
        end

        # === DATE FIELDS ($eq, $gte, $lte) ===
        describe "date fields" do
          # invoice_date: entry=2013-09-20, entry_borrowable=2013-09-19, entry_low_price=2013-09-18
          it "invoice_date (:$eq)" do
            data = fetch_filtered('{:invoice_date {:$eq "2013-09-20"}}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
            expect(ids).not_to include(entry_low_price[:id].to_s)
          end

          it "invoice_date (:$gte)" do
            data = fetch_filtered('{:invoice_date {:$gte "2013-09-19"}}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)         # 2013-09-20
            expect(ids).to include(entry_borrowable[:id].to_s) # 2013-09-19
            expect(ids).not_to include(entry_low_price[:id].to_s) # 2013-09-18
          end

          it "invoice_date (:$lte)" do
            # Filter for items with invoice_date <= 2013-09-20 (includes all test items)
            data = fetch_filtered('{:invoice_date {:$lte "2013-09-20"}}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)         # 2013-09-20
            expect(ids).to include(entry_borrowable[:id].to_s) # 2013-09-19
            expect(ids).to include(entry_low_price[:id].to_s)  # 2013-09-18
          end

          it "invoice_date (:$lte) excludes items after date" do
            # Filter for items with invoice_date <= 2012-12-31 (excludes all test items from 2013)
            data = fetch_filtered('{:invoice_date {:$lte "2012-12-31"}}')
            ids = data.map { |i| i["id"] }
            expect(ids).not_to include(entry[:id].to_s)         # 2013-09-20
            expect(ids).not_to include(entry_borrowable[:id].to_s) # 2013-09-19
            expect(ids).not_to include(entry_low_price[:id].to_s)  # 2013-09-18
          end

          it "invoice_date range (:$gte and :$lte combined)" do
            # Filter for items with invoice_date between 2013-09-18 and 2013-09-20 (all test items)
            data = fetch_filtered('{:$and [{:invoice_date {:$gte "2013-09-18"}} {:invoice_date {:$lte "2013-09-20"}}]}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)         # 2013-09-20, in range
            expect(ids).to include(entry_borrowable[:id].to_s) # 2013-09-19, in range
            expect(ids).to include(entry_low_price[:id].to_s)  # 2013-09-18, in range
          end

          it "invoice_date range excludes out-of-range items" do
            # Filter for items with invoice_date between 2014-01-01 and 2014-12-31 (excludes all test items)
            data = fetch_filtered('{:$and [{:invoice_date {:$gte "2014-01-01"}} {:invoice_date {:$lte "2014-12-31"}}]}')
            ids = data.map { |i| i["id"] }
            expect(ids).not_to include(entry[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
            expect(ids).not_to include(entry_low_price[:id].to_s)
          end

          # last_check: all items have 2025-11-06
          it "last_check (:$eq)" do
            data = fetch_filtered('{:last_check {:$eq "2025-11-06"}}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).to include(entry_borrowable[:id].to_s)
          end

          it "last_check (:$gte)" do
            data = fetch_filtered('{:last_check {:$gte "2025-01-01"}}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).to include(entry_borrowable[:id].to_s)
          end

          it "last_check (:$lte)" do
            data = fetch_filtered('{:last_check {:$lte "2025-12-31"}}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).to include(entry_borrowable[:id].to_s)
          end

          it "last_check range (:$gte and :$lte combined)" do
            data = fetch_filtered('{:$and [{:last_check {:$gte "2025-01-01"}} {:last_check {:$lte "2025-12-31"}}]}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).to include(entry_borrowable[:id].to_s)
            expect(ids).to include(entry_low_price[:id].to_s)
          end
        end

        # === STRING FIELDS ($eq, $ilike, $ilike) ===
        describe "string fields" do
          it "inventory_code (:$eq)" do
            data = fetch_filtered('{:inventory_code {:$eq "ITZ21122"}}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
          end

          it "inventory_code (:$ilike)" do
            data = fetch_filtered('{:inventory_code {:$ilike "ITZ211"}}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).to include(entry_borrowable[:id].to_s)
            expect(ids).to include(entry_low_price[:id].to_s)
          end

          it "inventory_code (:$ilike)" do
            data = fetch_filtered('{:inventory_code {:$ilike "itz211"}}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).to include(entry_borrowable[:id].to_s)
          end

          it "serial_number (:$eq)" do
            data = fetch_filtered('{:serial_number {:$eq "SN-12345-ABC"}}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
          end

          it "serial_number (:$ilike)" do
            data = fetch_filtered('{:serial_number {:$ilike "SN-12345"}}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
          end

          it "serial_number (:$ilike case-insensitive)" do
            data = fetch_filtered('{:serial_number {:$ilike "sn-12345"}}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
          end

          it "shelf (:$eq)" do
            data = fetch_filtered('{:shelf {:$eq "Shelf-A1"}}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
          end

          it "shelf (:$ilike)" do
            data = fetch_filtered('{:shelf {:$ilike "Shelf"}}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
          end

          it "user_name (:$eq)" do
            data = fetch_filtered('{:user_name {:$eq "itz"}}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
          end

          it "user_name (:$ilike)" do
            data = fetch_filtered('{:user_name {:$ilike "itz"}}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
          end

          it "retired_reason (:$eq)" do
            data = fetch_filtered('{:retired_reason {:$eq "end of life"}}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry_borrowable[:id].to_s)
            expect(ids).not_to include(entry[:id].to_s)
          end

          it "retired_reason (:$ilike)" do
            data = fetch_filtered('{:retired_reason {:$ilike "end"}}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry_borrowable[:id].to_s)
            expect(ids).not_to include(entry[:id].to_s)
          end

          it "status_note (:$eq)" do
            data = fetch_filtered('{:status_note {:$eq "Available for loan"}}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
          end

          it "status_note (:$ilike)" do
            data = fetch_filtered('{:status_note {:$ilike "Available"}}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
          end

          it "note (:$eq)" do
            data = fetch_filtered('{:note {:$eq "Test item note"}}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
          end

          it "note (:$ilike)" do
            data = fetch_filtered('{:note {:$ilike "Test item"}}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
          end

          it "invoice_number (:$eq)" do
            data = fetch_filtered('{:invoice_number {:$eq "INV-2013-001"}}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
          end

          it "invoice_number (:$ilike)" do
            data = fetch_filtered('{:invoice_number {:$ilike "INV-2013"}}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
          end

          it "name (:$eq)" do
            data = fetch_filtered('{:name {:$eq "Test Item Name"}}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
          end

          it "name (:$ilike)" do
            data = fetch_filtered('{:name {:$ilike "Test Item"}}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
          end

          it "name (:$ilike case-insensitive)" do
            data = fetch_filtered('{:name {:$ilike "test item"}}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
          end

          it "insurance_number (:$eq)" do
            data = fetch_filtered('{:insurance_number {:$eq "INS-98765"}}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
          end

          it "insurance_number (:$ilike)" do
            data = fetch_filtered('{:insurance_number {:$ilike "INS-98"}}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
          end

          it "item_version (:$eq)" do
            data = fetch_filtered('{:item_version {:$eq "v1.0"}}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
          end

          it "item_version (:$ilike)" do
            data = fetch_filtered('{:item_version {:$ilike "v1"}}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
          end

          it "responsible (:$eq)" do
            data = fetch_filtered('{:responsible {:$eq "IT Department"}}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
          end

          it "responsible (:$ilike)" do
            data = fetch_filtered('{:responsible {:$ilike "IT"}}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
          end
        end

        # === PROPERTIES FIELDS ($eq, $ilike) ===
        describe "properties fields" do
          it "properties_reference (:$eq)" do
            data = fetch_filtered('{:properties_reference {:$eq "invoice"}}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
          end

          it "properties_reference (:$ilike)" do
            data = fetch_filtered('{:properties_reference {:$ilike "inv"}}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
          end

          it "properties_contract_expiration (:$eq)" do
            data = fetch_filtered('{:properties_contract_expiration {:$eq "2040-11-06"}}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
          end

          it "properties_mac_address (:$ilike)" do
            data = fetch_filtered('{:properties_mac_address {:$ilike "00:1B"}}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
          end
        end

        # === LOGICAL OPERATORS ($and, $or) ===
        describe "logical operators" do
          it ":$and with 2 conditions" do
            data = fetch_filtered("{:$and [{:inventory_code \"ITZ21122\"} {:model_id {:$eq \"#{entry[:model_id]}\"}}]}")
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
          end

          it ":$and with 3 conditions (boolean + numeric + string)" do
            data = fetch_filtered('{:$and [{:is_borrowable false} {:price {:$gte 100}} {:inventory_code {:$ilike "ITZ21122"}}]}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
            expect(ids).not_to include(entry_low_price[:id].to_s)
          end

          it ":$and where no items match all conditions" do
            data = fetch_filtered("{:$and [{:is_borrowable true} {:is_broken false}]}")
            ids = data.map { |i| i["id"] }
            # entry_borrowable: is_borrowable=true BUT is_broken=true
            expect(ids).not_to include(entry[:id].to_s)
            expect(ids).not_to include(entry_borrowable[:id].to_s)
            expect(ids).not_to include(entry_low_price[:id].to_s)
          end

          it ":$or with 2 conditions" do
            data = fetch_filtered('{:$or [{:inventory_code {:$eq "ITZ21122"}} {:inventory_code {:$eq "ITZ21123"}}]}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).to include(entry_borrowable[:id].to_s)
            expect(ids).not_to include(entry_low_price[:id].to_s)
          end

          it ":$or with 3 conditions matching all items" do
            data = fetch_filtered("{:$or [{:price {:$eq 100}} {:price {:$eq 250}} {:price {:$eq 50}}]}")
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).to include(entry_borrowable[:id].to_s)
            expect(ids).to include(entry_low_price[:id].to_s)
          end

          it ":$or where only first condition matches" do
            data = fetch_filtered("{:$or [{:is_borrowable true} {:price {:$eq 999}}]}")
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry_borrowable[:id].to_s)
            expect(ids).not_to include(entry[:id].to_s)
            expect(ids).not_to include(entry_low_price[:id].to_s)
          end

          it ":$or with nested :$and (2 subconditions each)" do
            # (is_borrowable=false AND price>=100) OR (is_broken=true AND price>=200)
            data = fetch_filtered("{:$or [{:$and [{:is_borrowable false} {:price {:$gte 100}}]} {:$and [{:is_broken true} {:price {:$gte 200}}]}]}")
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)         # is_borrowable=false, price=100
            expect(ids).to include(entry_borrowable[:id].to_s) # is_broken=true, price=250
            expect(ids).not_to include(entry_low_price[:id].to_s) # is_borrowable=false, price=50 (price too low)
          end

          it ":$or with nested :$and (3 subconditions in one)" do
            # (is_borrowable=false AND is_broken=false AND price=100) OR (is_borrowable=true)
            data = fetch_filtered("{:$or [{:$and [{:is_borrowable false} {:is_broken false} {:price {:$eq 100}}]} {:is_borrowable true}]}")
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)         # matches first $and
            expect(ids).to include(entry_borrowable[:id].to_s) # matches second condition
            expect(ids).not_to include(entry_low_price[:id].to_s) # price=50, doesn't match
          end

          it ":$or with nested :$and where only one branch matches" do
            # Only the second $and should match entry_borrowable
            filter = "{:$or [" \
              "{:$and [{:is_borrowable false} {:price {:$gte 500}}]} " \
              "{:$and [{:is_borrowable true} {:is_broken true}]} " \
              "{:$and [{:user_name {:$ilike \"xyz\"}} {:is_incomplete true}]}" \
            "]}"
            data = fetch_filtered(filter)
            ids = data.map { |i| i["id"] }
            expect(ids).not_to include(entry[:id].to_s)        # price < 500
            expect(ids).to include(entry_borrowable[:id].to_s)  # matches second $and
            expect(ids).not_to include(entry_low_price[:id].to_s)
          end

          it ":$or combining boolean, numeric, string, and date conditions" do
            # Complex real-world scenario:
            # OR condition 1: broken items with high price (2 conditions)
            # OR condition 2: specific user with borrowable status and date range (3 conditions)
            # OR condition 3: specific inventory code (1 condition)
            filter = "{:$or [" \
              "{:$and [{:is_broken true} {:price {:$gte 200}}]} " \
              "{:$and [{:user_name {:$ilike \"itz\"}} {:is_borrowable false} {:invoice_date {:$gte \"2013-09-01\"}}]} " \
              "{:inventory_code {:$eq \"ITZ21124\"}}" \
            "]}"
            data = fetch_filtered(filter)
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)         # matches second $and
            expect(ids).to include(entry_borrowable[:id].to_s) # matches first $and
            expect(ids).to include(entry_low_price[:id].to_s)  # matches third condition
          end

          it ":$or with mixed field types (boolean, numeric, string)" do
            # (is_borrowable=true) OR (price <= 50) OR (inventory_code matches ilike)
            data = fetch_filtered('{:$or [{:is_borrowable true} {:price {:$lte 50}} {:inventory_code {:$ilike "ITZ21122"}}]}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)         # matches inventory_code ilike
            expect(ids).to include(entry_borrowable[:id].to_s) # is_borrowable=true
            expect(ids).to include(entry_low_price[:id].to_s)  # price=50
          end

          it ":$and with date range" do
            data = fetch_filtered('{:$and [{:invoice_date {:$gte "2013-09-19"}} {:invoice_date {:$lte "2013-09-20"}}]}')
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)
            expect(ids).to include(entry_borrowable[:id].to_s)
            expect(ids).not_to include(entry_low_price[:id].to_s)
          end

          it ":$and with price range" do
            data = fetch_filtered("{:$and [{:price {:$gte 100}} {:price {:$lte 200}}]}")
            ids = data.map { |i| i["id"] }
            expect(ids).to include(entry[:id].to_s)         # price=100
            expect(ids).not_to include(entry_borrowable[:id].to_s) # price=250
            expect(ids).not_to include(entry_low_price[:id].to_s)  # price=50
          end
        end
      end
    end

    # ==========================================================================
    # SEARCH PARAMETER TESTS
    # The 'search_term' parameter provides simple text search across multiple fields
    # (different from filter_q which uses EDN-based precise filtering)
    # ==========================================================================
    context "GET /inventory/:pool-id/items?page=1&search_term= (text search)" do
      let!(:search_entry) {
        model = FactoryBot.create(:leihs_model, id: SecureRandom.uuid, product: "SearchableProduct")
        FactoryBot.create(:item,
          inventory_code: "SEARCH-001",
          model_id: model.id,
          room_id: @room.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          serial_number: "UNIQUE-SERIAL-XYZ",
          name: "Searchable Item Name")
      }

      def search_url(term)
        "#{base_url}?page=1&search_term=#{CGI.escape(term)}"
      end

      def fetch_searched(term)
        resp = client.get(search_url(term))
        expect(resp.status).to eq(200), "expected 200 but got #{resp.status}: #{resp.body}"
        expect(resp.body).to be_a(Hash)
        expect(resp.body).to have_key("data")
        resp.body["data"]
      end

      describe "search by inventory_code" do
        it "finds items matching inventory_code" do
          data = fetch_searched("SEARCH-001")
          ids = data.map { |i| i["id"] }
          expect(ids).to include(search_entry[:id].to_s)
        end

        it "finds items with partial inventory_code match" do
          data = fetch_searched("SEARCH")
          ids = data.map { |i| i["id"] }
          expect(ids).to include(search_entry[:id].to_s)
        end
      end

      describe "search by serial_number" do
        it "finds items matching serial_number" do
          data = fetch_searched("UNIQUE-SERIAL")
          ids = data.map { |i| i["id"] }
          expect(ids).to include(search_entry[:id].to_s)
        end
      end

      describe "search by model product name" do
        it "finds items matching model product" do
          data = fetch_searched("SearchableProduct")
          ids = data.map { |i| i["id"] }
          expect(ids).to include(search_entry[:id].to_s)
        end
      end

      describe "search returns empty for non-matching term" do
        it "returns empty array for non-existent term" do
          data = fetch_searched("NONEXISTENT-TERM-12345")
          expect(data).to be_an(Array)
          # Should not include search_entry
          ids = data.map { |i| i["id"] }
          expect(ids).not_to include(search_entry[:id].to_s)
        end
      end

      describe "search_term combined with filter_q" do
        it "applies both search_term and filter" do
          # Search for inventory_code and filter by model_id
          url = "#{base_url}?page=1&search_term=SEARCH&filter_q=#{CGI.escape("{:model_id {:$eq \"#{search_entry[:model_id]}\"}}")}"
          resp = client.get(url)
          expect(resp.status).to eq(200)
          ids = resp.body["data"].map { |i| i["id"] }
          expect(ids).to include(search_entry[:id].to_s)
        end
      end
    end
  end
end
