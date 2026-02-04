require "spec_helper"
require "pry"
require "csv"
require_relative "_shared"

describe "Items Export" do
  include_context :setup_models_min_api

  before :each do
    @user, @user_cookies, @user_cookies_str, @cookie_token = create_and_login(:user)
    FactoryBot.create(:access_right,
      inventory_pool_id: @inventory_pool.id,
      user_id: @user.id,
      role: "inventory_manager")

    @model = FactoryBot.create(:leihs_model,
      product: "Export Test Product",
      is_package: false)

    @other_model = FactoryBot.create(:leihs_model,
      product: "Other Product",
      is_package: false)

    @building = FactoryBot.create(:building, name: "Test Building")
    @room = FactoryBot.create(:room,
      name: "Test Room",
      building_id: @building.id)

    # Batch-create 3 items via POST (mirrors the batch create flow that leads to review page)
    post_client = session_auth_plain_faraday_json_client(cookies: @user_cookies)
    resp = post_client.post "/inventory/#{@inventory_pool.id}/items/" do |req|
      req.body = {
        count: 3,
        model_id: @model.id,
        room_id: @room.id,
        inventory_pool_id: @inventory_pool.id,
        owner_id: @inventory_pool.id
      }.to_json
      req.headers["Content-Type"] = "application/json"
      req.headers["Accept"] = "application/json"
      req.headers["x-csrf-token"] = X_CSRF_TOKEN
    end
    expect(resp.status).to eq(200)

    @batch_items = resp.body
    @batch_ids = @batch_items.map { |i| i["id"] }
    @batch_codes = @batch_items.map { |i| i["inventory_code"] }

    # Extra item on different model — must be excluded when filtering by ids
    FactoryBot.create(:item,
      inventory_code: "EXTRA-#{SecureRandom.hex(4)}",
      leihs_model: @other_model,
      inventory_pool_id: @inventory_pool.id,
      owner_id: @inventory_pool.id,
      responsible: @inventory_pool,
      room_id: @room.id)
  end

  let(:inventory_pool_id) { @inventory_pool.id }
  let(:base_url) { "/inventory/#{inventory_pool_id}/items/" }
  let(:ids_params) { {ids: @batch_ids} }

  def export_client(accept)
    headers = {"accept" => accept}
    headers[:Cookie] = @user_cookies.map(&:to_s).join("; ")
    Faraday.new(url: api_base_url, headers: headers) do |conn|
      conn.adapter Faraday.default_adapter
    end
  end

  context "CSV export filtered by ids" do
    it "returns only the batch-created items with correct headers" do
      resp = export_client(ACCEPT_CSV).get base_url, ids_params

      expect(resp.status).to eq(200)
      expect(resp.headers["content-type"]).to include("text/csv")
      expect(resp.headers["content-disposition"]).to include("items.csv")

      csv_data = CSV.parse(resp.body)
      header = csv_data[0]
      rows = csv_data[1..]

      expect(header).to include("product")
      expect(header).to include("inventory_code")

      # Exactly 3 rows — the extra item is excluded by the ids filter
      expect(rows.length).to eq(3)

      # All batch inventory codes present
      code_idx = header.index("inventory_code")
      exported_codes = rows.map { |row| row[code_idx] }
      expect(exported_codes.sort).to eq(@batch_codes.sort)
    end

    it "includes correct model product name for all rows" do
      resp = export_client(ACCEPT_CSV).get base_url, ids_params
      csv_data = CSV.parse(resp.body)
      header = csv_data[0]
      product_idx = header.index("product")

      csv_data[1..].each do |row|
        expect(row[product_idx]).to eq("Export Test Product")
      end
    end
  end

  context "Excel export filtered by ids" do
    it "returns Excel file with correct content-type and disposition" do
      resp = export_client(ACCEPT_XLSX).get base_url, ids_params

      expect(resp.status).to eq(200)
      expect(resp.headers["content-type"]).to include("spreadsheet")
      expect(resp.headers["content-disposition"]).to include("items.xlsx")
      expect(resp.body).not_to be_empty
    end
  end

  context "CSV export without ids filter" do
    it "returns all items in the pool" do
      resp = export_client(ACCEPT_CSV).get base_url
      csv_data = CSV.parse(resp.body)
      rows = csv_data[1..]

      # 3 batch items + 1 extra = 4
      expect(rows.length).to eq(4)
    end
  end
end
