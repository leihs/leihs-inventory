require "spec_helper"
require_relative "../_shared"

describe "Inventory model transportable" do
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

  describe "model form includes enable_alternative_pickup_locations" do
    it "returns the pool flag on model GET" do
      model = FactoryBot.create(:leihs_model)
      expect(@inventory_pool.enable_alternative_pickup_locations).to eq(false)

      resp = client.get("#{models_url}#{model.id}")
      expect(resp.status).to eq(200)
      expect(resp.body["enable_alternative_pickup_locations"]).to eq(false)

      @inventory_pool.update(enable_alternative_pickup_locations: true)
      resp = client.get("#{models_url}#{model.id}")
      expect(resp.status).to eq(200)
      expect(resp.body["enable_alternative_pickup_locations"]).to eq(true)
    end

    it "returns the pool flag on form-meta GET" do
      expect(@inventory_pool.enable_alternative_pickup_locations).to eq(false)

      resp = client.get("#{models_url}form-meta")
      expect(resp.status).to eq(200)
      expect(resp.body["enable_alternative_pickup_locations"]).to eq(false)

      @inventory_pool.update(enable_alternative_pickup_locations: true)
      resp = client.get("#{models_url}form-meta")
      expect(resp.status).to eq(200)
      expect(resp.body["enable_alternative_pickup_locations"]).to eq(true)
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

  describe "software endpoints do not expose transportable" do
    let(:software_url) { "/inventory/#{inventory_pool_id}/software/" }

    it "does not return transportable on software GET" do
      software = FactoryBot.create(:leihs_model, type: "Software", product: "SW-No-Transportable")
      resp = client.get("#{software_url}#{software.id}")
      expect(resp.status).to eq(200)
      expect(resp.body).not_to have_key("transportable")
      expect(resp.body).not_to have_key("enable_alternative_pickup_locations")
    end

    it "ignores transportable on software create and update" do
      create_resp = client.post(software_url) do |req|
        req.body = {
          product: "SW-Ignore-Transportable",
          version: "1",
          transportable: false
        }.to_json
        req.headers["Content-Type"] = "application/json"
        req.headers["Accept"] = "application/json"
        req.headers["x-csrf-token"] = X_CSRF_TOKEN
        req.headers["Cookie"] = @user_cookies.map(&:to_s).join("; ")
      end

      expect(create_resp.status).to eq(200)
      software_id = create_resp.body["id"]
      expect(create_resp.body).not_to have_key("transportable")

      software = LeihsModel[software_id]
      expect(software.transportable).to eq(true)

      update_resp = client.put("#{software_url}#{software_id}") do |req|
        req.body = {
          product: "SW-Ignore-Transportable",
          version: "1",
          transportable: false
        }.to_json
        req.headers["Content-Type"] = "application/json"
        req.headers["Accept"] = "application/json"
        req.headers["x-csrf-token"] = X_CSRF_TOKEN
        req.headers["Cookie"] = @user_cookies.map(&:to_s).join("; ")
      end

      expect(update_resp.status).to eq(200)
      expect(update_resp.body).not_to have_key("transportable")
      expect(LeihsModel[software_id].transportable).to eq(true)
    end
  end
end
