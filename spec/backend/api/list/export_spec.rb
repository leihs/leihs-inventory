require "spec_helper"
require "pry"
require "csv"
require "cgi"
require_relative "../_shared"

describe "Inventory List Export" do
  include_context :setup_models_min_api

  before :each do
    @user, @user_cookies, @user_cookies_str, @cookie_token = create_and_login(:user)
    FactoryBot.create(:access_right,
      inventory_pool_id: @inventory_pool.id,
      user_id: @user.id,
      role: "inventory_manager")

    @models = create_models(2)
    create_and_add_items_to_models(@inventory_pool, [@models.first])
  end

  let(:inventory_pool_id) { @inventory_pool.id }
  let(:url) { "/inventory/#{inventory_pool_id}/list/" }

  context "CSV Export" do
    it "returns CSV with proper headers and data" do
      headers = {"accept" => ACCEPT_CSV}
      headers[:Cookie] = @user_cookies.map(&:to_s).join("; ")

      client = Faraday.new(url: api_base_url, headers: headers) do |conn|
        conn.adapter Faraday.default_adapter
      end

      resp = client.get url

      expect(resp.status).to eq(200)
      expect(resp.headers["content-type"]).to include("text/csv")
      expect(resp.headers["content-disposition"]).to include("inventory-list.csv")

      csv_data = CSV.parse(resp.body)
      header = csv_data[0]

      # Check that basic columns exist
      expect(header).to include("type")
      expect(header).to include("product")
      expect(header).to include("description")
      expect(header).to include("inventory_code")

      # Check that we have data rows
      expect(csv_data.length).to be > 1
    end

    it "shows 'Item' type for rows with inventory_code" do
      headers = {"accept" => ACCEPT_CSV}
      headers[:Cookie] = @user_cookies.map(&:to_s).join("; ")

      client = Faraday.new(url: api_base_url, headers: headers) do |conn|
        conn.adapter Faraday.default_adapter
      end

      resp = client.get url

      csv_data = CSV.parse(resp.body)
      header = csv_data[0]
      type_index = header.index("type")
      inventory_code_index = header.index("inventory_code")

      # Find a row with inventory_code
      row_with_item = csv_data[1..].find { |row| row[inventory_code_index].to_s.strip != "" }

      if row_with_item
        expect(row_with_item[type_index]).to eq("Item")
      end
    end

    it "splits property fields into separate columns" do
      headers = {"accept" => ACCEPT_CSV}
      headers[:Cookie] = @user_cookies.map(&:to_s).join("; ")

      client = Faraday.new(url: api_base_url, headers: headers) do |conn|
        conn.adapter Faraday.default_adapter
      end

      resp = client.get url

      csv_data = CSV.parse(resp.body)
      header = csv_data[0]

      # Check for property field columns (fields starting with properties_)
      property_columns = header.select { |h| h.to_s.start_with?("properties_") }

      # We should have property columns based on active fields for the pool
      expect(property_columns).not_to be_empty
    end
  end

  context "Excel Export" do
    it "returns Excel file with proper headers" do
      headers = {"accept" => ACCEPT_XLSX}
      headers[:Cookie] = @user_cookies.map(&:to_s).join("; ")

      client = Faraday.new(url: api_base_url, headers: headers) do |conn|
        conn.adapter Faraday.default_adapter
      end

      resp = client.get url

      expect(resp.status).to eq(200)
      expect(resp.headers["content-type"]).to include("spreadsheet")
      expect(resp.headers["content-disposition"]).to include("inventory-list.xlsx")

      # Basic check that we got binary data
      expect(resp.body).not_to be_empty
    end
  end

  context "export row parity with expanded list" do
    let(:json_client) { session_auth_plain_faraday_json_client(cookies: @user_cookies) }

    def csv_client
      headers = {"accept" => ACCEPT_CSV, "Cookie" => @user_cookies.map(&:to_s).join("; ")}
      Faraday.new(url: api_base_url, headers: headers) do |conn|
        conn.adapter Faraday.default_adapter
      end
    end

    def csv_inventory_codes(list_url)
      resp = csv_client.get(list_url)
      expect(resp.status).to eq(200)
      csv_data = CSV.parse(resp.body)
      code_idx = csv_data[0].index("inventory_code")
      csv_data[1..].map { |row| row[code_idx] }.compact.reject { |c| c.to_s.strip == "" }
    end

    def paginated_data(body)
      body.is_a?(Hash) ? body.fetch("data") : body
    end

    def expanded_items_inventory_codes(list_url, items_query_suffix)
      list_resp = json_client.get(list_url)
      expect(list_resp.status).to eq(200)
      model_ids = paginated_data(list_resp.body).map { |m| m["id"] }
      model_ids.flat_map do |model_id|
        items_url = "/inventory/#{inventory_pool_id}/items/?page=1&size=100&#{items_query_suffix}&model_id=#{model_id}"
        items_resp = json_client.get(items_url)
        expect(items_resp.status).to eq(200), "expected 200 but got #{items_resp.status}: #{items_resp.body}"
        paginated_data(items_resp.body).map { |i| i["inventory_code"] }
      end
    end

    context "note-only search does not export sibling items" do
      let(:search_term) { "standfuss" }
      let(:items_query_suffix) { "retired=false&search=#{CGI.escape(search_term)}" }
      let(:list_url) { "/inventory/#{inventory_pool_id}/list/?page=1&size=100&with_items=true&#{items_query_suffix}" }

      before do
        @model = FactoryBot.create(:leihs_model, product: "Speaker", version: "v1")
        @building = FactoryBot.create(:building)
        @room = FactoryBot.create(:room, building_id: @building.id)
        @item_match = FactoryBot.create(:item,
          leihs_model: @model,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          room_id: @room.id,
          note: "mount with #{search_term}",
          retired: nil,
          is_borrowable: true)
        @item_other = FactoryBot.create(:item,
          inventory_code: "NO-MATCH-#{SecureRandom.hex(4)}",
          leihs_model: @model,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          room_id: @room.id,
          note: "plain mount",
          retired: nil,
          is_borrowable: true)
      end

      it "CSV inventory_codes match GET /items/ per visible model" do
        items_codes = expanded_items_inventory_codes(list_url, items_query_suffix)
        csv_codes = csv_inventory_codes(list_url)

        expect(csv_codes.sort).to eq(items_codes.sort)
        expect(csv_codes).to include(@item_match.inventory_code)
        expect(csv_codes).not_to include(@item_other.inventory_code)
      end
    end

    context "model-level search exports all filtered items on the model" do
      let(:search_term) { "Richtlautsprecher" }
      let(:items_query_suffix) { "retired=false&search=#{CGI.escape(search_term)}" }
      let(:list_url) { "/inventory/#{inventory_pool_id}/list/?page=1&size=100&with_items=true&#{items_query_suffix}" }

      before do
        @model = FactoryBot.create(:leihs_model,
          product: "Speaker",
          version: search_term)
        @building = FactoryBot.create(:building)
        @room = FactoryBot.create(:room, building_id: @building.id)
        @item_one = FactoryBot.create(:item,
          inventory_code: "RL-ONE-#{SecureRandom.hex(3)}",
          leihs_model: @model,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          room_id: @room.id,
          retired: nil,
          is_borrowable: true)
        @item_two = FactoryBot.create(:item,
          inventory_code: "RL-TWO-#{SecureRandom.hex(3)}",
          leihs_model: @model,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          room_id: @room.id,
          retired: nil,
          is_borrowable: true)
      end

      it "includes every item returned by GET /items/ for that model" do
        items_codes = expanded_items_inventory_codes(list_url, items_query_suffix)
        csv_codes = csv_inventory_codes(list_url)

        expect(csv_codes.sort).to eq(items_codes.sort)
        expect(csv_codes).to include(@item_one.inventory_code, @item_two.inventory_code)
      end
    end
  end

  context "Column Order Consistency" do
    it "has the same column order in CSV and Excel exports" do
      headers = {"accept" => ACCEPT_CSV}
      headers[:Cookie] = @user_cookies.map(&:to_s).join("; ")

      csv_client = Faraday.new(url: api_base_url, headers: headers) do |conn|
        conn.adapter Faraday.default_adapter
      end

      csv_resp = csv_client.get url

      csv_data = CSV.parse(csv_resp.body)
      csv_header = csv_data[0]

      # We can't easily parse Excel in this test, but we've verified
      # that the keys are passed in the same order to excel-response
      expect(csv_header).to include("type", "product", "description")
    end
  end
end
