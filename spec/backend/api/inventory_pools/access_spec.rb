require "spec_helper"
require_relative "../_shared"

RSpec.describe "Inventory inventory-pools API (inactive responsible filter)" do
  let(:client) { plain_faraday_json_client(@cookie_header) }
  let(:pool_id) { @inventory_pool.id }
  let(:inventory_pools_path) { "/inventory/#{pool_id}/inventory-pools/?responsible=true" }

  include_context :setup_api, "group_manager"
  include_context :generate_session_header

  let(:model) { FactoryBot.create(:leihs_model, id: SecureRandom.uuid) }
  let!(:active_responsible_pool) { FactoryBot.create(:inventory_pool) }
  let!(:inactive_responsible_pool) do
    pool = FactoryBot.create(:inventory_pool)
    deactivate_inventory_pool!(pool)
    pool
  end

  before :each do
    FactoryBot.create(:item,
      leihs_model: model,
      owner_id: @inventory_pool.id,
      inventory_pool_id: active_responsible_pool.id,
      responsible: active_responsible_pool,
      is_borrowable: true)
    FactoryBot.create(:item,
      leihs_model: model,
      owner_id: @inventory_pool.id,
      inventory_pool_id: inactive_responsible_pool.id,
      responsible: inactive_responsible_pool,
      is_borrowable: true)
  end

  def pool_ids(body)
    body.map { |p| p["id"] }
  end

  it "excludes inactive pools from responsible listing" do
    resp = client.get inventory_pools_path
    expect(resp.status).to eq(200)
    expect(resp.body).to be_an(Array)
    expect(pool_ids(resp.body)).to include(active_responsible_pool.id.to_s)
    expect(pool_ids(resp.body)).not_to include(inactive_responsible_pool.id.to_s)
  end
end
