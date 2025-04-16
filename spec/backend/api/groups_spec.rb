require "spec_helper"
require "pry"
require "#{File.dirname(__FILE__)}/_shared"

describe "Inventory API Endpoints - Group Links" do
  context "when fetching group links for a specific inventory pool" do
    include_context :setup_access_rights

    before :each do
      @user_cookies, @user_cookies_str, @cookie_token = create_and_login_by(@user)
    end

    let(:url) { "/inventory/#{@inventory_pool.id}/groups" }
    let(:client) { session_auth_plain_faraday_json_csrf_client(cookies: @user_cookies) }
    let(:resp) { client.get url }
    let(:group_id) { resp.body[0]["id"] }

    context "GET /inventory/groups" do
      it "retrieves all groups and returns status 200" do
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(1)
      end

      it "retrieves paginated group results and returns status 200" do
        resp = client.get "#{url}/#{group_id}"

        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(1)
        expect(resp.body[0]["id"]).to eq(group_id)
      end
    end
  end
end
