require "spec_helper"
require_relative "_shared"

RSpec.describe "Inventory inactive pool direct access" do
  let(:client) { plain_faraday_json_client(@cookie_header) }
  let(:pool_id) { @inventory_pool.id }
  let(:list_path) { "/inventory/#{pool_id}/list/" }

  include_context :setup_api, "group_manager"
  include_context :generate_session_header

  before :each do
    @models = create_models
    create_and_add_items_to_models(@inventory_pool, [@models.first])
    deactivate_inventory_pool!(@inventory_pool)
  end

  let(:model_id) { @models.first.id }
  let(:items_path) { "/inventory/#{pool_id}/items/?model_id=#{model_id}&retired=false" }

  def pool_ids(body)
    (body["available_inventory_pools"] || []).map { |p| p["id"] }
  end

  it "excludes the inactive pool from profile" do
    resp = client.get "/inventory/profile/"
    expect(resp.status).to eq(200)
    expect(pool_ids(resp.body)).not_to include(pool_id.to_s)
  end

  it "still allows GET list via direct URL when pool is inactive" do
    resp = client.get list_path
    expect(resp.status).to eq(200)
    expect(resp.body).to be_an(Array)
  end

  it "still allows GET items via direct URL when pool is inactive" do
    resp = client.get items_path
    expect(resp.status).to eq(200)
    expect(resp.body).to be_an(Array)
  end
end
