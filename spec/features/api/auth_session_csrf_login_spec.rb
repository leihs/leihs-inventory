require "spec_helper"

feature "Call swagger-endpoints" do
  context "with accept=text/html", driver: :selenium_headless do
    before :each do
      @user = FactoryBot.create(:user, login: "test", password: "test")
    end

    let(:client) { plain_faraday_json_client }


    # TODO:
    it "accesses protected resource with valid session cookie" do
      resp = plain_faraday_json_client.get("/inventory/session/protected")
      expect(resp.status).to eq(403)



      # redirect after invalid login
      resp = common_plain_faraday_login_client(:post, "/sign-in",  body: {
        "user" => @user.login,
        "password" => @user.password,
        "csrf-token" => X_CSRF_TOKEN,
        "return-to" => "/inventory/models"
      })

      expect(resp.status).to eq(302)
      expect(resp.headers["location"]).to eq("/sign-in?return-to=%2Finventory&message=CSRF-Token/Session not valid")



      # redirect after correct login
      cookie = CGI::Cookie.new("name" => "leihs-anti-csrf-token", "value" => X_CSRF_TOKEN)
      resp = common_plain_faraday_login_client(:post, "/sign-in",  body: {
        "user" => @user.login,
        "password" => @user.password,
        "csrf-token" => X_CSRF_TOKEN,
                     "return-to" => "/inventory/models"
      },
        headers: {"Cookie" => cookie.to_s}
      )

      expect(resp.status).to eq(302)
      expect(resp.headers["location"]).to eq("/inventory/8bd16d45-056d-5590-bc7f-12849f034351/models")

      resp = session_auth_plain_faraday_json_client().get("/test-csrf")
      expect(resp.status).to eq(200)

      cookie = CGI::Cookie.new("name" => "leihs-anti-csrf-token", "value" => X_CSRF_TOKEN)

      resp = session_auth_plain_faraday_json_client().put("/test-csrf") do |req|
        req.headers["Content-Type"] = "application/json"
        req.headers["x-csrf-token"] = X_CSRF_TOKEN
        req.headers["Cookie"] = "#{cookie}"
      end
      expect(resp.status).to eq(200)

      resp = session_auth_plain_faraday_json_client().put("/test-csrf") do |req|
        req.headers["Content-Type"] = "application/json"
        req.headers["x-csrf-token"] = X_CSRF_TOKEN
      end

      expect(resp.status).to eq(404)
      expect(resp.body["detail"]).to eq("The anti-csrf-token cookie value is not set.")

      resp = session_auth_plain_faraday_json_client().put("/test-csrf") do |req|
        req.headers["Content-Type"] = "application/json"
        req.headers["Cookie"] = "#{cookie}"
      end
      expect(resp.body["detail"]).to eq("The x-csrf-token has not been send!")
      expect(resp.status).to eq(404)

      resp = session_auth_plain_faraday_json_client().put("/test-csrf") do |req|
        req.headers["Content-Type"] = "application/json"
      end
      expect(resp.status).to eq(404)
      expect(resp.body["detail"]).to eq("The anti-csrf-token cookie value is not set.")


      resp = session_auth_plain_faraday_json_client().put("/test-csrf") do |req|
        req.headers["Content-Type"] = "application/json"
        req.headers["x-csrf-token"] = "not-correct-token"
        req.headers["Cookie"] = "#{cookie}"
      end
      expect(resp.status).to eq(404)
      expect(resp.body["detail"]).to eq("The x-csrf-token is not equal to the anti-csrf cookie value.")




      resp = session_auth_plain_faraday_json_client().post("/test-csrf") do |req|
        req.headers["Content-Type"] = "application/json"
        req.headers["x-csrf-token"] = X_CSRF_TOKEN
        req.headers["Cookie"] = "#{cookie}"
      end
      expect(resp.status).to eq(200)

      resp = session_auth_plain_faraday_json_client().delete("/test-csrf") do |req|
        req.headers["Content-Type"] = "application/json"
        req.headers["x-csrf-token"] = X_CSRF_TOKEN
        req.headers["Cookie"] = "#{cookie}"
      end
      expect(resp.status).to eq(200)
    end



  end
end
