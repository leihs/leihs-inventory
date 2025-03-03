require "spec_helper"
require "pry"
require_relative "../../_shared"
require_relative "../_common"
require "faker"

expected_fields = ["serial_number", "properties_mac_address", "properties_imei_number", "name", "note", "attachments",
  "last_check", "responsible", "invoice_number", "invoice_date", "price", "supplier_id",
  "properties_warranty_expiration", "properties_contract_expiration", "building_id", "room_id",
  "shelf", "inventory_code", "model_id", "retired", "retired_reason", "is_broken", "is_incomplete",
  "is_borrowable", "status_note"]

fetch_response = {
  "properties" => Hash,
  "inventory_code" => String,
  "supplier" => [NilClass, String],
  "owner_id" => String,
  "is_borrowable" => [TrueClass, FalseClass],
  "retired" => [TrueClass, FalseClass],
  "is_inventory_relevant" => [TrueClass, FalseClass],
  "last_check" => [NilClass, String],
  "building_id" => String,
  "shelf" => String,
  "status_note" => String,
  "name" => [NilClass, String],
  "attachments" => Array,
  "invoice_number" => String,
  "supplier_name" => [NilClass, String],
  "is_broken" => [TrueClass, FalseClass],
  "note" => String,
  "updated_at" => String,
  "retired_reason" => [NilClass, String],
  "responsible" => [NilClass, String],
  "invoice_date" => [NilClass, String],
  "product" => Hash,
  "model_id" => String,
  "supplier_id" => [NilClass, String],
  "parent_id" => [NilClass, String],
  "id" => String,
  "inventory_pool_id" => String,
  "is_incomplete" => [TrueClass, FalseClass],
  "needs_permission" => [TrueClass, FalseClass],
  "user_name" => String,
  "version" => [NilClass, String],
  "room_id" => String,
  "serial_number" => String,
  "price" => [NilClass, Numeric],
  "created_at" => String,
  "product_name" => String,
  "insurance_number" => [NilClass, String]
}

put_post_response = {
  "properties" => Hash,
  "inventory_code" => String,
  "owner_id" => String,
  "is_borrowable" => [TrueClass, FalseClass],
  "retired" => [NilClass, TrueClass, FalseClass],
  "is_inventory_relevant" => [TrueClass, FalseClass],
  "last_check" => [NilClass, String],
  "shelf" => String,
  "status_note" => String,
  "name" => [NilClass, String],
  "invoice_number" => String,
  "is_broken" => [TrueClass, FalseClass],
  "note" => String,
  "updated_at" => String,
  "retired_reason" => [NilClass, String],
  "responsible" => [NilClass, String],
  "invoice_date" => [NilClass, String],
  "model_id" => String,
  "supplier_id" => [NilClass, String],
  "parent_id" => [NilClass, String],
  "id" => String,
  "inventory_pool_id" => String,
  "is_incomplete" => [TrueClass, FalseClass],
  "item_version" => [NilClass, String],
  "needs_permission" => [TrueClass, FalseClass],
  "user_name" => String,
  "room_id" => String,
  "serial_number" => String,
  "price" => [NilClass, Numeric],
  "created_at" => String,
  "insurance_number" => [NilClass, String]
}

feature "Inventory Item" do
  context "when interacting with inventory item with role=lending_manager", driver: :selenium_headless do
    include_context :setup_models_api_model, "lending_manager"
    include_context :generate_session_header

    let(:pool_id) { @inventory_pool.id }
    let(:cookie_header) { @cookie_header }
    let(:client) { plain_faraday_json_client(cookie_header) }

    let(:form_categories) { @form_categories }
    let(:form_compatible_models) { @form_compatible_models }

    let(:path_test_pdf) { File.expand_path("spec/files/test.pdf", Dir.pwd) }
    let(:path_test_txt) { File.expand_path("spec/files/text-file.txt", Dir.pwd) }

    before do
      result = client.get "/inventory/#{pool_id}/entitlement-groups"
      @form_entitlement_groups = result.body
      raise "Failed to fetch entitlement groups" unless result.status == 200

      result = client.get "/inventory/owners"
      @form_owners = result.body
      raise "Failed to fetch manufacturers" unless result.status == 200

      result = client.get "/inventory/buildings"
      @form_buildings = result.body
      raise "Failed to fetch entitlement groups" unless result.status == 200

      result = client.get "/inventory/rooms?building_id=#{@form_buildings[0]["id"]}"
      @form_rooms = result.body
      raise "Failed to fetch entitlement groups" unless result.status == 200

      result = client.get "/inventory/manufacturers?type=Model&in-detail=true"
      @form_model_names = result.body
      raise "Failed to fetch compatible models" unless result.status == 200

      result = client.get "/inventory/manufacturers?type=Model&in-detail=true&search-term=#{@form_model_names[0]["product"]}"
      @form_model_data = result.body
      raise "Failed to fetch compatible models" unless result.status == 200
    end

    context "fetch of form" do
      it "ensures form manufacturer data is fetched" do
        result = client.get "/inventory/#{pool_id}/item"
        response_body = result.body
        expect(result.status).to be(200)

        expect(response_body["data"]["inventory_pool_id"]).to eq(pool_id)
        expect(response_body["fields"].count).to eq(26)

        expect(response_body).not_to be_nil
        expect(response_body.count).to eq(2)
      end
    end

    context "create item" do
      it "create and modify item with attachments" do
        # create item
        form_data = {
          serial_number: "",
          note: "",
          attachments: [File.open(path_test_txt, "rb"), File.open(path_test_pdf, "rb")],
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

        result = http_multipart_client(
          "/inventory/#{pool_id}/item",
          form_data,
          method: :post,
          headers: cookie_header
        )
        # binding.pry

        expect(result.status).to eq(200)
        model_id = result.body["data"]["model_id"]
        item_id = result.body["data"]["id"]

        # fetch created item
        result = client.get "/inventory/#{pool_id}/models/#{model_id}/item/#{item_id}"
        response_body = result.body
        attachments = result.body["data"]["attachments"]
        attachment_id = attachments[0]["id"]
        expect(attachments.count).to be(2)
        expect(result.status).to be(200)

        expected_form_fields(result.body["fields"], expected_fields)
        expect(validate_map_structure(result.body["data"], fetch_response)).to eq(true)

        expect(response_body).not_to be_nil
        expect(response_body.count).to eq(2)

        expect(result.status).to eq(200)
        expect(response_body["data"]).to be_present
        expect(response_body["fields"].count).to eq(25)

        # update item request
        form_data = {
          :serial_number => nil,
          :note => nil,
          :attachments => [], # binary data
          "attachments_to_delete" => [attachment_id],
          :is_inventory_relevant => "true",
          :last_check => nil,
          :user_name => nil,
          :invoice_number => nil,
          :invoice_date => nil,
          :price => nil,
          :shelf => nil,
          :inventory_code => "AUS45859",
          :retired => "false",
          :is_broken => "false",
          :is_incomplete => "false",
          :is_borrowable => "false",
          :status_note => nil,
          :room_id => @form_rooms[0]["id"],
          :model_id => @form_model_data[0]["id"],
          :owner_id => @form_owners[0]["id"],
          :properties => {
            electrical_power: "",
            imei_number: "",
            p4u: "",
            reference: "invoice",
            warranty_expiration: "",
            quantity_allocations: []
          }.to_json
        }.transform_values { |v| v.nil? ? "" : v.to_s }
        result = http_multipart_client(
          "/inventory/#{pool_id}/models/#{model_id}/item/#{item_id}",
          form_data,
          method: :put,
          headers: cookie_header
        )
        expect(result.status).to eq(200)
        expect(validate_map_structure(result.body["data"], put_post_response)).to eq(true)

        # fetch created item
        result = client.get "/inventory/#{pool_id}/models/#{model_id}/item/#{item_id}"
        attachments = result.body["data"]["attachments"]
        expect(attachments.count).to be(1)
        expect(result.status).to be(200)
        expect(validate_map_structure(result.body["data"], fetch_response)).to eq(true)
      end

      context "create item" do
        it "create and modify item with attachments" do
          # create item
          form_data = {
            serial_number: "",
            note: "",
            attachments: [File.open(path_test_txt, "rb"), File.open(path_test_pdf, "rb")],
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

          result = http_multipart_client(
            "/inventory/#{pool_id}/item",
            form_data,
            method: :post,
            headers: cookie_header
          )
          # binding.pry

          expect(result.status).to eq(200)
          model_id = result.body["data"]["model_id"]
          item_id = result.body["data"]["id"]
          expect(validate_map_structure(result.body["data"], put_post_response)).to eq(true)

          # fetch created item
          result = client.get "/inventory/#{pool_id}/models/#{model_id}/item/#{item_id}"
          response_body = result.body
          attachments = result.body["data"]["attachments"]
          attachment_id = attachments[0]["id"]
          expect(attachments.count).to be(2)
          expect(result.status).to be(200)
          expect(validate_map_structure(result.body["data"], fetch_response)).to eq(true)

          expect(response_body).not_to be_nil
          expect(response_body.count).to eq(2)

          expect(result.status).to eq(200)
          expect(response_body["data"]).to be_present
          expect(response_body["fields"].count).to eq(25)

          # update item request
          form_data = {
            :serial_number => nil,
            :note => nil,
            :attachments => [], # binary data
            "attachments_to_delete" => [attachment_id],
            :is_inventory_relevant => "true",
            :last_check => nil,
            :user_name => nil,
            :invoice_number => nil,
            :invoice_date => nil,
            :price => nil,
            :shelf => nil,
            :inventory_code => "AUS45859",
            :retired => "false",
            :is_broken => "false",
            :is_incomplete => "false",
            :is_borrowable => "false",
            :status_note => nil,
            :room_id => @form_rooms[0]["id"],
            :model_id => @form_model_data[0]["id"],
            :owner_id => @form_owners[0]["id"],
            :properties => {
              electrical_power: "",
              imei_number: "",
              p4u: "",
              reference: "invoice",
              warranty_expiration: "",
              quantity_allocations: []
            }.to_json
          }.transform_values { |v| v.nil? ? "" : v.to_s }
          result = http_multipart_client(
            "/inventory/#{pool_id}/models/#{model_id}/item/#{item_id}",
            form_data,
            method: :put,
            headers: cookie_header
          )
          expect(result.status).to eq(200)
          expect(validate_map_structure(result.body["data"], put_post_response)).to eq(true)

          # fetch created item
          result = client.get "/inventory/#{pool_id}/models/#{model_id}/item/#{item_id}"
          attachments = result.body["data"]["attachments"]
          expect(attachments.count).to be(1)
          expect(result.status).to be(200)
          expect(validate_map_structure(result.body["data"], fetch_response)).to eq(true)
        end
      end
    end
  end
end
