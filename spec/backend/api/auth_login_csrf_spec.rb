require "spec_helper"
require_relative "_shared"

describe "Call swagger-endpoints" do
  context "with accept=text/html" do
    before :each do
      @user = FactoryBot.create(:admin, login: "test", password: "test")
    end

    let(:client) { plain_faraday_json_client }
    let(:cookie) { CGI::Cookie.new("name" => "leihs-anti-csrf-token", "value" => X_CSRF_TOKEN) }

    context "GET /sign-in" do
      it "returns 200 to fetch csrf-token" do
        resp = session_auth_plain_faraday_json_client(headers: {accept: "application/json"}).get("/sign-in")
        expect(resp.status).to eq(404)
      end

      it "returns 200 to fetch login-form containing csrf-token" do
        resp = session_auth_plain_faraday_json_client(headers: {accept: "text/html"}).get("/sign-in")
        expect(resp.status).to eq(200)
        expect(resp.body["csrf-token"]).to be
      end
    end

    context "POST /sign-in data" do
      # TODO: why is this possible?
      it "returns 200 for correct sign-in" do
        resp = session_auth_plain_faraday_json_client.post("/sign-in") do |req|
          req.body = URI.encode_www_form(
            "user" => @user.login,
            "password" => @user.password,
            "return-to" => "/inventory/"
          )

          req.headers["Accept"] = "text/html"
          req.headers["Content-Type"] = "application/x-www-form-urlencoded"
          req.headers["Cookie"] = cookie.to_s
          req.headers["x-csrf-token"] = X_CSRF_TOKEN
        end

        expect(resp.status).to eq(302)
        expect(resp.headers["location"]).to be
      end

      context "Process sign-out" do
        let(:sign_in_response) do
          resp = session_auth_plain_faraday_json_client.post("/sign-in") do |req|
            req.body = URI.encode_www_form(
              "user" => @user.login,
              "password" => @user.password,
              "return-to" => "/inventory/"
            )
            req.headers["Accept"] = "text/html"
            req.headers["Content-Type"] = "application/x-www-form-urlencoded"
            req.headers["Cookie"] = cookie.to_s
            req.headers["x-csrf-token"] = X_CSRF_TOKEN
          end

          expect(resp.status).to eq(302)
          expect(resp.headers["location"]).to be
          expect(resp.headers["set-cookie"]).to be
          resp
        end

        it "returns 200 for correct sign-out" do
          cookie_token = parse_cookie(sign_in_response.headers["set-cookie"])["leihs-user-session"]
          _, cookies_str = generate_csrf_session_data(cookie_token)

          # logout successful
          resp = session_auth_plain_faraday_json_client.post("/sign-out") do |req|
            req.headers["Accept"] = "text/html"
            req.headers["Cookie"] = cookies_str
            req.headers["x-csrf-token"] = X_CSRF_TOKEN
          end
          expect(resp.status).to eq(302)
          expect(resp.headers["location"]).to eq("/inventory/")
        end

        it "returns 40x for incorrect sign-out" do
          cookie_token = parse_cookie(sign_in_response.headers["set-cookie"])["leihs-user-session"]
          _, _ = generate_csrf_session_data(cookie_token)

          # logout fails due missing cookie
          resp = session_auth_plain_faraday_json_client.post("/sign-out") do |req|
            req.headers["Accept"] = "application/json"
            req.headers["x-csrf-token"] = X_CSRF_TOKEN
          end
          expect(resp.status).to eq(404)

          # logout fails due invalid cookie
          resp = session_auth_plain_faraday_json_client.post("/sign-out") do |req|
            req.headers["Accept"] = "text/html"
            req.headers["x-csrf-token"] = X_CSRF_TOKEN
          end
          expect(resp.status).to eq(403)

          _, invalid_cookies_str = generate_csrf_session_data("")
          resp = session_auth_plain_faraday_json_client.post("/sign-out") do |req|
            req.headers["Accept"] = "text/html"
            req.headers["Cookie"] = invalid_cookies_str
          end
          expect(resp.status).to eq(403)
        end
      end
    end
  end
end
