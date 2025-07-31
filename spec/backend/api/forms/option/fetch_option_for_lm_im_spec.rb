require "spec_helper"
require "pry"
require_relative "../../_shared"
require "faker"
require_relative "../_common"

response = {
  "id" => String,
  "inventory_code" => String,
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
            price: 999999.99,
            inventory_code: "O-1001"
          }

          resp = json_client_post(
            "/inventory/#{pool_id}/options/",
            body: form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(200)
          expect(validate_map_structure(resp.body, response)).to eq(true)
          expect(resp.body["id"]).to be_present
          option_id = resp.body["id"]

          # fetch option
          resp = client.get "/inventory/#{pool_id}/options/#{option_id}"
          expect(resp.status).to eq(200)
          expect(validate_map_structure(resp.body, response)).to eq(true)

          # update option
          form_data = {
            product: Faker::Commerce.product_name,
            inventory_code: "INV-1001",
            version: "v2",
            price: 0
          }

          resp = json_client_put(
            "/inventory/#{pool_id}/options/#{option_id}",
            body: form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(200)
          expect(validate_map_structure(resp.body, response)).to eq(true)
          expect(resp.body["version"]).to eq("v2")
          expect(resp.body["price"]).to eq(0)
        end

        it "rejects requests with negative prices" do
          # create option
          form_data = {
            product: Faker::Commerce.product_name,
            version: "v1",
            price: 1,
            inventory_code: "O-1001"
          }

          resp = json_client_post(
            "/inventory/#{pool_id}/options/",
            body: form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(200)
          option_id = resp.body["id"]

          # update option
          form_data["price"] = -5
          resp = json_client_put(
            "/inventory/#{pool_id}/options/#{option_id}",
            body: form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(422)

          form_data["price"] = 2
          resp = json_client_put(
            "/inventory/#{pool_id}/options/#{option_id}",
            body: form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(200)
          expect(resp.body["price"]).to eq(2)
        end
      end

      context "fetch options" do
        before :each do
          15.times do
            FactoryBot.create(:option, inventory_pool_id: pool_id)
          end
        end

        it "with pagination" do
          resp = client.get "/inventory/#{pool_id}/options/?page=1&per_page=10"
          expect(resp.status).to eq(200)
          expect(resp.body["data"].count).to be 10
          expect(resp.body["pagination"]["page"]).to eq(1)
          expect(resp.body["pagination"]["size"]).to eq(10)
          expect(resp.body["pagination"]["total_rows"]).to eq(15)
        end

        it "without pagination" do
          resp = client.get "/inventory/#{pool_id}/options/"
          expect(resp.status).to eq(200)
          expect(resp.body.count).to be 15
        end
      end

      context "create option (min)" do
        it "create, fetch & update by form data" do
          # create option
          form_data = {
            product: Faker::Commerce.product_name,
            inventory_code: "O-1001"
          }

          resp = json_client_post(
            "/inventory/#{pool_id}/options/",
            body: form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(200)
          expect(validate_map_structure(resp.body, response)).to eq(true)
          expect(resp.body["id"]).to be_present
          option_id = resp.body["id"]

          # fetch option
          resp = client.get "/inventory/#{pool_id}/options/#{option_id}"
          expect(resp.status).to eq(200)
          expect(validate_map_structure(resp.body, response)).to eq(true)

          # update option
          form_data = {
            product: Faker::Commerce.product_name,
            inventory_code: "INV-1001"
          }

          resp = json_client_put(
            "/inventory/#{pool_id}/options/#{option_id}",
            body: form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(200)
          expect(validate_map_structure(resp.body, response)).to eq(true)
        end

        it "create & delete by form data" do
          # create option
          form_data = {
            product: Faker::Commerce.product_name,
            inventory_code: "O-1001"
          }

          resp = json_client_post(
            "/inventory/#{pool_id}/options/",
            body: form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(200)
          expect(validate_map_structure(resp.body, response)).to eq(true)
          expect(resp.body["id"]).to be_present
          option_id = resp.body["id"]

          # delete option
          resp = client.delete "/inventory/#{pool_id}/options/#{option_id}"
          expect(validate_map_structure(resp.body, response)).to eq(true)
        end

        it "block duplicate inventory_code" do
          # create option
          form_data = {
            product: Faker::Commerce.product_name,
            inventory_code: "O-1001"
          }

          resp = json_client_post(
            "/inventory/#{pool_id}/options/",
            body: form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(200)

          resp = json_client_post(
            "/inventory/#{pool_id}/options/",
            body: form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(400)
        end
      end
    end
  end
end
