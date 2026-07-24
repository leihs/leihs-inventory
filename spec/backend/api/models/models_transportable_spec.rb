require "spec_helper"
require_relative "../_shared"

describe "Inventory model transportable + pickup locations flag" do
  include_context :setup_models_min_api

  before :each do
    @user_cookies, @user_cookies_str, @cookie_token = create_and_login_by(@user)
  end

  let(:client) {
    headers = {"accept" => "application/json", "x-csrf-token" => X_CSRF_TOKEN}
    session_auth_plain_faraday_json_client(cookies: @user_cookies, headers: headers)
  }
  let(:inventory_pool_id) { @inventory_pool.id }
  let(:models_url) { "/inventory/#{inventory_pool_id}/models/" }
  let(:pickup_locations_url) { "/inventory/#{inventory_pool_id}/pickup-locations/" }

  describe "GET /inventory/:pool-id/pickup-locations/" do
    it "returns has_pickup_locations false when pool has none" do
      resp = client.get pickup_locations_url
      expect(resp.status).to eq(200)
      expect(resp.body["has_pickup_locations"]).to eq(false)
    end

    it "returns has_pickup_locations true when pool has alternatives" do
      FactoryBot.create(:pickup_location, inventory_pool: @inventory_pool)
      resp = client.get pickup_locations_url
      expect(resp.status).to eq(200)
      expect(resp.body["has_pickup_locations"]).to eq(true)
    end
  end

  describe "model transportable persistence" do
    it "defaults transportable to true when omitted on create" do
      resp = client.post(models_url) do |req|
        req.body = {
          product: "Default Transportable Model",
          version: "1",
          type: "Model",
          is_package: false
        }.to_json
        req.headers["Content-Type"] = "application/json"
        req.headers["Accept"] = "application/json"
        req.headers["x-csrf-token"] = X_CSRF_TOKEN
        req.headers["Cookie"] = @user_cookies.map(&:to_s).join("; ")
      end

      expect(resp.status).to eq(200)
      model_id = resp.body["id"]
      get_resp = client.get("#{models_url}#{model_id}")
      expect(get_resp.status).to eq(200)
      expect(get_resp.body["transportable"]).to eq(true)
    end

    it "persists transportable false on create and update" do
      create_resp = client.post(models_url) do |req|
        req.body = {
          product: "Not Transportable Model",
          version: "1",
          type: "Model",
          is_package: false,
          transportable: false
        }.to_json
        req.headers["Content-Type"] = "application/json"
        req.headers["Accept"] = "application/json"
        req.headers["x-csrf-token"] = X_CSRF_TOKEN
        req.headers["Cookie"] = @user_cookies.map(&:to_s).join("; ")
      end

      expect(create_resp.status).to eq(200)
      model_id = create_resp.body["id"]

      get_resp = client.get("#{models_url}#{model_id}")
      expect(get_resp.status).to eq(200)
      expect(get_resp.body["transportable"]).to eq(false)

      update_resp = client.put("#{models_url}#{model_id}") do |req|
        req.body = {
          product: "Not Transportable Model",
          version: "1",
          type: "Model",
          is_package: false,
          transportable: true
        }.to_json
        req.headers["Content-Type"] = "application/json"
        req.headers["Accept"] = "application/json"
        req.headers["x-csrf-token"] = X_CSRF_TOKEN
        req.headers["Cookie"] = @user_cookies.map(&:to_s).join("; ")
      end

      expect(update_resp.status).to eq(200)
      get_again = client.get("#{models_url}#{model_id}")
      expect(get_again.body["transportable"]).to eq(true)
    end
  end
end
