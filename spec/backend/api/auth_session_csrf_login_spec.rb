require "spec_helper"
require_relative "_shared"

describe "Call swagger-endpoints" do
  context "with accept=text/html2" do
    it "redirect to login if request not comes from swagger-ui" do
      resp = plain_faraday_json_client.get("/inventory/session/protected") do |req|
        req.headers["Referer"] = "/inventory"
      end
      expect(resp.status).to eq(401)
    end

    it "returns correct result 200 SPA that results in a 404" do
      resp = plain_faraday_html_client.get("/inventory/session/protected")
      expect_spa_content(resp, 200)
    end

    it "returns correct result 401" do
      resp = plain_faraday_html_client.get("/inventory/session/protected") do |req|
        req.headers["Accept"] = "application/json"
      end
      expect(resp.status).to eq(401)
    end

    it "denies access to protected resource without login" do
      resp = plain_faraday_json_client.get("/inventory/session/protected")
      expect(resp.status).to eq(401)
    end
  end

  context "with accept=text/html" do
    before :each do
      @user, @user_cookies, @user_cookies_str, @cookie_token = create_and_login(:user)
    end

    let(:client) { session_auth_plain_faraday_json_client(cookies: @user_cookies) }
    let(:cookie2) { [CGI::Cookie.new("name" => "leihs-user-session", "value" => @cookie_token)] }

    it "denies access to protected resource without login" do
      resp = plain_faraday_json_client.get("/inventory/session/protected")
      expect(resp.status).to eq(401)
    end

    context "login flow" do
      it "fails with invalid CSRF token/session" do
        resp = common_plain_faraday_login_client(:post, "/sign-in", body: {
          "user" => @user.login,
          "password" => @user.password,
          "csrf-token" => X_CSRF_TOKEN,
          "return-to" => "/inventory/"
        })
        expect(resp.status).to eq(302)
        expect(resp.headers["location"]).to eq("/sign-in?return-to=%2Finventory&message=CSRF-Token/Session not valid")
      end

      it "succeeds with valid session and CSRF token" do
        resp = common_plain_faraday_login_client(:post, "/sign-in", body: {
          "user" => @user.login,
          "password" => @user.password,
          "csrf-token" => X_CSRF_TOKEN,
          "return-to" => "/inventory/"
        }, headers: {"Cookie" => @user_cookies_str})

        expect(resp.status).to eq(302)
        expect(resp.headers["location"]).to match(%r{/inventory/})
      end
    end

    context "CSRF-protected endpoints" do
      let(:auth_client) { session_auth_plain_faraday_json_csrf_client(cookies: @user_cookies) }
      let(:auth_client_no) { session_auth_plain_faraday_json_client(cookies: @user_cookies) }
      let(:auth_client_no2) { session_auth_plain_faraday_json_client(cookies: cookie2) }

      it "GET /test-csrf succeeds with session" do
        resp = auth_client.get("/inventory/test-csrf")
        expect(resp.status).to eq(200)
      end

      it "PUT /test-csrf with token and cookie succeeds" do
        resp = auth_client_no.put("/inventory/test-csrf") do |req|
          req.headers["Content-Type"] = "application/json"
          req.headers["x-csrf-token"] = X_CSRF_TOKEN
          req.headers["Cookie"] = @user_cookies_str
        end
        expect(resp.status).to eq(200)
      end

      it "PUT /test-csrf missing cookie returns error" do
        resp = auth_client_no2.put("/inventory/test-csrf") do |req|
          req.headers["Content-Type"] = "application/json"
          req.headers["x-csrf-token"] = X_CSRF_TOKEN
        end
        expect(resp.status).to eq(403)
        expect(resp.body["details"]).to eq("The anti-csrf-token cookie value is not set.")
      end

      it "PUT /test-csrf missing token returns error" do
        resp = auth_client_no.put("/inventory/test-csrf") do |req|
          req.headers["Content-Type"] = "application/json"
          req.headers["Cookie"] = @user_cookies_str
        end

        expect(resp.status).to eq(403)
        expect(resp.body["details"]).to eq("The x-csrf-token has not been send!")
      end

      it "PUT /test-csrf missing token and cookie returns error" do
        resp = auth_client_no2.put("/inventory/test-csrf") do |req|
          req.headers["Content-Type"] = "application/json"
        end
        expect(resp.status).to eq(403)
        expect(resp.body["details"]).to eq("The anti-csrf-token cookie value is not set.")
      end

      it "PUT /test-csrf with mismatched token returns error" do
        resp = auth_client.put("/inventory/test-csrf") do |req|
          req.headers["Content-Type"] = "application/json"
          req.headers["x-csrf-token"] = "not-correct-token"
          req.headers["Cookie"] = @user_cookies_str
        end
        expect(resp.status).to eq(403)
        expect(resp.body["details"]).to eq("The x-csrf-token is not equal to the anti-csrf cookie value.")
      end

      it "POST /test-csrf with valid CSRF headers succeeds" do
        resp = auth_client.post("/inventory/test-csrf") do |req|
          req.headers["Content-Type"] = "application/json"
          req.headers["x-csrf-token"] = X_CSRF_TOKEN
          req.headers["Cookie"] = @user_cookies_str
        end
        expect(resp.status).to eq(200)
      end

      it "DELETE /test-csrf with valid CSRF headers succeeds" do
        resp = auth_client.delete("/inventory/test-csrf") do |req|
          req.headers["Content-Type"] = "application/json"
          req.headers["x-csrf-token"] = X_CSRF_TOKEN
          req.headers["Cookie"] = @user_cookies_str
        end
        expect(resp.status).to eq(200)
      end
    end
  end
end
