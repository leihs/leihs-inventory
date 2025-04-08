require "spec_helper"
require_relative "_shared"

feature "Call swagger-endpoints" do
  context "with accept=text/html", driver: :selenium_headless do
    before :each do
      @user = FactoryBot.create(:admin, login: "test-user")
    end

    let(:client) { plain_faraday_json_client }

    it "returns 403 for unauthenticated request" do
      resp = client.get "/inventory/login"
      expect(resp.status).to eq(403)
    end

    it "returns 403 for incorrect credentials" do
      resp = basic_auth_plain_faraday_json_client("abc", "def").get("/inventory/login")
      expect(resp.status).to eq(403)
    end

    it "returns 200 for correct credentials" do
      resp = basic_auth_plain_faraday_json_client(@user.login, @user.password).get("/inventory/login")
      expect(resp.status).to eq(200)
    end

    it "accesses protected resource with valid session cookie" do
      resp = plain_faraday_json_client.get("/inventory/session/protected")
      expect(resp.status).to eq(403)

      resp = basic_auth_plain_faraday_json_client(@user.login, @user.password).get("/inventory/login")
      expect(resp.status).to eq(200)

      cookie_token = parse_cookie(resp.headers["set-cookie"])["leihs-user-session"]
      cookies, cookies_str = generate_csrf_data(cookie_token)

      resp = session_auth_plain_faraday_json_client(cookies: cookies).get("/inventory/session/protected") do |req|
        req.headers["Content-Type"] = "application/json"
        req.headers["Cookie"] = cookies_str
        req.headers["x-csrf-token"] = X_CSRF_TOKEN
      end

      expect(resp.status).to eq(200)
    end

    it "accesses protected resource with valid session cookie" do
      resp = plain_faraday_json_client.get("/inventory/session/protected")
      expect(resp.status).to eq(403)

      resp = plain_faraday_json_client.put("/inventory/dev/update-accounts")
      expect(resp.status).to eq(403)

      puts "before login login #{@user.login} password #{@user.password}"
      resp = basic_auth_plain_faraday_json_client(@user.login, @user.password).get("/inventory/login")
      expect(resp.status).to eq(200)

      cookie_token = parse_cookie(resp.headers["set-cookie"])["leihs-user-session"]
      cookies, cookies_str = generate_csrf_data(cookie_token)

      resp = session_auth_plain_faraday_json_client(cookies: cookies).put("/inventory/dev/update-accounts") do |req|
        req.headers["Content-Type"] = "application/json"
        req.headers["Cookie"] = cookies_str
        req.headers["x-csrf-token"] = X_CSRF_TOKEN
      end

      expect(resp.status).to eq(200)
    end

    it "accesses protected resource" do
      resp = plain_faraday_json_client.get("/inventory/session/protected")
      expect(resp.status).to eq(403)

      resp = plain_faraday_json_client.put("/inventory/dev/update-accounts")
      expect(resp.status).to eq(403)

      resp = plain_faraday_json_client.put("/inventory/dev/update-accounts")
      expect(resp.status).to eq(403)
    end
  end

  context "with accept=text/html", driver: :selenium_headless do
    it "returns 403 for incorrect credentials" do
      resp = basic_auth_plain_faraday_json_client("invalid-login", "invalid-pw").get("/inventory/login")
      expect(resp.status).to eq(403)
    end
  end

  context "with accept=text/html", driver: :selenium_headless do
    before :each do
      @user, @user_cookies, @user_cookies_str, @cookie_token = create_and_login(:user)
    end

    let(:client) { session_auth_plain_faraday_json_csrf_client(cookies: @user_cookies) }

    it "returns 403 for unauthenticated request" do
      resp = client.get "/inventory/login"
      expect(resp.status).to eq(403)
    end

    # FIXME: returns 200, shouldn't be possible
    # it "returns 403 for incorrect credentials" do
    #   resp = basic_auth_plain_faraday_json_client("invalid-login", "invalid-pw").get("/inventory/login")
    #   binding.pry
    #   expect(resp.status).to eq(403)
    # end

    it "returns 200 for correct credentials" do
      resp = basic_auth_plain_faraday_json_client(@user.login, @user.password).get("/inventory/login")
      expect(resp.status).to eq(200)
    end

    it "accesses protected resource with valid session cookie" do
      resp = plain_faraday_json_client.get("/inventory/session/protected")
      expect(resp.status).to eq(403)

      resp = basic_auth_plain_faraday_json_client(@user.login, @user.password).get("/inventory/login")
      expect(resp.status).to eq(200)

      cookie_token = parse_cookie(resp.headers["set-cookie"])["leihs-user-session"]
      cookies, cookies_str = generate_csrf_data(cookie_token)

      resp = session_auth_plain_faraday_json_client(cookies: cookies).get("/inventory/session/protected") do |req|
        req.headers["Content-Type"] = "application/json"
        req.headers["Cookie"] = cookies_str
        req.headers["x-csrf-token"] = X_CSRF_TOKEN
      end

      expect(resp.status).to eq(200)
    end

    it "forbidden access of resource with valid session cookie" do
      resp = plain_faraday_json_client.get("/inventory/session/protected")
      expect(resp.status).to eq(403)

      resp = plain_faraday_json_client.put("/inventory/dev/update-accounts")
      expect(resp.status).to eq(403)

      puts "before login login #{@user.login} password #{@user.password}"
      resp = basic_auth_plain_faraday_json_client(@user.login, @user.password).get("/inventory/login")
      # binding.pry
      expect(resp.status).to eq(200)

      cookie_token = parse_cookie(resp.headers["set-cookie"])["leihs-user-session"]
      cookies, cookies_str = generate_csrf_data(cookie_token)

      resp = session_auth_plain_faraday_json_client(cookies: cookies).put("/inventory/dev/update-accounts") do |req|
        req.headers["Content-Type"] = "application/json"
        req.headers["Cookie"] = cookies_str
        req.headers["x-csrf-token"] = X_CSRF_TOKEN
      end

      expect(resp.status).to eq(403)
    end

    it "forbidden access of resource with valid basicAuth" do
      resp = plain_faraday_json_client.get("/inventory/session/protected")
      expect(resp.status).to eq(403)
      expect(resp.body["message"]).to eq("Unauthorized")

      resp = plain_faraday_json_client.put("/inventory/dev/update-accounts")
      expect(resp.status).to eq(403)

      puts "before login login #{@user.login} password #{@user.password}"
      resp = plain_faraday_json_client.put("/inventory/dev/update-accounts")
      expect(resp.status).to eq(403)
    end
  end
end
