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
  let(:software_url) { "/inventory/#{inventory_pool_id}/software/" }

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

  describe "software transportable persistence" do
    it "defaults transportable to true when omitted on create" do
      resp = client.post(software_url) do |req|
        req.body = {
          product: "Default Transportable Software",
          version: "1"
        }.to_json
        req.headers["Content-Type"] = "application/json"
        req.headers["Accept"] = "application/json"
        req.headers["x-csrf-token"] = X_CSRF_TOKEN
        req.headers["Cookie"] = @user_cookies.map(&:to_s).join("; ")
      end

      expect(resp.status).to eq(200)
      software_id = resp.body["id"]
      get_resp = client.get("#{software_url}#{software_id}")
      expect(get_resp.status).to eq(200)
      expect(get_resp.body["transportable"]).to eq(true)
    end

    it "persists transportable false on create and update" do
      create_resp = client.post(software_url) do |req|
        req.body = {
          product: "Not Transportable Software",
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
      expect(create_resp.body["transportable"]).to eq(false)

      get_resp = client.get("#{software_url}#{software_id}")
      expect(get_resp.status).to eq(200)
      expect(get_resp.body["transportable"]).to eq(false)

      update_resp = client.put("#{software_url}#{software_id}") do |req|
        req.body = {
          product: "Not Transportable Software",
          version: "1",
          transportable: true
        }.to_json
        req.headers["Content-Type"] = "application/json"
        req.headers["Accept"] = "application/json"
        req.headers["x-csrf-token"] = X_CSRF_TOKEN
        req.headers["Cookie"] = @user_cookies.map(&:to_s).join("; ")
      end

      expect(update_resp.status).to eq(200)
      expect(update_resp.body["transportable"]).to eq(true)
      get_again = client.get("#{software_url}#{software_id}")
      expect(get_again.body["transportable"]).to eq(true)
    end
  end
end
