require "spec_helper"
require "pry"
require "#{File.dirname(__FILE__)}/_shared"
require_relative "_shared"

describe "Inventory API Endpoints - Model Links" do
  context "when fetching model links for a specific inventory pool" do
    include_context :setup_models_api, "inventory_manager"

    before :each do
      @user_cookies, @user_cookies_str, @cookie_token = create_and_login_by(@user)
    end

    let(:url) { "/inventory/#{@inventory_pool.id}/entitlement-groups/" }
    let(:client) { session_auth_plain_faraday_json_csrf_client(cookies: @user_cookies) }
    let(:resp) { client.get url }
    let(:model_id) { resp.body[0]["id"] }

    context "GET /inventory/models-compatibles" do
      it "retrieves all compatible models and returns status 200" do
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(2)
      end

      it "retrieves paginated compatible models with status 200" do
        resp = client.get "#{url}?page=1&size=1"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(2)
      end
    end
  end
end
