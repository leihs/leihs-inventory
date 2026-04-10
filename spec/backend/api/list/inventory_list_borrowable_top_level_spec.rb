require "spec_helper"
require "cgi"
require_relative "../_shared"

describe "GET /inventory/:pool_id/list borrowable_quantity top-level items" do
  include_context :setup_api, "inventory_manager"
  include_context :generate_session_header

  let(:client) { plain_faraday_json_client(@cookie_header) }
  let(:pool_id) { @inventory_pool.id }

  it "does not count package child rows in borrowable_quantity (matches in_stock top-level scope)" do
    unique_product = "PKG_CHILD_LIST_#{SecureRandom.hex(6)}"
    model = FactoryBot.create(:leihs_model,
      id: SecureRandom.uuid,
      product: unique_product,
      type: "Model",
      is_package: false)

    parent_item = FactoryBot.create(:item,
      leihs_model: model,
      inventory_pool_id: @inventory_pool.id,
      responsible: @inventory_pool,
      is_borrowable: true)

    FactoryBot.create(:item,
      leihs_model: model,
      inventory_pool_id: @inventory_pool.id,
      responsible: @inventory_pool,
      is_borrowable: true,
      parent_id: parent_item.id)

    q = CGI.escape(unique_product)
    resp = client.get(
      "/inventory/#{pool_id}/list/?with_items=true&type=model&search=#{q}&page=1&size=50"
    )

    expect(resp.status).to eq(200)
    rows = resp.body["data"]
    expect(rows).to be_a(Array)
    row = rows.find { |r| r["id"].to_s == model.id.to_s }
    expect(row).to be_present, "expected one list row for model #{model.id}"
    expect(row["items_quantity"]).to eq(2)
    expect(row["borrowable_quantity"]).to eq(1)
    expect(row["in_stock_quantity"]).to eq(1)
  end
end
