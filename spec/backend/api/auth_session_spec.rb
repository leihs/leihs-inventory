require "spec_helper"
require_relative "_shared"

describe "Call swagger-endpoints" do
  context "with accept=text/html" do
    before :each do
      @user, @user_cookies, @user_cookies_str, @cookie_token = create_and_login(:user)
    end

    let(:client) { session_auth_plain_faraday_json_client(cookies: @user_cookies) }

    it "returns 200" do
      resp = client.get "/sign-in"
      expect(resp.status).to eq(200)
    end

    # FIXME uses session instead of basicAuth
    # it "returns 403 for incorrect credentials" do
    #   resp = basic_auth_plain_faraday_json_client("abc", "def").get("/sign-in")
    #   expect(resp.status).to eq(403)
    # end

    it "returns 200 for correct credentials" do
      resp = client.get("/sign-in")
      expect(resp.status).to eq(200)
    end

    it "accesses protected resource with valid session cookie" do
      resp = plain_faraday_json_client.get("/inventory/session/protected")
      expect(resp.status).to eq(403)

      resp = client.get("/sign-in")
      expect(resp.status).to eq(200)

      resp = client.get("/inventory/session/protected") do |req|
        req.headers["Content-Type"] = "application/json"
        req.headers["Cookie"] = @user_cookies_str
      end

      expect(resp.status).to eq(200)
    end
  end
end
