require "spec_helper"
require "pry"
require_relative "../../_shared"

describe "Fetching Fields" do
  context "when searching for fields in a specific inventory pool" do
    include_context :setup_models_api_license
    include_context :generate_session_header

    let(:pool_id) { @inventory_pool.id }
    let(:direct_access_right_of_user) { @direct_access_right }
    let(:cookie_header) { @cookie_header }
    let(:client) { plain_faraday_json_client(cookie_header) }

    let(:software_model) { @software_model }
    let(:license_item) { @license_item }

    # TODO:
    # - inventory_manager should fetch all fields
    # - lending_manager a subset of fields in edit-mode only
    # - no fields for group_manager/customer

    it "compare counts of keys concerning filter with status 200" do
      # ["inventory_manager", "lending_manager", "group_manager", "customer"].each do |role|
      [["inventory_manager", 29], ["lending_manager", 11]].each do |role, expected_count|
        direct_access_right_of_user.update(role: role)

        url = "/inventory/#{pool_id}/license"
        resp = client.get url

        expect(resp.body["data"]["inventory_pool_id"]).to eq(pool_id)
        puts "role #{role} has #{resp.body["fields"].size} fields"
        expect(resp.body["fields"].size).to eq(expected_count)
        expect(resp.status).to eq(200)
      end
    end

    it "compare counts of keys concerning filter with status 200" do
      # ["inventory_manager", "lending_manager", "group_manager", "customer"].each do |role|
      [["inventory_manager", 29], ["lending_manager", 11]].each do |role, expected_count|
        direct_access_right_of_user.update(role: role)

        url = "/inventory/#{pool_id}/models/#{software_model.id}/licenses/#{license_item.id}"
        resp = client.get url

        expect(resp.body["fields"].size).to eq(expected_count)
        expect(resp.status).to eq(200)
      end
    end

    it "compare counts of keys concerning filter with status 200" do
      # ["inventory_manager", "lending_manager", "group_manager", "customer"].each do |role|
      [["inventory_manager", 1], ["lending_manager", 1]].each do |role, expected_count|
        direct_access_right_of_user.update(role: role)

        url = "/inventory/#{pool_id}/software/#{software_model.id}"
        resp = client.get url

        expect(resp.body.size).to eq(expected_count)
        expect(resp.status).to eq(200)
      end
    end
  end
end
