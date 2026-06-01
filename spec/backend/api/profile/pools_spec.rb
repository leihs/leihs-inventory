require "spec_helper"
require_relative "../_shared"

RSpec.describe "Inventory profile API (available_inventory_pools)" do
  let(:client) { plain_faraday_json_client(@cookie_header) }

  def pool_ids(body)
    (body["available_inventory_pools"] || []).map { |p| p["id"] }
  end

  def pool_by_id(body, id)
    (body["available_inventory_pools"] || []).find { |p| p["id"] == id.to_s }
  end

  context "as group_manager" do
    include_context :setup_api, "group_manager"
    include_context :generate_session_header

    it "returns the pool with read permission" do
      resp = client.get "/inventory/profile/"
      expect(resp.status).to eq(200)

      pool = pool_by_id(resp.body, @inventory_pool.id)
      expect(pool).to be
      expect(pool["name"]).to eq(@inventory_pool.name)
      expect(pool["permission"]).to eq("read")
    end
  end

  context "as lending_manager" do
    include_context :setup_api, "lending_manager"
    include_context :generate_session_header

    it "returns the pool with edit permission" do
      resp = client.get "/inventory/profile/"
      expect(resp.status).to eq(200)

      pool = pool_by_id(resp.body, @inventory_pool.id)
      expect(pool).to be
      expect(pool["permission"]).to eq("edit")
    end
  end

  context "as inventory_manager" do
    include_context :setup_api, "inventory_manager"
    include_context :generate_session_header

    it "returns the pool with edit permission" do
      resp = client.get "/inventory/profile/"
      expect(resp.status).to eq(200)

      pool = pool_by_id(resp.body, @inventory_pool.id)
      expect(pool).to be
      expect(pool["permission"]).to eq("edit")
    end
  end

  context "as customer only" do
    include_context :setup_api, "customer"
    include_context :generate_session_header

    it "does not list the pool" do
      resp = client.get "/inventory/profile/"
      expect(resp.status).to eq(200)
      expect(pool_ids(resp.body)).not_to include(@inventory_pool.id.to_s)
    end
  end

  context "inactive inventory pool" do
    include_context :setup_api, "group_manager"
    include_context :generate_session_header

    let!(:inactive_pool) do
      pool = FactoryBot.create(:inventory_pool)
      deactivate_inventory_pool!(pool)
      FactoryBot.create(:direct_access_right,
        inventory_pool_id: pool.id,
        user_id: @user.id,
        role: "group_manager")
      pool
    end

    it "excludes inactive pools" do
      resp = client.get "/inventory/profile/"
      expect(resp.status).to eq(200)
      expect(pool_ids(resp.body)).to include(@inventory_pool.id.to_s)
      expect(pool_ids(resp.body)).not_to include(inactive_pool.id.to_s)
    end
  end

  context "when user has a direct role on the same pool" do
    include_context :setup_api, "group_manager"
    include_context :generate_session_header

    it "returns the pool once" do
      resp = client.get "/inventory/profile/"
      expect(resp.status).to eq(200)

      pools = resp.body["available_inventory_pools"] || []
      matching_pool = pools.select { |p| p["id"] == @inventory_pool.id.to_s }

      expect(matching_pool.size).to eq(1)
      expect(matching_pool.first["permission"]).to eq("read")
    end
  end
end
