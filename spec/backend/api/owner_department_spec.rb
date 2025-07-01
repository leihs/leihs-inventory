require "spec_helper"
require "pry"
require "#{File.dirname(__FILE__)}/_shared"

describe "Inventory API Endpoints - Departments and Owners" do
  context "when fetching departments and owners for an inventory pool" do
    include_context :setup_access_rights

    before :each do
      @user_cookies, @user_cookies_str, @cookie_token = create_and_login_by(@user)
    end

    let(:client) { session_auth_plain_faraday_json_csrf_client(cookies: @user_cookies) }

    ["/inventory/45130fae-7bfe-4fe2-bc29-980f15c7dca8/departments/", "/inventory/45130fae-7bfe-4fe2-bc29-980f15c7dca8/owners/"].each do |url|
      context "GET #{url}" do
        it "retrieves all records and returns status 200" do
          resp = client.get url
          expect(resp.status).to eq(200)
          expect(resp.body.count).to eq(4)
        end

        it "retrieves paginated results and returns status 200" do
          resp = client.get "#{url}?page=3&size=1"
          expect(resp.status).to eq(200)
          expect(resp.body["pagination"]["total_rows"]).to eq(4)
          expect(resp.body["data"].count).to eq(1)

          id = resp.body["data"][0]["id"]
          resp = client.get "#{url}#{id}"
          expect(resp.status).to eq(200)
          expect(resp.body.count).to eq(1)
        end
      end
    end
  end
end
