require "spec_helper"
require "pry"
require_relative "../api/_shared"

describe "Request " do
  context " with accept=text/html" do
    before :each do
      @user, @user_cookies, @user_cookies_str, @cookie_token = create_and_login(:admin)
    end

    let(:client) { session_auth_plain_faraday_json_csrf_client(cookies: @user_cookies) }

    context "against public endpoint /inventory/status" do
      scenario "status-check for cider" do
        resp = client.get "/inventory/status"
        expect(resp.status).to be == 200
        expect(resp.body.keys).to eq(["memory", "db-pool", "health-checks"])
      end
    end
  end

  context "requests to public endpoints" do
    let :json_client do
      plain_faraday_json_client
    end

    let :http_client do
      plain_faraday_html_client
    end

    context "against /" do
      scenario "json response is correct" do
        resp = http_client.get "/"
        expect(resp.status).to be == 200
        expect(resp.body).to include("Overview _> go to")
      end

      scenario "json response is correct" do
        resp = json_client.get "/"
        expect(resp.status).to be == 404
      end
    end

    context "http-request against /inventory/status" do
      scenario "valid status-check for cider" do
        resp = http_client.get "/inventory/status"
        expect_spa_content(resp, 200)
      end
    end
  end
end
