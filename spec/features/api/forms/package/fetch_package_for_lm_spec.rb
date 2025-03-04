require "spec_helper"
require "pry"
require_relative "../../_shared"
require_relative "../_common"
require "faker"

expected_lm_fields = ["note", "last_check", "price", "building_id", "room_id", "shelf", "inventory_code", "model_id",
  "retired", "retired_reason", "is_broken", "is_incomplete", "is_borrowable", "status_note",
  "add-item-group"]

put_post_response = {
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
  "invoice_number" => [NilClass, String],
  "is_broken" => [TrueClass, FalseClass],
  "note" => [NilClass, String],
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
  "user_name" => [NilClass, String],
  "room_id" => String,
  "serial_number" => [NilClass, String],
  "price" => [NilClass, Numeric],
  "created_at" => String,
  "insurance_number" => [NilClass, String]
}

get_response = {
  "inventory_code" => String,
  "owner_id" => String,
  "is_borrowable" => [TrueClass, FalseClass],
  "retired" => [TrueClass, FalseClass],
  "is_inventory_relevant" => [TrueClass, FalseClass],
  "last_check" => [NilClass, String],
  "building_id" => String,
  "shelf" => [NilClass, String],
  "status_note" => [NilClass, String],
  "items_attributes" => Array,
  "is_broken" => [TrueClass, FalseClass],
  "note" => [NilClass, String],
  "updated_at" => String,
  "retired_reason" => [NilClass, String],
  "product" => Hash,
  "model_id" => String,
  "id" => String,
  "inventory_pool_id" => String,
  "is_incomplete" => [TrueClass, FalseClass],
  "user_name" => [NilClass, String],
  "room_id" => String,
  "price" => [NilClass, Numeric],
  "created_at" => String,
  "product_name" => String
}

["lending_manager"].each do |role|
  feature "Inventory package" do
    context "when interacting with inventory package with role=#{role}", driver: :selenium_headless do
      include_context :setup_models_api_model, role
      include_context :setup_unknown_building_room_supplier
      include_context :generate_session_header

      let(:cookie_header) { @cookie_header }
      let(:client) { plain_faraday_json_client(cookie_header) }
      let(:pool_id) { @inventory_pool.id }

      let(:software_model) { @software_model }
      let(:license_item) { @license_item }
      let(:model_id) { @software_model.id }

      before do
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

      context "create model" do
        it "fetch default" do
          resp = client.get "/inventory/#{pool_id}/items-with-model-info?result_type=Normal"

          # FIXME: no resp handling
          # resp = client.get "/inventory/#{pool_id}/items-with-model-info?result_type=Normal&search_term=podest"

          expect(resp.status).to eq(200)
          expect(resp.body.count).to eq(1)
        end

        it "fetch default" do
          resp = client.get "/inventory/#{pool_id}/package"

          expect(resp.status).to eq(200)
          expect(resp.body["data"]["inventory_pool_id"]).to eq(pool_id)
          expect(resp.body["fields"].count).to eq(15)
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
          resp = client.get "/inventory/buildings"

          expect(resp.status).to eq(200)
          expect(resp.body.count).to eq(3)
        end

        it "fetch default" do
          resp = client.get "/inventory/manufacturers?type=Model&in-detail=true"

          expect(resp.status).to eq(200)
          expect(resp.body.count).to eq(2)
        end

        it "fetch by form data" do
          resp = client.get "/inventory/#{pool_id}/package"

          expect(resp.body["data"]["inventory_pool_id"]).to eq(pool_id)
          expect(resp.body["fields"].count).to eq(15)
        end

        it "create, fetch & update by form data" do
          # create package
          form_data = {
            is_inventory_relevant: true,
            last_check: nil,
            user_name: nil,
            price: 12.33,
            shelf: nil,
            inventory_code: "P-AUS00002",
            retired: false,
            is_broken: false,
            is_incomplete: false,
            is_borrowable: false,
            status_note: nil,
            note: nil,
            room_id: @form_rooms[0]["id"],
            model_id: @form_model_data[0]["id"],
            owner_id: @form_owners[0]["id"],
            items_attributes: []
          }.transform_values { |v| v.nil? ? "" : v.to_s }

          resp = http_multipart_client(
            "/inventory/#{pool_id}/package",
            form_data,
            headers: cookie_header
          )

          expect(resp.status).to eq(200)
          expect(resp.body["data"]).to be_present
          expect(resp.body["validation"].count).to eq(0)
          expect(validate_map_structure(resp.body["data"], put_post_response)).to eq(true)

          item_id = resp.body["data"]["id"]
          model_id = resp.body["data"]["model_id"]

          # fetch package
          resp = client.get "/inventory/#{pool_id}/models/#{model_id}/package/#{item_id}"
          expect(resp.body["data"]).to be_present
          expect(resp.body["fields"].count).to eq(15)
          expect(validate_map_structure(resp.body["data"], get_response)).to eq(true)
          expected_form_fields(resp.body["fields"], expected_lm_fields)

          # update package
          resp = http_multipart_client(
            "/inventory/#{pool_id}/models/#{model_id}/package/#{item_id}",
            form_data,
            method: :put,
            headers: cookie_header
          )

          expect(validate_map_structure(resp.body["data"], put_post_response)).to eq(true)
          expect(resp.status).to eq(200)
          expect(resp.body["data"]).to be_present
          expect(resp.body["validation"].count).to eq(0)
        end
      end
    end
  end
end
