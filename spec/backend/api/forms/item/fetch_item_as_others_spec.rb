require "spec_helper"
require "pry"
require_relative "../../_shared"
require "faker"

describe "Inventory Item" do
  ["group_manager", "customer"].each do |role|
    context "when interacting with inventory item with role=#{role}" do
      include_context :setup_models_api_model, role
      include_context :generate_session_header

      let(:pool_id) { @inventory_pool.id }
      let(:cookie_header) { @cookie_header }
      let(:client) { plain_faraday_json_client(cookie_header) }

      let(:form_categories) { @form_categories }
      let(:form_compatible_models) { @form_compatible_models }

      let(:path_test_pdf) { File.expand_path("spec/files/test.pdf", Dir.pwd) }
      let(:path_test_txt) { File.expand_path("spec/files/text-file.txt", Dir.pwd) }

      let(:fake_item) {
        Item.first
      }

      before do
        resp = client.get "/inventory/#{pool_id}/entitlement-groups"
        @form_entitlement_groups = resp.body
        raise "Failed to fetch entitlement groups" unless resp.status == 200

        resp = client.get "/inventory/owners"
        @form_owners = resp.body
        raise "Failed to fetch manufacturers" unless resp.status == 200

        resp = client.get "/inventory/buildings"
        @form_buildings = resp.body
        raise "Failed to fetch entitlement groups" unless resp.status == 200

        resp = client.get "/inventory/rooms?building_id=#{@form_buildings[0]["id"]}"
        @form_rooms = resp.body
        raise "Failed to fetch entitlement groups" unless resp.status == 200

        resp = client.get "/inventory/manufacturers?type=Model&in-detail=true"
        @form_model_names = resp.body
        raise "Failed to fetch compatible models" unless resp.status == 200

        resp = client.get "/inventory/manufacturers?type=Model&in-detail=true&search-term=#{@form_model_names[0]["product"]}"
        @form_model_data = resp.body
        raise "Failed to fetch compatible models" unless resp.status == 200
      end

      context "fetch of form" do
        it "ensures form manufacturer data is fetched" do
          resp = client.get "/inventory/#{pool_id}/item"
          @form_entitlement_groups = resp.body
          expect(resp.status).to be(401)
        end
      end

      context "create item" do
        it "create and modify item with attachments" do
          # create item
          form_data = {
            serial_number: "",
            note: "",
            is_inventory_relevant: "true",
            last_check: "",
            user_name: "",
            invoice_number: "",
            invoice_date: "",
            price: "",
            shelf: "",
            status_note: "",
            inventory_code: "AUS45859",
            retired: "false",
            is_broken: "false",
            is_incomplete: "false",
            is_borrowable: "false",
            room_id: @form_rooms[0]["id"],
            model_id: @form_model_data[0]["id"],
            owner_id: @form_owners[0]["id"],
            properties: {
              electrical_power: "",
              imei_number: "",
              p4u: "",
              reference: "invoice",
              warranty_expiration: "",
              quantity_allocations: []
            }.to_json
          }

          resp = http_multipart_client(
            "/inventory/#{pool_id}/item",
            form_data,
            method: :post,
            headers: cookie_header
          )
          expect(resp.status).to eq(401)

          model_id = fake_item.model_id
          item_id = fake_item.id

          # fetch created item
          resp = client.get "/inventory/#{pool_id}/models/#{model_id}/item/#{item_id}"
          expect(resp.status).to be(401)

          # update item request
          form_data = {
            serial_number: nil,
            note: nil,
            invoice_date: nil,
            invoice_number: nil,
            price: nil,
            shelf: nil,
            inventory_code: "AUS45859",
            retired: "false",
            is_broken: "false",
            is_incomplete: "false",
            is_borrowable: "false",
            status_note: nil,
            room_id: @form_rooms[0]["id"],
            properties: {
              electrical_power: "",
              imei_number: "",
              p4u: "",
              reference: "invoice",
              warranty_expiration: "",
              quantity_allocations: []
            }.to_json
          }.transform_values { |v| v.nil? ? "" : v.to_s }
          resp = http_multipart_client(
            "/inventory/#{pool_id}/models/#{model_id}/item/#{item_id}",
            form_data,
            method: :put,
            headers: cookie_header
          )
          expect(resp.status).to eq(401)

          # fetch created item
          resp = client.get "/inventory/#{pool_id}/models/#{model_id}/item/#{item_id}"
          expect(resp.status).to be(401)
        end
      end
    end
  end
end
