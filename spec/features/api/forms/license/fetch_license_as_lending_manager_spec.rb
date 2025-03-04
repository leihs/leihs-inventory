require "spec_helper"
require "pry"
require_relative "../../_shared"
require_relative "../_common"
require "faker"

expected_lm_fields = ["serial_number", "note", "attachments", "invoice_date", "price", "supplier_id", "retired",
  "retired_reason", "is_borrowable", "inventory_code", "software_model_id"]

post_response = {
  "properties" => Hash,
  "inventory_code" => String,
  "owner_id" => String,
  "is_borrowable" => [TrueClass, FalseClass],
  "retired" => String,
  "is_inventory_relevant" => [TrueClass, FalseClass],
  "last_check" => [NilClass, String],
  "shelf" => [NilClass, String],
  "status_note" => [NilClass, String],
  "item_id" => String,
  "name" => [NilClass, String],
  "attachments" => Array,
  "invoice_number" => [NilClass, String],
  "is_broken" => [TrueClass, FalseClass],
  "note" => String,
  "updated_at" => String,
  "retired_reason" => String,
  "responsible" => [NilClass, String],
  "invoice_date" => String,
  "model_id" => String,
  "supplier_id" => [NilClass, String],
  "parent_id" => [NilClass, String],
  "id" => String,
  "inventory_pool_id" => String,
  "is_incomplete" => [TrueClass, FalseClass],
  "item_version" => String,
  "needs_permission" => [TrueClass, FalseClass],
  "user_name" => [NilClass, String],
  "room_id" => String,
  "serial_number" => String,
  "price" => Numeric,
  "created_at" => String,
  "insurance_number" => [NilClass, String]
}

get_response = {
  "properties" => Hash,
  "inventory_code" => String,
  "supplier" => [NilClass, String],
  "is_borrowable" => [TrueClass, FalseClass],
  "retired" => [TrueClass, FalseClass],
  "attachments" => Array,
  "note" => String,
  "retired_reason" => String,
  "invoice_date" => String,
  "product" => Hash,
  "inventory_pool_id" => String,
  "version" => String,
  "serial_number" => String,
  "price" => Numeric
}

put_response = {
  "properties" => Hash,
  "inventory_code" => String,
  "owner_id" => String,
  "is_borrowable" => [TrueClass, FalseClass],
  "retired" => [NilClass, TrueClass, FalseClass],
  "is_inventory_relevant" => [TrueClass, FalseClass],
  "last_check" => [NilClass, String],
  "shelf" => [NilClass, String],
  "status_note" => [NilClass, String],
  "name" => [NilClass, String],
  "attachments" => Array,
  "invoice_number" => [NilClass, String],
  "is_broken" => [TrueClass, FalseClass],
  "note" => String,
  "updated_at" => String,
  "retired_reason" => [NilClass, String],
  "responsible" => [NilClass, String],
  "invoice_date" => String,
  "model_id" => String,
  "supplier_id" => [NilClass, String],
  "parent_id" => [NilClass, String],
  "id" => String,
  "inventory_pool_id" => String,
  "is_incomplete" => [TrueClass, FalseClass],
  "item_version" => String,
  "needs_permission" => [TrueClass, FalseClass],
  "user_name" => [NilClass, String],
  "room_id" => String,
  "serial_number" => String,
  "price" => Numeric,
  "created_at" => String,
  "insurance_number" => [NilClass, String]
}

feature "Inventory License" do
  context "when interacting with inventory license with role=lending_manager", driver: :selenium_headless do
    include_context :setup_models_api_license, "lending_manager"
    include_context :setup_unknown_building_room_supplier
    include_context :generate_session_header

    let(:cookie_header) { @cookie_header }
    let(:client) { plain_faraday_json_client(cookie_header) }
    let(:pool_id) { @inventory_pool.id }

    let(:software_model) { @software_model }
    let(:license_item) { @license_item }
    let(:model_id) { @software_model.id }

    let(:path_test_pdf) { File.expand_path("spec/files/test.pdf", Dir.pwd) }
    let(:path_test_txt) { File.expand_path("spec/files/text-file.txt", Dir.pwd) }

    before do
      [path_test_pdf, path_test_txt].each do |path|
        raise "File not found: #{path}" unless File.exist?(path)
      end
    end

    context "create model" do
      it "fetch default" do
        resp = client.get "/inventory/#{pool_id}/license"

        expect(resp.status).to eq(200)
        expect(resp.body["fields"].count).to eq(11)
      end

      it "fetch default" do
        resp = client.get "/inventory/#{pool_id}/entitlement-groups"

        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(2)
      end

      it "fetch default" do
        resp = client.get "/inventory/owners"

        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(2)
      end

      it "fetch default" do
        resp = client.get "/inventory/supplier?search-term=a"

        expect(resp.status).to eq(200)
      end

      it "fetch default" do
        resp = client.get "inventory/manufacturers?type=Software&in-detail=true&search-term=b"

        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(1)
      end

      it "fetch default" do
        resp = client.get "inventory/manufacturers?type=Software&in-detail=true"

        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(1)
      end

      it "creates and update license (simple)" do
        resp = client.get "inventory/manufacturers?type=Software&in-detail=true"

        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(1)

        supplier_id = resp.body[0]["id"]

        form_data = {
          "serial_number" => "your-serial-number",
          "note" => "your-note",
          "attachments_to_delete" => [],
          "invoice_date" => "2024-12-06",
          "price" => "123.45",
          "retired" => false.to_s,
          "is_borrowable" => false.to_s,
          "inventory_code" => "AUS45863",
          "item_version" => "v1.0",
          "model_id" => model_id,
          "supplier_id" => supplier_id,
          "owner_id" => pool_id,
          "properties" => {
            "activation_type" => "none",
            "license_type" => "free",
            "total_quantity" => "",
            "license_expiration" => "",
            "p4u" => "",
            "reference" => "invoice",
            "procured_by" => "",
            "maintenance_contract" => "false",
            "maintenance_expiration" => "",
            "maintenance_currency" => "CHF",
            "maintenance_price" => "",
            "quantity_allocations" => []
          }.to_json
        }

        resp = http_multipart_client(
          "/inventory/#{pool_id}/models/#{model_id}/licenses",
          form_data,
          headers: cookie_header
        )

        expect(resp.status).to eq(200)
        expect(resp.body["data"]["item_id"]).to be
        expect(resp.body["data"]["id"]).to be
        expect(resp.body["data"]["id"]).to eq(resp.body["data"]["item_id"])

        item_id = resp.body["data"]["id"]

        expect(resp.body["data"]["room_id"]).to be
        expect(resp.body["data"]["owner_id"]).to be
        expect(resp.body["data"]["inventory_pool_id"]).to be

        form_data = {
          "serial_number" => "your-serial-number",
          "note" => "your-note",
          "attachments_to_delete" => [],
          "invoice_date" => "2024-12-06",
          "price" => "123.45",
          "retired" => false.to_s,
          "is_borrowable" => false.to_s,
          "inventory_code" => "AUS45863",
          "item_version" => "v1.0",
          "model_id" => model_id,
          "supplier_id" => nil.to_s,
          "owner_id" => pool_id,
          "properties" => {
            "activation_type" => "none",
            "license_type" => "free",
            "total_quantity" => "",
            "license_expiration" => "",
            "p4u" => "",
            "reference" => "invoice",
            "procured_by" => "",
            "maintenance_contract" => "false",
            "maintenance_expiration" => "",
            "maintenance_currency" => "CHF",
            "maintenance_price" => "",
            "quantity_allocations" => []
          }.to_json
        }

        resp = http_multipart_client(
          "/inventory/#{pool_id}/models/#{model_id}/licenses/#{item_id}",
          form_data,
          method: :put,
          headers: cookie_header
        )

        expect(resp.status).to eq(200)

        # TODO: revise to use data/validation response-format
        expect(resp.body[0]["id"]).to be

        # item_id = resp.body[0]["id"]

        expect(resp.body[0]["room_id"]).to be
        expect(resp.body[0]["owner_id"]).to be
        expect(resp.body[0]["inventory_pool_id"]).to be
      end

      it "creates and update license with attachment" do
        # fetch supplier
        resp = client.get "inventory/manufacturers?type=Software&in-detail=true"

        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(1)

        supplier_id = resp.body[0]["id"]

        # create license
        form_data = {
          "serial_number" => "your-serial-number",
          "note" => "your-note",
          "attachments" => [File.open(path_test_pdf, "rb"), File.open(path_test_txt, "rb")],
          "invoice_date" => "2024-12-19",
          "price" => "100",
          "retired" => true.to_s,
          "retired_reason" => "your-reason-retired",
          "is_borrowable" => false.to_s,
          "inventory_code" => "AUS45863",
          "item_version" => "your-version",
          "supplier_id" => supplier_id,
          "owner_id" => pool_id,
          "properties" => {
            "activation_type" => "dongle",
            "dongle_id" => "your-dongle-id",
            "license_type" => "single_workplace",
            "total_quantity" => "33",
            "operating_system" => ["windows", "mac_os_x", "linux", "ios"],
            "installation" => ["citrix", "local", "web"],
            "license_expiration" => "2024-12-05",
            "p4u" => "your-p4u",
            "reference" => "investment",
            "project_number" => "your-project-number",
            "procured_by" => "your-procured-person",
            "maintenance_contract" => "true",
            "maintenance_expiration" => "2024-12-20",
            "maintenance_currency" => "CHF",
            "maintenance_price" => "20",
            "quantity_allocations" => [
              {"quantity" => "your-key", "room" => "your-value"}
            ]
          }.to_json
        }

        resp = http_multipart_client(
          "/inventory/#{pool_id}/models/#{model_id}/licenses",
          form_data,
          headers: cookie_header
        )

        expect(validate_map_structure(resp.body["data"], post_response)).to eq(true)
        expect(resp.status).to eq(200)

        expect(resp.body["data"]["item_id"]).to be
        expect(resp.body["data"]["id"]).to be
        expect(resp.body["data"]["id"]).to eq(resp.body["data"]["item_id"])

        item_id = resp.body["data"]["id"]

        expect(resp.body["data"]["room_id"]).to be
        expect(resp.body["data"]["owner_id"]).to be
        expect(resp.body["data"]["inventory_pool_id"]).to be

        # fetch license
        resp = client.get "/inventory/#{pool_id}/models/#{model_id}/licenses/#{item_id}"
        fields = resp.body["fields"]
        expected_form_fields(fields, expected_lm_fields)

        expect(validate_map_structure(resp.body["data"], get_response)).to eq(true)

        attachments = resp.body["data"]["attachments"]
        expect(resp.status).to eq(200)
        expect(attachments.count).to eq(2)
        expect(resp.body["data"]).to be_present
        expect(resp.body["fields"].count).to eq(11)

        # update license
        form_data = {
          "serial_number" => "your-serial-number",
          "note" => "your-note",
          "invoice_date" => "2024-12-19",
          "price" => "100",
          "retired" => false.to_s,
          "is_borrowable" => false.to_s,
          "inventory_code" => "AUS45863",
          "item_version" => "your-version",
          "supplier_id" => nil.to_s,
          "owner_id" => pool_id,
          "attachments_to_delete" => [attachments[0]["id"]].to_json,
          "properties" => {
            "activation_type" => "none",
            "license_type" => "single_workplace",
            "total_quantity" => "33",
            "operating_system" => [],
            "installation" => [],
            "license_expiration" => "2024-12-05",
            "p4u" => "your-p4u",
            "reference" => "investment",
            "project_number" => "your-project-number",
            "procured_by" => "your-procured-person",
            "maintenance_contract" => true.to_s,
            "maintenance_expiration" => "2024-12-20",
            "maintenance_currency" => "CHF",
            "maintenance_price" => "20",
            "quantity_allocations" => []
          }.to_json
        }

        resp = http_multipart_client(
          "/inventory/#{pool_id}/models/#{model_id}/licenses/#{item_id}",
          form_data,
          method: :put,
          headers: cookie_header
        )

        expect(validate_map_structure(resp.body.first, put_response)).to eq(true)
        expect(resp.status).to eq(200)

        # TODO: revise to use data/validation resultonse-format
        expect(resp.body[0]["id"]).to be
        expect(resp.body[0]["room_id"]).to be
        expect(resp.body[0]["owner_id"]).to be
        expect(resp.body[0]["inventory_pool_id"]).to be

        # fetch license
        resp = client.get "/inventory/#{pool_id}/models/#{model_id}/licenses/#{item_id}"
        fields = resp.body["fields"]
        expected_form_fields(fields, expected_lm_fields)
        expect(validate_map_structure(resp.body["data"], get_response)).to eq(true)

        attachments = resp.body["data"]["attachments"]
        expect(resp.status).to eq(200)
        expect(attachments.count).to eq(1)
        expect(resp.body["data"]).to be_present
      end
    end
  end
end
