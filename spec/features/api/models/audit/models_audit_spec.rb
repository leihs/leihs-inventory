require "spec_helper"
require "pry"
require_relative "../../_shared"
require_relative "../../_audit_validator"

shared_context :setup_model_creation do
  before :each do
    @category = FactoryBot.create(:category)
    @response = json_client_post(url, body: {
      product: Faker::Lorem.word,
      version: "1",
      type: "Model",
      is_package: false
    })
  end
end

feature "Swagger Inventory Endpoints - Models with audits" do
  context "when managing models within an inventory pool", driver: :selenium_headless do
    include_context :setup_models_min_api

    let(:client) { plain_faraday_json_client }
    let(:inventory_pool_id) { @inventory_pool.id }
    let(:url) { "/inventory/models" }

    context "CRUD operations for model management" do
      include_context :setup_model_creation

      it "creates a model and returns status 200" do
        expect(@response.status).to eq(200)
        expect(@response.body[0]["id"]).to be_present
        expect(@response.body.count).to eq(1)
        expect_audit_entries_count(1, 6, 1)
      end

      it "updates a model and returns status 200" do
        model_id = @response.body[0]["id"]

        updated_response = json_client_put("#{url}/#{model_id}", body: {
          product: "Example Model 2",
          type: "Model",
          manufacturer: "Example Manufacturer after update"
        })

        expect(updated_response.status).to eq(200)
        expect(updated_response.body[0]["id"]).to eq(model_id)
        expect_audit_entries_count(2, 7, 2)
      end

      it "deletes a model and verifies it is removed" do
        model_id = @response.body[0]["id"]
        delete_response = json_client_delete("#{url}/#{model_id}")

        expect(delete_response.status).to eq(200)
        expect(delete_response.body[0]["id"]).to eq(model_id)
        expect_audit_entries_count(2, 7, 2)

        check_deleted_response = json_client_get("#{url}/#{model_id}")
        expect(check_deleted_response.status).to eq(200)
        expect(check_deleted_response.body.count).to eq(0)
        expect_audit_entries_count(2, 7, 2)
      end
    end
  end
end