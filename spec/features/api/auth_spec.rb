require "spec_helper"
require "pry"

feature "Call swagger-endpoints" do
  context "with accept=text/html", driver: :selenium_headless do
    before :each do
      login = "test-user"
      @user = FactoryBot.create(:user, login: login)
    end

    let :client do
      plain_faraday_client
    end

    it 'returns id of created request' do
      resp = client.get "/inventory/login"
      expect(resp.status).to be == 403
    end

    it 'returns id of created request2' do
      resp = basic_auth_plain_faraday_json_client("abc", "def").get("/inventory/login")
      expect(resp.status).to be == 403
    end

    it 'returns id of created request2' do
      resp = basic_auth_plain_faraday_json_client(@user.login, @user.password).get("/inventory/login")
      expect(resp.status).to be == 200
    end

    it 'returns id of created request3' do
      puts "user.login: #{@user.login}"
      puts "user.password: #{@user.password}"

      resp = plain_faraday_json_client.get("/inventory/session/protected")
      expect(resp.status).to be == 403

      resp = basic_auth_plain_faraday_json_client(@user.login, @user.password).get("/inventory/login")
      expect(resp.status).to be == 200

      cookie = parse_cookie(resp.headers["set-cookie"])
      cookie_token = cookie["leihs-user-session"]
      puts "leihs-user-session token : #{cookie}"

      cookie = CGI::Cookie.new("name" => "leihs-user-session",
                               "value" => cookie_token)

      resp = session_auth_plain_faraday_json_client(cookie.to_s).get("/inventory/session/protected") do |req|
        req.headers["Content-Type"] = "application/json"
        req.headers["Cookie"] = cookie.to_s
      end

      expect(resp.status).to be == 200
    end
  end
end
