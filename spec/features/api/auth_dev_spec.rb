require "spec_helper"

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
      cookie = CGI::Cookie.new("name" => "leihs-user-session", "value" => cookie_token)

      resp = session_auth_plain_faraday_json_client(cookie.to_s).get("/inventory/session/protected") do |req|
        req.headers["Content-Type"] = "application/json"
        req.headers["Cookie"] = cookie.to_s
      end

      expect(resp.status).to eq(200)
    end

    it "accesses protected resource with valid session cookie" do
      resp = plain_faraday_json_client.get("/inventory/session/protected")
      expect(resp.status).to eq(403)

      resp = plain_faraday_json_client.get("/inventory/dev/update-accounts")
      expect(resp.status).to eq(403)

      puts "before login login #{@user.login} password #{@user.password}"
      resp = basic_auth_plain_faraday_json_client(@user.login, @user.password).get("/inventory/login")
      expect(resp.status).to eq(200)

      cookie_token = parse_cookie(resp.headers["set-cookie"])["leihs-user-session"]
      cookie = CGI::Cookie.new("name" => "leihs-user-session", "value" => cookie_token)

      resp = session_auth_plain_faraday_json_client(cookie.to_s).get("/inventory/dev/update-accounts") do |req|
        req.headers["Content-Type"] = "application/json"
        req.headers["Cookie"] = cookie.to_s
      end

      expect(resp.status).to eq(200)
    end

    it "accesses protected resource with valid basicAuth" do
      resp = plain_faraday_json_client.get("/inventory/session/protected")
      expect(resp.status).to eq(403)

      resp = plain_faraday_json_client.get("/inventory/dev/update-accounts")
      expect(resp.status).to eq(403)

      puts "before login login #{@user.login} password #{@user.password}"
      resp = basic_auth_plain_faraday_json_client(@user.login, @user.password).get("/inventory/dev/update-accounts")
      expect(resp.status).to eq(200)
    end
  end

  context "with accept=text/html", driver: :selenium_headless do
    before :each do
      @user = FactoryBot.create(:user, login: "test-user")
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
      cookie = CGI::Cookie.new("name" => "leihs-user-session", "value" => cookie_token)

      resp = session_auth_plain_faraday_json_client(cookie.to_s).get("/inventory/session/protected") do |req|
        req.headers["Content-Type"] = "application/json"
        req.headers["Cookie"] = cookie.to_s
      end

      expect(resp.status).to eq(200)
    end

    it "forbidden access of resource with valid session cookie" do
      resp = plain_faraday_json_client.get("/inventory/session/protected")
      expect(resp.status).to eq(403)

      resp = plain_faraday_json_client.get("/inventory/dev/update-accounts")
      expect(resp.status).to eq(403)

      puts "before login login #{@user.login} password #{@user.password}"
      resp = basic_auth_plain_faraday_json_client(@user.login, @user.password).get("/inventory/login")
      # binding.pry
      expect(resp.status).to eq(200)

      cookie_token = parse_cookie(resp.headers["set-cookie"])["leihs-user-session"]
      cookie = CGI::Cookie.new("name" => "leihs-user-session", "value" => cookie_token)

      resp = session_auth_plain_faraday_json_client(cookie.to_s).get("/inventory/dev/update-accounts") do |req|
        req.headers["Content-Type"] = "application/json"
        req.headers["Cookie"] = cookie.to_s
      end

      expect(resp.status).to eq(403)
    end

    it "forbidden access of resource with valid basicAuth" do
      resp = plain_faraday_json_client.get("/inventory/session/protected")
      expect(resp.status).to eq(403)

      resp = plain_faraday_json_client.get("/inventory/dev/update-accounts")
      expect(resp.status).to eq(403)

      puts "before login login #{@user.login} password #{@user.password}"
      resp = basic_auth_plain_faraday_json_client(@user.login, @user.password).get("/inventory/dev/update-accounts")
      expect(resp.status).to eq(403)
    end
  end
end
