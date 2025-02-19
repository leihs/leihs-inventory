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

["group_manager", "customer"].each do |role|
  feature "Inventory option" do
    context "when interacting with inventory  option with role=#{role}", driver: :selenium_headless do
      include_context :setup_models_api_model, role
      include_context :setup_unknown_building_room_supplier
      include_context :generate_session_header

      let(:cookie_header) { @cookie_header }
      let(:client) { plain_faraday_json_client(cookie_header) }
      let(:pool_id) { @inventory_pool.id }

      let(:option_id) { FactoryBot.create(:option, inventory_pool_id: pool_id).id }

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
          expect(result.status).to eq(401)

          # fetch option
          result = client.get "/inventory/#{pool_id}/option/#{option_id}"
          expect(result.status).to eq(401)

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
          expect(result.status).to eq(401)
        end
      end
    end
  end
end
