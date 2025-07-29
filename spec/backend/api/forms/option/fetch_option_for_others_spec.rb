require "spec_helper"
require "pry"
require_relative "../../_shared"
require "faker"
require_relative "../_common"

["group_manager", "customer"].each do |role|
  describe "Inventory option" do
    context "when interacting with inventory option with role=#{role}" do
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
            price: 11,
            inventory_code: "O-1001"
          }

          resp = json_client_post(
            "/inventory/#{pool_id}/options/",
            body: form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(404)

          # fetch option
          resp = client.get "/inventory/#{pool_id}/options/#{option_id}"
          expect(resp.status).to eq(404)

          # update option
          form_data = {
            product: Faker::Commerce.product_name,
            inventory_code: "INV-1001",
            version: "v2",
            price: 22
          }

          resp = json_client_put(
            "/inventory/#{pool_id}/options/#{option_id}",
            body: form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(404)

          resp = json_client_delete(
            "/inventory/#{pool_id}/options/#{option_id}",
            headers: cookie_header
          )
          expect(resp.status).to eq(404)
        end
      end
    end
  end
end
