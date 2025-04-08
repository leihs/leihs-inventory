require "spec_helper"
require "pry"
require_relative "_shared"

feature "Call swagger-endpoints" do
  context "with accept=text/html", driver: :selenium_headless do
    before :each do
      @user, @user_cookies, @user_cookies_str, @cookie_token = create_and_login(:user)
      @create_token_url = "/inventory/token/"
      @protected_url = "/inventory/token/protected"
    end

    let(:client) { session_auth_plain_faraday_json_client(cookies: @user_cookies) }

    it "returns 403 for unauthenticated request" do
      resp = plain_faraday_json_client.post @create_token_url do |req|
        req.body = {
          description: "string",
          scopes: {
            read: true,
            write: true,
            admin_read: true,
            admin_write: true
          }
        }.to_json
        req.headers["Content-Type"] = "application/json"
        req.headers["Accept"] = "application/json"
        req.headers["x-csrf-token"] = X_CSRF_TOKEN
      end
      expect(resp.status).to eq(403)
    end

    it "returns 200 for correct credentials" do
      resp = client.post(@create_token_url) do |req|
        req.body = {
          description: "string",
          scopes: {
            read: true,
            write: true,
            admin_read: true,
            admin_write: true
          }
        }.to_json
        req.headers["Content-Type"] = "application/json"
        req.headers["Accept"] = "application/json"
        req.headers["x-csrf-token"] = X_CSRF_TOKEN
      end
      expect(resp.status).to eq(200)
    end

    it "returns 200 and valid token for protected resource access" do
      resp = plain_faraday_json_client.get(@protected_url)
      expect(resp.status).to eq(403)

      resp = client.post(@create_token_url) do |req|
        req.body = {
          description: "string",
          scopes: {
            read: true,
            write: true,
            admin_read: true,
            admin_write: true
          }
        }.to_json
        req.headers["Content-Type"] = "application/json"
        req.headers["Accept"] = "application/json"
        req.headers["x-csrf-token"] = X_CSRF_TOKEN
      end
      expect(resp.status).to eq(200)
      token = resp.body["token"]
      expect(token).to be

      resp = json_client_get(@protected_url, token: token)
      expect(resp.status).to eq(200)
    end

    it "returns 200 with all scopes set to false" do
      resp = client.post(@create_token_url) do |req|
        req.body = {
          description: "string",
          scopes: {
            read: false,
            write: false,
            admin_read: false,
            admin_write: false
          }
        }.to_json
        req.headers["Content-Type"] = "application/json"
        req.headers["Accept"] = "application/json"
        req.headers["x-csrf-token"] = X_CSRF_TOKEN
      end
      expect(resp.status).to eq(200)
      token = resp.body["token"]
      expect(token).to be

      resp = json_client_get(@protected_url, token: token)
      expect(resp.status).to eq(200)
      expect(resp.body["token"]["scopes"].values).to all(eq(false))
    end

    it "redirects to sign-in when accessing protected resource without valid token" do
      resp = plain_faraday_json_client.get(@protected_url)
      expect(resp.status).to eq(403)

      resp = client.post(@create_token_url) do |req|
        req.body = {
          description: "string",
          scopes: {
            read: true,
            write: true,
            admin_read: true,
            admin_write: true
          }
        }.to_json
        req.headers["Content-Type"] = "application/json"
        req.headers["Accept"] = "application/json"
        req.headers["x-csrf-token"] = X_CSRF_TOKEN
      end
      expect(resp.status).to eq(200)
      token = resp.body["token"]
      expect(token).to be

      resp = json_client_get(@protected_url, headers: {"Accept" => "text/html"}, token: token)
      expect(resp.status).to eq(302)
      expect(resp.headers["location"]).to eq "/sign-in?return-to=%2Finventory"
    end
  end
end
