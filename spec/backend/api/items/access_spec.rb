require "spec_helper"
require_relative "../_shared"

RSpec.describe "Inventory items API (access control)" do
  let(:client) { plain_faraday_json_client(@cookie_header) }
  let(:pool_id) { @inventory_pool.id }
  let(:model_id) { @models.first.id }
  let(:items_path) { "/inventory/#{pool_id}/items/?model_id=#{model_id}&retired=false" }

  shared_examples "items access setup" do |role|
    include_context :setup_api, role
    include_context :generate_session_header

    before :each do
      @models = create_models
      create_and_add_items_to_models(@inventory_pool, [@models.first])
    end
  end

  context "as group_manager" do
    include_examples "items access setup", "group_manager"

    it "allows GET items filtered by model_id and retired" do
      resp = client.get items_path
      expect(resp.status).to eq(200)
      expect(resp.body).to be_an(Array)
    end

    it "still denies POST items" do
      resp = client.post "/inventory/#{pool_id}/items/" do |req|
        req.headers["Content-Type"] = "application/json"
        req.body = {inventory_code: "GM-001", model_id: model_id, owner_id: pool_id}.to_json
      end
      expect(resp.status).to eq(403)
    end

    it "still denies GET templates" do
      resp = client.get "/inventory/#{pool_id}/templates/"
      expect(resp.status).to eq(403)
    end

    context "when the pool is inactive" do
      before :each do
        deactivate_inventory_pool!(@inventory_pool)
      end

      it "still allows GET items when pool is inactive" do
        resp = client.get items_path
        expect(resp.status).to eq(200)
        expect(resp.body).to be_an(Array)
      end

      it "still denies POST items when pool is inactive" do
        resp = client.post "/inventory/#{pool_id}/items/" do |req|
          req.headers["Content-Type"] = "application/json"
          req.body = {inventory_code: "GM-001", model_id: model_id, owner_id: pool_id}.to_json
        end
        expect(resp.status).to eq(403)
      end
    end
  end

  context "as inventory_manager" do
    include_examples "items access setup", "inventory_manager"

    it "allows GET items filtered by model_id and retired" do
      resp = client.get items_path
      expect(resp.status).to eq(200)
      expect(resp.body).to be_an(Array)
    end

    context "when the pool is inactive" do
      before :each do
        deactivate_inventory_pool!(@inventory_pool)
      end

      it "still allows GET items when pool is inactive" do
        resp = client.get items_path
        expect(resp.status).to eq(200)
        expect(resp.body).to be_an(Array)
      end
    end
  end
end
