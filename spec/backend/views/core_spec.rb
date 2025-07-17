require "spec_helper"
require "pry"
require_relative "../api/_shared"

describe "Request " do
  context " with accept=text/html" do
    before :each do
      @user, @user_cookies, @user_cookies_str, @cookie_token = create_and_login(:admin)
    end

    let(:client) { session_auth_plain_faraday_json_csrf_client(cookies: @user_cookies) }

    context "against public endpoint /inventory/status/" do
      scenario "status-check for cider" do
        resp = client.get "/inventory/status/"
        expect(resp.status).to be == 200
      end
    end
  end

  context "with accept=application/json" do
    let :http_client do
      plain_faraday_json_client
    end

    let :prepare_http_client do
      http_client.headers["Accept"] = "application/json"
    end

    before :each do
      prepare_http_client
    end

    context "against /" do
      scenario "json response is correct" do
        resp = http_client.get "/"
        expect(resp.status).to be == 200
        expect(resp.body).to include("Overview _> go to")
      end
    end

    context "against /inventory/status/" do
      scenario "status-check for cider" do
        resp = http_client.get "/inventory/status/"
        expect(resp.status).to be == 403
      end
    end
  end
end
