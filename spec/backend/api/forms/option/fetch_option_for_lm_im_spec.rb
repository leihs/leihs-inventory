require "spec_helper"
require "pry"
require_relative "../../_shared"
require "faker"
require_relative "../_common"

response = {
  "id" => String,
  "inventory_pool_id" => String,
  "inventory_code" => String, # TODO: remove
  "manufacturer" => [NilClass, String], # TODO: remove
  "product" => String,
  "version" => [NilClass, String],
  "name" => String,
  "price" => [NilClass, Numeric]
}

["inventory_manager", "lending_manager"].each do |role|
  describe "Inventory option" do
    context "when interacting with inventory option with role=#{role}" do
      include_context :setup_models_api_model, role
      include_context :setup_unknown_building_room_supplier
      include_context :generate_session_header

      let(:cookie_header) { @cookie_header }
      let(:client) { plain_faraday_json_client(cookie_header) }
      let(:pool_id) { @inventory_pool.id }

      let(:software_model) { @software_model }
      let(:license_item) { @license_item }
      let(:model_id) { @software_model.id }

      context "create option" do
        it "create, fetch & update by form data" do
          # create option
          form_data = {
            product: Faker::Commerce.product_name,
            version: "v1",
            price: "111",
            inventory_code: "O-1001"
          }

          resp = http_multipart_client(
            "/inventory/#{pool_id}/option",
            form_data,
            headers: cookie_header
          )
          expect(validate_map_structure(resp.body["data"], response)).to eq(true)

          expect(resp.status).to eq(200)
          expect(resp.body["data"]["id"]).to be_present
          expect(resp.body["validation"].count).to eq(0)
          option_id = resp.body["data"]["id"]

          # fetch option
          resp = client.get "/inventory/#{pool_id}/option/#{option_id}"
          expect(resp.body.count).to eq(1)
          expect(validate_map_structure(resp.body.first, response)).to eq(true)

          # update option
          form_data = {
            product: Faker::Commerce.product_name,
            inventory_code: "INV-1001",
            version: "v2",
            price: "222"
          }

          resp = http_multipart_client(
            "/inventory/#{pool_id}/option/#{option_id}",
            form_data,
            method: :put,
            headers: cookie_header
          )
          expect(validate_map_structure(resp.body.first, response)).to eq(true)

          expect(resp.status).to eq(200)
          expect(resp.body[0]["version"]).to eq("v2")
          expect(resp.body[0]["price"]).to eq(222.0)
        end
      end

      context "create option (min)" do
        it "create, fetch & update by form data" do
          # create option
          form_data = {
            product: Faker::Commerce.product_name,
            inventory_code: "O-1001"
          }

          resp = http_multipart_client(
            "/inventory/#{pool_id}/option",
            form_data,
            headers: cookie_header
          )
          expect(validate_map_structure(resp.body["data"], response)).to eq(true)

          expect(resp.status).to eq(200)
          expect(resp.body["data"]["id"]).to be_present
          expect(resp.body["validation"].count).to eq(0)
          option_id = resp.body["data"]["id"]

          # fetch option
          resp = client.get "/inventory/#{pool_id}/option/#{option_id}"
          expect(resp.body.count).to eq(1)
          expect(validate_map_structure(resp.body.first, response)).to eq(true)

          # update option
          form_data = {
            product: Faker::Commerce.product_name,
            inventory_code: "INV-1001"
          }

          resp = http_multipart_client(
            "/inventory/#{pool_id}/option/#{option_id}",
            form_data,
            method: :put,
            headers: cookie_header
          )
          expect(validate_map_structure(resp.body.first, response)).to eq(true)

          expect(resp.status).to eq(200)
        end
      end
    end
  end
end
