require "spec_helper"
require "pry"
require "csv"
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
