require "spec_helper"
require "pry"
require "#{File.dirname(__FILE__)}/_shared"

describe "Inventory API Endpoints - Supplier" do
  context "when fetching suppliers for a specific inventory pool" do
    include_context :setup_access_rights

    before :each do
      @user_cookies, @user_cookies_str, @cookie_token = create_and_login_by(@user)
    end

    let(:pool_id) { @inventory_pool.id }
    let(:url) { "/inventory/#{pool_id}/suppliers/" }
    let(:client) { session_auth_plain_faraday_json_csrf_client(cookies: @user_cookies) }
    let(:resp) { client.get url }
    let(:supplier_id) { resp.body[0]["id"] }

    context "GET /inventory/supplier" do
      it "retrieves all suppliers and returns status 200" do
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(1)
      end

      it "retrieves paginated supplier results and returns status 200" do
        resp = client.get "#{url}?page=1&size=1"
        expect(resp.status).to eq(200)
        expect(resp.body["data"].count).to eq(1)
        expect(resp.body["pagination"]["total_rows"]).to eq(1)
      end

      it "retrieves specific supplier details by ID and returns status 200" do
        resp = client.get "#{url}#{supplier_id}"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(1)
        expect(resp.body[0]["id"]).to eq(supplier_id)
      end
    end
  end
end
