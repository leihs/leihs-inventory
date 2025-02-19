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
  "price" => Numeric
}

feature "Inventory Model Management2" do
  context "when interacting with inventory models in a specific inventory pool2", driver: :selenium_headless do
    include_context :setup_models_api_model
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

        result = http_multipart_client(
          "/inventory/#{pool_id}/option",
          form_data,
          headers: cookie_header
        )
        validate_map_structure(result.body["data"], response)

        expect(result.status).to eq(200)
        expect(result.body["data"]["id"]).to be_present
        expect(result.body["validation"].count).to eq(0)
        option_id = result.body["data"]["id"]

        # fetch option
        result = client.get "/inventory/#{pool_id}/option/#{option_id}"
        expect(result.body.count).to eq(1)
        validate_map_structure(result.body.first, response)

        # update option
        form_data = {
          product: Faker::Commerce.product_name,
          inventory_code: "INV-1001",
          version: "v2",
          price: "222"
        }

        result = http_multipart_client(
          "/inventory/#{pool_id}/option/#{option_id}",
          form_data,
          method: :put,
          headers: cookie_header
        )
        validate_map_structure(result.body.first, response)

        expect(result.status).to eq(200)
        expect(result.body[0]["version"]).to eq("v2")
        expect(result.body[0]["price"]).to eq(222.0)
      end
    end

    context "create option (min)" do
      it "create, fetch & update by form data" do
        # create option
        form_data = {
          product: Faker::Commerce.product_name,
          inventory_code: "O-1001"
        }

        result = http_multipart_client(
          "/inventory/#{pool_id}/option",
          form_data,
          headers: cookie_header
        )
        validate_map_structure(result.body["data"], response)

        expect(result.status).to eq(200)
        expect(result.body["data"]["id"]).to be_present
        expect(result.body["validation"].count).to eq(0)
        option_id = result.body["data"]["id"]

        # fetch option
        result = client.get "/inventory/#{pool_id}/option/#{option_id}"
        expect(result.body.count).to eq(1)
        validate_map_structure(result.body.first, response)

        # update option
        form_data = {
          product: Faker::Commerce.product_name,
          inventory_code: "INV-1001"
        }

        result = http_multipart_client(
          "/inventory/#{pool_id}/option/#{option_id}",
          form_data,
          method: :put,
          headers: cookie_header
        )
        validate_map_structure(result.body.first, response)

        expect(result.status).to eq(200)
      end
    end
  end
end
