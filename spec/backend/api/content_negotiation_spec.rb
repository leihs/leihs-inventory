require "spec_helper"
require_relative "_shared"

describe "Content Negotiation" do
  let(:unauthenticated_client) { plain_faraday_json_client }
  let(:pool_id) { "8bd16d45-056d-5590-bc7f-12849f034351" }
  let(:non_existent_model_id) { "2aa391d8-447c-4b28-bc9a-136ebe2db3ef" }

  describe "Unauthenticated scenarios" do
    describe "Scenario 1: GET /models/ with Accept: application/json (unauthenticated)" do
      it "returns 401 with proper error message" do
        resp = get_with_accept(unauthenticated_client, "/inventory/#{pool_id}/models/", "application/json")
        expect(resp.status).to eq(401)
        expect(resp.body["status"]).to eq("failure")
        expect(resp.body["message"]).to eq("Not authenticated")
      end
    end

    describe "Scenario 2: GET /models/ with Accept: */* (unauthenticated)" do
      it "returns SPA (HTML)" do
        resp = get_with_accept(unauthenticated_client, "/inventory/#{pool_id}/models/", "*/*")
        expect(resp.status).to eq(200)
        expect(resp.headers["Content-Type"]).to match(%r{text/html})
      end
    end

    describe "Scenario 3: GET /models/ with Accept: image/* (unauthenticated)" do
      it "returns 401" do
        resp = get_with_accept(unauthenticated_client, "/inventory/#{pool_id}/models/", "image/*")
        expect(resp.status).to eq(401)
        expect(resp.body["status"]).to eq("failure")
        expect(resp.body["message"]).to eq("Not authenticated")
      end
    end

    describe "Scenario 4: GET /models/:id/ with Accept: image/* (non-existent, unauthenticated)" do
      it "returns 401 (correct behavior)" do
        resp = get_with_accept(unauthenticated_client, "/inventory/#{pool_id}/models/#{non_existent_model_id}/",
          "image/*")
        expect(resp.status).to eq(401)
        expect(resp.body["status"]).to eq("failure")
        expect(resp.body["message"]).to eq("Not authenticated")
      end
    end

    describe "Scenario 5: GET /models/:id/ with Accept: */* (non-existent, unauthenticated)" do
      it "returns SPA (HTML)" do
        resp = get_with_accept(unauthenticated_client, "/inventory/#{pool_id}/models/#{non_existent_model_id}/", "*/*")
        expect(resp.status).to eq(200)
        expect(resp.headers["Content-Type"]).to match(%r{text/html})
      end
    end

    describe "Scenario 6: GET /models/:id/ with Accept: application/json (non-existent, unauthenticated)" do
      it "returns 401 (auth check happens first)" do
        resp = get_with_accept(unauthenticated_client, "/inventory/#{pool_id}/models/#{non_existent_model_id}/",
          "application/json")
        expect(resp.status).to eq(401)
        expect(resp.body["status"]).to eq("failure")
        expect(resp.body["message"]).to eq("Not authenticated")
      end
    end
  end

  describe "Authenticated scenarios" do
    before do
      @user = FactoryBot.create(:user, login: "test", password: "password")
      @inventory_pool = FactoryBot.create(:inventory_pool, id: pool_id)
      FactoryBot.create(:direct_access_right, inventory_pool_id: @inventory_pool.id, user_id: @user.id, role: "inventory_manager")
    end

    let(:authenticated_client) do
      cookies, _cookies_str, _token = create_and_login_by(@user)
      session_auth_plain_faraday_json_client(cookies: cookies, headers: {})
    end

    describe "Scenario 7: GET /models/ with Accept: application/json (authenticated)" do
      it "returns JSON (200)" do
        resp = get_with_accept(authenticated_client, "/inventory/#{pool_id}/models/", "application/json")
        expect(resp.status).to eq(200)
        expect(resp.headers["Content-Type"]).to match(%r{application/json})
      end
    end

    describe "Scenario 8: GET /models/ with Accept: */* (authenticated)" do
      it "returns SPA (HTML)" do
        resp = get_with_accept(authenticated_client, "/inventory/#{pool_id}/models/", "*/*")
        expect(resp.status).to eq(200)
        expect(resp.headers["Content-Type"]).to match(%r{text/html})
      end
    end

    describe "Scenario 9: GET /models/ with Accept: image/* (authenticated)" do
      it "returns 406 Not Acceptable" do
        resp = get_with_accept(authenticated_client, "/inventory/#{pool_id}/models/", "image/*")
        expect(resp.status).to eq(406)
        expect(resp.body).to eq("Not Acceptable")
      end
    end
  end

  private

  def get_with_accept(client, url, accept_header)
    client.get url do |req|
      req.headers["Accept"] = accept_header
      req.headers["x-csrf-token"] = X_CSRF_TOKEN
    end
  end
end
