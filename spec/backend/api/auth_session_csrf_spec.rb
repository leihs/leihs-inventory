require "spec_helper"
require_relative "_shared"

describe "Call swagger-endpoints" do
  context "with accept=text/html" do
    before :each do
      @user, @user_cookies, @user_cookies_str, @cookie_token = create_and_login(:user)
    end

    let(:client) { session_auth_plain_faraday_json_csrf_client(cookies: @user_cookies) }

    it "accesses protected resource with valid session cookie" do
      resp = plain_faraday_json_client.get("/inventory/session/protected")
      expect(resp.status).to eq(403)

      resp = client.get("/inventory/test-csrf")
      expect(resp.status).to eq(200)

      resp = client.post("/inventory/test-csrf")
      expect(resp.status).to eq(200)

      resp = client.delete("/inventory/test-csrf")
      expect(resp.status).to eq(200)

      resp = client.put("/inventory/test-csrf")
      expect(resp.status).to eq(200)

      resp = client.patch("/inventory/test-csrf")
      expect(resp.status).to eq(200)
    end

    context "accesses protected json-resource by accept=application/json" do
      it "return 404 for invalid url" do
        resp = client.get("/inventory/whats/up/222")
        expect(resp.status).to eq(404)
      end

      it "return 404 for invalid url" do
        resp = client.get("/invalid/inventory/whats/up/222")
        expect(resp.status).to eq(404)
      end

      it "return 404 for unknown id by model-id" do
        resp = client.get("/inventory/models/8bd16d45-0000-0000-0000-12849f034351")
        expect(resp.status).to eq(404)
      end

      it "return 404 for unknown id by pool-id/model-id" do
        resp = client.get("/inventory/8bd16d45-0000-0000-0000-12849f034351/models/8bd16d45-0000-0000-0000-12849f034351")
        expect(resp.status).to eq(404)
      end
    end

    it "accesses protected json-resource by accept=*/*" do
      resp = plain_faraday_json_client.get("/inventory/test-csrf")
      expect(resp.status).to eq(403)

      resp = plain_faraday_json_client.post("/inventory/test-csrf")
      expect(resp.status).to eq(403)

      resp = plain_faraday_json_client.delete("/inventory/test-csrf")
      expect(resp.status).to eq(403)

      resp = plain_faraday_json_client.put("/inventory/test-csrf")
      expect(resp.status).to eq(403)

      resp = plain_faraday_json_client.patch("/inventory/test-csrf")
      expect(resp.status).to eq(403)
    end

    it "protected access against json-resource by accept=*/*" do
      resp = plain_faraday_client.get("/inventory/test-csrf")
      expect(resp.status).to eq(404)

      resp = plain_faraday_client.post("/inventory/test-csrf")
      expect(resp.status).to eq(403)

      resp = plain_faraday_client.delete("/inventory/test-csrf")
      expect(resp.status).to eq(403)

      resp = plain_faraday_client.put("/inventory/test-csrf")
      expect(resp.status).to eq(403)

      resp = plain_faraday_client.patch("/inventory/test-csrf")
      expect(resp.status).to eq(403)
    end
  end
end
