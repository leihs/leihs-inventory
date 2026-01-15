require "spec_helper"
require "pry"
require_relative "../_shared"

describe "Coercion test" do
  context "when fetching models for an inventory pool" do
    include_context :setup_models_min_api

    before :each do
      @user, @user_cookies, @user_cookies_str, @cookie_token = create_and_login(:user)
      create(:access_right, user: @user, inventory_pool: @inventory_pool, role: "inventory_manager")
    end

    let(:client) { session_auth_plain_faraday_json_client(cookies: @user_cookies) }
    let(:inventory_pool_id) { @inventory_pool.id }

    context "GET /inventory/:pool_id/models for a specific pool" do
      let(:url) { "/inventory/#{@inventory_pool.id}/list/" }

      it "returns paginated empty results for a new pool and returns status 200" do
        resp = client.get "#{url}?page=3&size=1"
        expect(resp.status).to eq(200)
        expect(resp.body["data"].count).to eq(0)
      end

      it "returns 404 with invalid page-attribute" do
        resp = client.get "#{url}?page=abc"
        expect(resp.status).to eq(404)
        expect(resp.body).to eq({"coercion-type" => "schema", "detail" => "Request coercion failed",
                                  "reason" => "Coercion-Error", "scope" => "request/query-params",
                                  "uri" => "GET /inventory/#{@inventory_pool.id}/list/"})
      end
    end
  end

  context "when interacting with inventory item with role=lending_manager" do
    include_context :setup_models_api_model, "lending_manager"
    include_context :generate_session_header

    let(:pool_id) { @inventory_pool.id }
    let(:cookie_header) { @cookie_header }
    let(:client) { plain_faraday_json_client(cookie_header) }

    context "fetch of form" do
      it "return 404 with invalid uuid (schema)" do
        resp = client.get "/inventory/invalid-uuid/items/"
        expect(resp.status).to eq(404)
        expect(resp.body).to eq({"coercion-type" => "schema", "detail" => "Request coercion failed",
                                 "reason" => "Coercion-Error", "scope" => "request/path-params",
                                 "uri" => "GET /inventory/invalid-uuid/items/"})
      end

      it "return 404 with invalid uuid (spec)" do
        resp = client.get "/inventory/invalid-uuid/models/invalid-uuid"
        expect(resp.status).to eq(404)
        expect(resp.body).to eq({"coercion-type" => "spec", "detail" => "Request coercion failed", "reason" => "Coercion-Error",
                                 "scope" => "request/path-params", "uri" => "GET /inventory/invalid-uuid/models/invalid-uuid"})
      end
    end
  end
end
