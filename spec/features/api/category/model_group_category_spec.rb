require "spec_helper"
require "pry"
require_relative "../_shared"

feature "Inventory API - Model Group Endpoints" do
  context "when retrieving model groups for a specific inventory pool", driver: :selenium_headless do
    include_context :setup_access_rights

    before :each do
      @user_cookies, @user_cookies_str, @cookie_token = create_and_login_by(@user)
    end

    let(:url) { "/inventory/#{@inventory_pool.id}/model-groups" }
    let(:client) { session_auth_plain_faraday_json_csrf_client(cookies: @user_cookies) }
    let(:resp) { client.get url }
    let(:image_id) { resp.body[0]["id"] }

    context "GET /inventory/model-groups" do
      it "returns all model groups and a 200 status" do
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(1)
      end

      it "returns details for a specific model group with paginated results and a 200 status" do
        resp = client.get "#{url}/#{image_id}"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(1)
        expect(resp.body[0]["id"]).to eq(image_id)
      end
    end
  end
end
