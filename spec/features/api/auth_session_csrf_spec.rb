require "spec_helper"

feature "Call swagger-endpoints" do
  context "with accept=text/html", driver: :selenium_headless do
    before :each do
      @user = FactoryBot.create(:user, login: "test", password: "test")
    end

    let(:client) { plain_faraday_json_client }

    # it "returns 403 for unauthenticated request" do
    #   resp = client.get "/inventory/login"
    #   expect(resp.status).to eq(403)
    # end
    #
    # it "returns 403 for incorrect credentials" do
    #   resp = basic_auth_plain_faraday_json_client("abc", "def").get("/inventory/login")
    #   expect(resp.status).to eq(403)
    # end
    #
    # it "returns 200 for correct credentials" do
    #   resp = basic_auth_plain_faraday_json_client(@user.login, @user.password).get("/inventory/login")
    #   expect(resp.status).to eq(200)
    # end

    it "accesses protected resource with valid session cookie" do
      resp = plain_faraday_json_client.get("/inventory/session/protected")
      expect(resp.status).to eq(403)

      resp = basic_auth_plain_faraday_json_client(@user.login, @user.password).get("/inventory/login")
      expect(resp.status).to eq(200)

      csrf_token = "abc-def-ghi"

      cookie_token = parse_cookie(resp.headers["set-cookie"])["leihs-user-session"]
      cookie1 = CGI::Cookie.new("name" => "leihs-user-session", "value" => cookie_token)
      cookie2 = CGI::Cookie.new("name" => "leihs-anti-csrf-token", "value" => X_CSRF_TOKEN)

      cookie_str = "#{cookie1}; #{cookie2}"

      # resp = session_auth_plain_faraday_json_client(cookie_string: cookie_str).get("/test-csrf")
      resp = session_auth_plain_faraday_json_client().get("/test-csrf")

      # resp = session_auth_plain_faraday_json_client(cookie_string: cookie_str).get("/test-csrf") do |req|
      # resp = session_auth_plain_faraday_json_client().get("/test-csrf") do |req|
      #   req.headers["Content-Type"] = "application/json"
      #   req.headers["x-csrf-token"] = X_CSRF_TOKEN
      #   # req.headers["Cookie"] = cookie1.merge.cookie2.to_s
      #   # req.headers["Cookie"] = "#{cookie1}; #{cookie2}"
      # end

      expect(resp.status).to eq(200)
    end



    it "accesses protected resource with valid session cookie" do
      resp = plain_faraday_json_client.get("/inventory/session/protected")
      expect(resp.status).to eq(403)

      resp = basic_auth_plain_faraday_json_client(@user.login, @user.password).get("/inventory/login")
      expect(resp.status).to eq(200)

      csrf_token = "abc-def-ghi"

      cookie_token = parse_cookie(resp.headers["set-cookie"])["leihs-user-session"]
      cookie1 = CGI::Cookie.new("name" => "leihs-user-session", "value" => cookie_token)
      cookie2 = CGI::Cookie.new("name" => "leihs-anti-csrf-token", "value" => X_CSRF_TOKEN)

      cookie_str = "#{cookie1}; #{cookie2}"

      # resp = session_auth_plain_faraday_json_client(cookie_string: "#{cookie1}; #{cookie2}").post("/test-csrf")

      # resp = session_auth_plain_faraday_json_client(cookie_string: "#{cookie1}; #{cookie2}").post("/test-csrf") do |req|
      resp = session_auth_plain_faraday_json_client().post("/test-csrf") do |req|
        req.headers["Content-Type"] = "application/json"
        req.headers["x-csrf-token"] = X_CSRF_TOKEN
        req.headers["Cookie"] = "#{cookie1}; #{cookie2}"
      end

      expect(resp.status).to eq(200)
    end

    it "accesses protected resource with valid session cookie for POST" do
      # block public access
      resp = plain_faraday_json_client.post("/test-csrf")
      expect(resp.status).to eq(404)

      # invalid csrf-token
      resp = session_auth_plain_faraday_json_client().post("/test-csrf") do |req|
        req.headers["Content-Type"] = "application/json"
      end

      expect(resp.status).to eq(404)
      expect(resp.body).to eq({ "status" => "failure",
                                "message" => "CSRF-Token/Session not valid",
                                "detail" => "The anti-csrf-token cookie value is not set." })

      # not identical csrf-token
      csrf_token = "abc-def-ghi"
      cookie = CGI::Cookie.new("name" => "leihs-anti-csrf-token", "value" => X_CSRF_TOKEN)

      resp = session_auth_plain_faraday_json_client().post("/test-csrf") do |req|
        req.headers["Content-Type"] = "application/json"
        req.headers["x-csrf-token"] = "any-different-token"
        req.headers["Cookie"] = "#{cookie}"
      end

      expect(resp.status).to eq(404)
      expect(resp.body).to eq({ "status" => "failure",
                                "message" => "CSRF-Token/Session not valid",
                                "detail" => "The x-csrf-token is not equal to the anti-csrf cookie value." })

      # correct csrf-token
      csrf_token = "abc-def-ghi"
      cookie = CGI::Cookie.new("name" => "leihs-anti-csrf-token", "value" => X_CSRF_TOKEN)

      resp = session_auth_plain_faraday_json_client().post("/test-csrf") do |req|
        req.headers["Content-Type"] = "application/json"
        req.headers["x-csrf-token"] = X_CSRF_TOKEN
        req.headers["Cookie"] = "#{cookie}"
      end

      expect(resp.status).to eq(200)
    end

    it "accesses protected resource with valid session cookie for PUT" do
      # block public access
      resp = plain_faraday_json_client.put("/test-csrf")
      expect(resp.status).to eq(404)

      # invalid csrf-token
      resp = session_auth_plain_faraday_json_client().put("/test-csrf") do |req|
        req.headers["Content-Type"] = "application/json"
      end

      expect(resp.status).to eq(404)
      expect(resp.body).to eq({ "status" => "failure",
                                "message" => "CSRF-Token/Session not valid",
                                "detail" => "The anti-csrf-token cookie value is not set." })

      # not identical csrf-token
      csrf_token = "abc-def-ghi"
      cookie = CGI::Cookie.new("name" => "leihs-anti-csrf-token", "value" => X_CSRF_TOKEN)

      resp = session_auth_plain_faraday_json_client().put("/test-csrf") do |req|
        req.headers["Content-Type"] = "application/json"
        req.headers["x-csrf-token"] = "any-different-token"
        req.headers["Cookie"] = "#{cookie}"
      end

      expect(resp.status).to eq(404)
      expect(resp.body).to eq({ "status" => "failure",
                                "message" => "CSRF-Token/Session not valid",
                                "detail" => "The x-csrf-token is not equal to the anti-csrf cookie value." })

      # correct csrf-token
      csrf_token = "abc-def-ghi"
      cookie = CGI::Cookie.new("name" => "leihs-anti-csrf-token", "value" => X_CSRF_TOKEN)

      resp = session_auth_plain_faraday_json_client().put("/test-csrf") do |req|
        req.headers["Content-Type"] = "application/json"
        req.headers["x-csrf-token"] = X_CSRF_TOKEN
        req.headers["Cookie"] = "#{cookie}"
      end

      expect(resp.status).to eq(200)
    end

    it "accesses protected resource with valid session cookie for DELETE" do
      # block public access
      resp = plain_faraday_json_client.delete("/test-csrf")
      expect(resp.status).to eq(404)

      # invalid csrf-token
      resp = session_auth_plain_faraday_json_client().delete("/test-csrf") do |req|
        req.headers["Content-Type"] = "application/json"
      end

      expect(resp.status).to eq(404)
      expect(resp.body).to eq({ "status" => "failure",
                                "message" => "CSRF-Token/Session not valid",
                                "detail" => "The anti-csrf-token cookie value is not set." })

      # not identical csrf-token
      csrf_token = "abc-def-ghi"
      cookie = CGI::Cookie.new("name" => "leihs-anti-csrf-token", "value" => X_CSRF_TOKEN)

      resp = session_auth_plain_faraday_json_client().delete("/test-csrf") do |req|
        req.headers["Content-Type"] = "application/json"
        req.headers["x-csrf-token"] = "any-different-token"
        req.headers["Cookie"] = "#{cookie}"
      end

      expect(resp.status).to eq(404)
      expect(resp.body).to eq({ "status" => "failure",
                                "message" => "CSRF-Token/Session not valid",
                                "detail" => "The x-csrf-token is not equal to the anti-csrf cookie value." })

      # correct csrf-token
      csrf_token = "abc-def-ghi"
      cookie = CGI::Cookie.new("name" => "leihs-anti-csrf-token", "value" => X_CSRF_TOKEN)

      resp = session_auth_plain_faraday_json_client().delete("/test-csrf") do |req|
        req.headers["Content-Type"] = "application/json"
        req.headers["x-csrf-token"] = X_CSRF_TOKEN
        req.headers["Cookie"] = "#{cookie}"
      end

      expect(resp.status).to eq(200)
    end

    it "accesses protected resource with valid session cookie for GET" do
      # block public access
      resp = plain_faraday_json_client.get("/test-csrf")
      expect(resp.status).to eq(200)

      # ignores invalid csrf-token
      resp = session_auth_plain_faraday_json_client().get("/test-csrf") do |req|
        req.headers["Content-Type"] = "application/json"
      end

      expect(resp.status).to eq(200)

      # ignores not identical csrf-token
      csrf_token = "abc-def-ghi"
      cookie = CGI::Cookie.new("name" => "leihs-anti-csrf-token", "value" => X_CSRF_TOKEN)

      resp = session_auth_plain_faraday_json_client().get("/test-csrf") do |req|
        req.headers["Content-Type"] = "application/json"
        req.headers["x-csrf-token"] = "any-different-token"
        req.headers["Cookie"] = "#{cookie}"
      end

      expect(resp.status).to eq(200)

      # correct csrf-token
      csrf_token = "abc-def-ghi"
      cookie = CGI::Cookie.new("name" => "leihs-anti-csrf-token", "value" => X_CSRF_TOKEN)

      resp = session_auth_plain_faraday_json_client().get("/test-csrf") do |req|
        req.headers["Content-Type"] = "application/json"
        req.headers["x-csrf-token"] = X_CSRF_TOKEN
        req.headers["Cookie"] = "#{cookie}"
      end

      expect(resp.status).to eq(200)
    end

  end
end
