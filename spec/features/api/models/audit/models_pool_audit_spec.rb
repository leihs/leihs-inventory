require "spec_helper"
require "pry"
require_relative "../../_shared"
require_relative "../../_audit_validator"

feature "Swagger Inventory Endpoints - Models of pool with audits" do
  context "when managing models within an inventory pool", driver: :selenium_headless do
    include_context :setup_models_min_api

    before :each do
      @user_cookies, @user_cookies_str, @cookie_token = create_and_login_by(@user)
    end

    let(:client) {
      headers = {"accept" => "application/json", "x-csrf-token" => X_CSRF_TOKEN}
      session_auth_plain_faraday_json_client(cookies: @user_cookies, headers: headers)
    }
    let(:inventory_pool_id) { @inventory_pool.id }
    let(:url) { "/inventory/#{inventory_pool_id}/models" }

    context "CRUD operations for model management" do
      let(:category) { FactoryBot.create(:category) }
      let(:response) {
        client.post(url) do |req|
          req.body = {
            product: Faker::Lorem.word,
            category_ids: [category.id],
            version: "1",
            type: "Model",
            is_package: false
          }.to_json
          req.headers["Content-Type"] = "application/json"
          req.headers["Accept"] = "application/json"
          req.headers["x-csrf-token"] = X_CSRF_TOKEN
          req.headers["Cookie"] = @user_cookies.map(&:to_s).join("; ")
        end
      }

      it "creates a model and returns status 200" do
        expect(response.status).to eq(200)
        expect(response.body[0]["id"]).to be_present
        expect(response.body.count).to eq(1)
        expect_audit_entries_count(1, 8, 1)
      end

      it "updates a model and returns status 200" do
        model_id = response.body[0]["id"]

        updated_response = client.put("#{url}/#{model_id}") do |req|
          req.body = {
            product: "Example Model 2",
            type: "Model",
            manufacturer: "Example Manufacturer after update"
          }.to_json
          req.headers["Content-Type"] = "application/json"
          req.headers["Accept"] = "application/json"
          req.headers["x-csrf-token"] = X_CSRF_TOKEN
          req.headers["Cookie"] = @user_cookies.map(&:to_s).join("; ")
        end

        expect(updated_response.status).to eq(200)
        expect(updated_response.body[0]["id"]).to eq(model_id)
        expect_audit_entries_count(2, 9, 2)
      end

      it "deletes a model and verifies it is removed" do
        model_id = response.body[0]["id"]
        delete_response = client.delete("#{url}/#{model_id}")

        expect(delete_response.status).to eq(200)
        expect(delete_response.body[0]["id"]).to eq(model_id)
        expect_audit_entries_count(2, 9, 2)

        check_deleted_response = client.get("#{url}/#{model_id}")
        expect(check_deleted_response.status).to eq(200)
        expect(check_deleted_response.body.count).to eq(0)
        expect_audit_entries_count(2, 9, 2)
      end
    end
  end
end
