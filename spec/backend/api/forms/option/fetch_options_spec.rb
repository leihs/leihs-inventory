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
      include_context :generate_session_header

      let(:cookie_header) { @cookie_header }
      let(:client) { plain_faraday_json_client(cookie_header) }
      let(:pool_id) { @inventory_pool.id }

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

      context "delete option" do
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
      end
    end
  end
end
