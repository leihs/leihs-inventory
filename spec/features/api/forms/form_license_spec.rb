require "spec_helper"
require "pry"
require_relative "../_shared"

feature "Fetching Fields" do
  context "when searching for fields in a specific inventory pool", driver: :selenium_headless do
    include_context :setup_models_api_license

    let(:client) { plain_faraday_json_client }
    let(:pool_id) { @inventory_pool.id }
    let(:direct_access_right_of_user) { @direct_access_right }

    let(:software_model) { @software_model }
    let(:license_item) { @license_item }

    # TODO:
    # - inventory_manager should fetch all fields
    # - lending_manager a subset of fields in edit-mode only
    # - no fields for group_manager/customer

    it "compare counts of keys concerning filter with status 200" do
      ["inventory_manager", "lending_manager", "group_manager", "customer"].each do |role|
        direct_access_right_of_user.update(role: role)

        url = "/inventory/#{pool_id}/license"
        resp = client.get url

        expect(resp.body["data"]["inventory_pool_id"]).to eq(pool_id)
        expect(resp.body["fields"].size).to eq(29)
        expect(resp.status).to eq(200)
      end
    end

    it "compare counts of keys concerning filter with status 200" do
      ["inventory_manager", "lending_manager", "group_manager", "customer"].each do |role|
        direct_access_right_of_user.update(role: role)

        url = "/inventory/#{pool_id}/models/#{software_model.id}/licenses/#{license_item.id}"
        resp = client.get url

        expect(resp.body["fields"].size).to eq(29)
        expect(resp.status).to eq(200)
      end
    end

    it "compare counts of keys concerning filter with status 200" do
      ["inventory_manager", "lending_manager", "group_manager", "customer"].each do |role|
        direct_access_right_of_user.update(role: role)

        url = "/inventory/#{pool_id}/software/#{software_model.id}"
        resp = client.get url

        expect(resp.body.size).to eq(1)
        expect(resp.status).to eq(200)
      end
    end
  end
end
