require "spec_helper"
require "pry"

feature "Call swagger-endpoints" do
  context "with accept=text/html", driver: :selenium_headless do
    before :each do
      @user = FactoryBot.create(:user, login: "test-user")
      @create_token_url = "/inventory/token/"
      @protected_url = "/inventory/token/protected"
    end

    let :client do
      plain_faraday_client
    end

    it 'returns id of created request' do
      resp = client.post @create_token_url do |req|
        req.body = {
          description: "string",
          scopes: {
            read: true,
            write: true,
            admin_read: true,
            admin_write: true
          }
        }.to_json
        req.headers["Content-Type"] = "application/json"
      end
# binding.pry
      expect(resp.status).to be == 401
    end

    it 'returns id of created request2' do
      resp = basic_auth_plain_faraday_json_client("abc", "def").post(@create_token_url) do |req|
        req.body = {
          description: "string",
          scopes: {
            read: true,
            write: true,
            admin_read: true,
            admin_write: true
          }
        }.to_json
        req.headers["Content-Type"] = "application/json"

      end
      expect(resp.status).to be == 401
    end

    it 'returns id of created request2' do
      resp = basic_auth_plain_faraday_json_client(@user.login, @user.password).post(@create_token_url) do |req|
        req.body = {
          description: "string",
          scopes: {
            read: true,
            write: true,
            admin_read: true,
            admin_write: true
          }
        }.to_json
        req.headers["Content-Type"] = "application/json"

      end
      expect(resp.status).to be == 200
    end

    it 'returns id of created request3' do
      resp = plain_faraday_json_client.get(@protected_url)
      expect(resp.status).to be == 401

      resp = basic_auth_plain_faraday_json_client(@user.login, @user.password).post(@create_token_url) do |req|
        req.body = {
          description: "string",
          scopes: {
            read: true,
            write: true,
            admin_read: true,
            admin_write: true
          }
        }.to_json
        req.headers["Content-Type"] = "application/json"
        end
      # binding.pry


      expect(resp.status).to be == 200
      expect(resp.body["token"]).to be

      token= resp.body["token"]

      puts "token: #{token}"


      # sleep 0.1

      # cookie = parse_cookie(resp.headers["set-cookie"])
      # cookie_token = cookie["leihs-user-session"]
      # cookie = CGI::Cookie.new("name" => "leihs-user-session",
      #                          "value" => cookie_token)

      # resp = session_auth_plain_faraday_json_client(cookie.to_s).get(@protected_url) do |req|

      resp = wtoken_header_plain_faraday_json_client_get(token, @protected_url)
      # resp = wtoken_header_plain_faraday_json_client_get(token, @protected_url, headers: { "Accept" => "text/html" })

      # binding.pry
      # resp = wtoken_header_plain_faraday_json_client_get(token, @protected_url) do |req|
      # resp = wtoken_header_plain_faraday_json_client(@protected_url) do |req|
      #   req.headers["Content-Type"] = "application/json"
      #   req.headers["Authorization"] = "Token #{token}"
      # end

# binding.pry

      expect(resp.status).to be == 200

      # expect(resp.body["token"]["scopes"].values).to all(eq(true))  # Check that all scope values are false


      # expect(resp.status).to be == 302
      # expect(resp.headers["location"]).to eq "/sign-in?return-to=%2Finventory"

    end

    it 'returns id of created request3, no creds' do
      resp = plain_faraday_json_client.get(@protected_url)
      expect(resp.status).to be == 401

      resp = basic_auth_plain_faraday_json_client(@user.login, @user.password).post(@create_token_url) do |req|
        req.body = {
          description: "string",
          scopes: {
            read: false,
            write: false,
            admin_read: false,
            admin_write: false
          }
        }.to_json
        # req.headers["Accept"] = "application/json"
        req.headers["Content-Type"] = "application/json"

      end
      # binding.pry


      expect(resp.status).to be == 200
      expect(resp.body["token"]).to be

      token= resp.body["token"]

      puts "token: #{token}"


      # sleep 0.1

      # cookie = parse_cookie(resp.headers["set-cookie"])
      # cookie_token = cookie["leihs-user-session"]
      # cookie = CGI::Cookie.new("name" => "leihs-user-session",
      #                          "value" => cookie_token)

      # resp = session_auth_plain_faraday_json_client(cookie.to_s).get(@protected_url) do |req|

      resp = wtoken_header_plain_faraday_json_client_get(token, @protected_url)
      # resp = wtoken_header_plain_faraday_json_client_get(token, @protected_url, headers: { "Accept" => "text/html" })

      # binding.pry
      # resp = wtoken_header_plain_faraday_json_client_get(token, @protected_url) do |req|
      # resp = wtoken_header_plain_faraday_json_client(@protected_url) do |req|
      #   req.headers["Content-Type"] = "application/json"
      #   req.headers["Authorization"] = "Token #{token}"
      # end

# binding.pry

      expect(resp.status).to be == 200
      expect(resp.body["token"]["scopes"].values).to all(eq(false))  # Check that all scope values are false


      # expect(resp.status).to be == 302
      # expect(resp.headers["location"]).to eq "/sign-in?return-to=%2Finventory"

    end


    it 'returns id of created request3' do
      resp = plain_faraday_json_client.get(@protected_url)
      expect(resp.status).to be == 401

      resp = basic_auth_plain_faraday_json_client(@user.login, @user.password).post(@create_token_url) do |req|
        req.body = {
          description: "string",
          scopes: {
            read: true,
            write: true,
            admin_read: true,
            admin_write: true
          }
        }.to_json
        req.headers["Content-Type"] = "application/json"
        end
      # binding.pry


      expect(resp.status).to be == 200
      expect(resp.body["token"]).to be

      token= resp.body["token"]

      puts "token: #{token}"


      # sleep 0.1

      # cookie = parse_cookie(resp.headers["set-cookie"])
      # cookie_token = cookie["leihs-user-session"]
      # cookie = CGI::Cookie.new("name" => "leihs-user-session",
      #                          "value" => cookie_token)

      # resp = session_auth_plain_faraday_json_client(cookie.to_s).get(@protected_url) do |req|

      # resp = wtoken_header_plain_faraday_json_client_get(token, @protected_url)
      resp = wtoken_header_plain_faraday_json_client_get(token, @protected_url, headers: { "Accept" => "text/html" })

      # binding.pry
      # resp = wtoken_header_plain_faraday_json_client_get(token, @protected_url) do |req|
      # resp = wtoken_header_plain_faraday_json_client(@protected_url) do |req|
      #   req.headers["Content-Type"] = "application/json"
      #   req.headers["Authorization"] = "Token #{token}"
      # end

      # binding.pry
      # expect(resp.status).to be == 200

      expect(resp.status).to be == 302
      expect(resp.headers["location"]).to eq "/sign-in?return-to=%2Finventory"

    end



  end
end
