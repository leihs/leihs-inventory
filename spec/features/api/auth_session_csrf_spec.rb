require "spec_helper"
require_relative "_shared"

feature "Call swagger-endpoints" do
  context "with accept=text/html", driver: :selenium_headless do
    before :each do
      @user, @user_cookies, @user_cookies_str, @cookie_token = create_and_login(:user)
    end

    let(:client) { session_auth_plain_faraday_json_csrf_client(cookies: @user_cookies) }

    it "accesses protected resource with valid session cookie" do
      resp = plain_faraday_json_client.get("/inventory/session/protected")
      expect(resp.status).to eq(403)

      resp = client.get("/test-csrf")
      expect(resp.status).to eq(200)

      resp = client.post("/test-csrf")
      expect(resp.status).to eq(200)

      resp = client.delete("/test-csrf")
      expect(resp.status).to eq(200)

      resp = client.put("/test-csrf")
      expect(resp.status).to eq(200)
    end

    it "accesses protected json-resource by accept=*/*" do
      resp = plain_faraday_json_client.get("/test-csrf")
      expect(resp.status).to eq(403)

      resp = plain_faraday_json_client.post("/test-csrf")
      expect(resp.status).to eq(403)

      resp = plain_faraday_json_client.delete("/test-csrf")
      expect(resp.status).to eq(403)

      resp = plain_faraday_json_client.put("/test-csrf")
      expect(resp.status).to eq(403)
    end

    it "accesses protected json-resource by accept=*/*" do
      resp = plain_faraday_client.get("/test-csrf")
      expect(resp.status).to eq(302)

      resp = plain_faraday_client.post("/test-csrf")
      expect(resp.status).to eq(404)

      resp = plain_faraday_client.delete("/test-csrf")
      expect(resp.status).to eq(404)

      resp = plain_faraday_client.put("/test-csrf")
      expect(resp.status).to eq(404)
    end
  end
end
