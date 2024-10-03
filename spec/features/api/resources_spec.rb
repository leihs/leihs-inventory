require "spec_helper"

feature "Call swagger-endpoints" do
  context "revoking the token ", driver: :selenium_headless do
    {
      "/inventory/zhdk-logo.svg" => 200,
      "/inventory/locales/de/translation.json" => 200,
      "/inventory/locales/fr/translation.json" => 200,
      "/inventory/locales/en/translation.json" => 200,
      "/inventory/locales/es/translation.json" => 200,
      "/inventory/css/additional.css" => 200,

      "/inventory/zhdk-logo.svg" => 200,
      "/inventory/static/zhdk-logo.svg" => 302
    }.each do |url, code|
      it "accessing #{url}    results in expected status-code" do
        response = plain_faraday_client.get(url)
        expect(response.status).to eq(code)
      end
    end
  end

  context "with accept=text/html", driver: :selenium_headless do
    before :each do
      @user = FactoryBot.create(:user, login: "test-user")
    end

    it "accesses protected resource with valid session cookie" do
      resp = plain_faraday_json_client.get("/inventory/session/protected")
      expect(resp.status).to eq(403)

      resp = basic_auth_plain_faraday_json_client(@user.login, @user.password).get("/inventory/login")
      expect(resp.status).to eq(200)

      cookie_token = parse_cookie(resp.headers["set-cookie"])["leihs-user-session"]
      cookie = CGI::Cookie.new("name" => "leihs-user-session", "value" => cookie_token)

      resp = session_auth_plain_faraday_json_client(cookie.to_s).get("/inventory/static/zhdk-logo.svg") do |req|
        req.headers["Accept"] = "*/*"
        req.headers["Cookie"] = cookie.to_s
      end

      expect(resp.status).to eq(200)
    end
  end
end