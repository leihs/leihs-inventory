require "spec_helper"
require "pry"

feature "Call swagger-endpoints" do
  context "with accept=text/html", driver: :selenium_headless do
    before :each do
      @user = FactoryBot.create(:user, login: "test-user")
      @create_token_url = "/inventory/token/"
      @protected_url = "/inventory/token/protected"
    end

    let(:client) { plain_faraday_json_client }

    it "returns 401 for unauthenticated request" do
      resp = client.post @create_token_url do |req|
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
      end
      expect(resp.status).to eq(401)
    end

    it "returns 401 for incorrect credentials" do
      resp = basic_auth_plain_faraday_json_client("abc", "def").post(@create_token_url) do |req|
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
      end
      expect(resp.status).to eq(401)
    end

    it "returns 200 for correct credentials" do
      resp = basic_auth_plain_faraday_json_client(@user.login, @user.password).post(@create_token_url) do |req|
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
      end
      expect(resp.status).to eq(200)
    end

    it "returns 200 and valid token for protected resource access" do
      resp = plain_faraday_json_client.get(@protected_url)
      expect(resp.status).to eq(401)

      resp = basic_auth_plain_faraday_json_client(@user.login, @user.password).post(@create_token_url) do |req|
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
        # req.headers["Accept"] = "text/html"
      end
      expect(resp.status).to eq(200)
      token = resp.body["token"]
      expect(token).to be

      resp = json_client_get(@protected_url, token: token)
      expect(resp.status).to eq(200)
    end

    it "returns 200 with all scopes set to false" do
      resp = basic_auth_plain_faraday_json_client(@user.login, @user.password).post(@create_token_url) do |req|
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
      expect(resp.status).to eq(401)

      resp = basic_auth_plain_faraday_json_client(@user.login, @user.password).post(@create_token_url) do |req|
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
