require "spec_helper"
require "pry"
require_relative "../../_shared"
require_relative "../_common"
require "faker"

def add_delete_flag(map)
  map["delete"] = true
  map
end

post_response = {
  "description" => String,
  "is_package" => [TrueClass, FalseClass],
  "maintenance_period" => Numeric,
  "type" => String,
  "cover_image_id" => [NilClass, String],
  "hand_over_note" => String,
  "updated_at" => String,
  "internal_description" => String,
  "product" => String,
  "info_url" => [NilClass, String],
  "id" => String,
  "manufacturer" => String,
  "version" => String,
  "created_at" => String,
  "technical_detail" => String
}
put_response = {
  "description" => String,
  "is_package" => [TrueClass, FalseClass],
  "maintenance_period" => Numeric,
  "type" => String,
  "cover_image_id" => [NilClass, String],
  "hand_over_note" => String,
  "updated_at" => String,
  "internal_description" => String,
  "product" => String,
  "info_url" => [NilClass, String],
  "id" => String,
  "manufacturer" => String,
  "version" => String,
  "created_at" => String,
  "technical_detail" => String
}

get_response = {
  "description" => String,
  "properties" => Array,
  "is_package" => [TrueClass, FalseClass],
  "accessories" => Array,
  "entitlement_groups" => Array,
  "images" => Array,
  "attachments" => Array,
  "type" => String,
  "hand_over_note" => String,
  "internal_description" => String,
  "product" => String,
  "categories" => Array,
  "id" => String,
  "compatibles" => Array,
  "manufacturer" => String,
  "version" => String,
  "technical_detail" => String
}

feature "Inventory Model" do
  ["inventory_manager", "lending_manager"].each do |role|
    context "when interacting with inventory model with role=#{role}", driver: :selenium_headless do
      include_context :setup_models_api_model, role
      include_context :generate_session_header

      let(:pool_id) { @inventory_pool.id }
      let(:cookie_header) { @cookie_header }
      let(:client) { plain_faraday_json_client(cookie_header) }

      let(:form_categories) { @form_categories }
      let(:form_compatible_models) { @form_compatible_models }

      let(:path_arrow) { File.expand_path("spec/files/arrow.png", Dir.pwd) }
      let(:path_arrow_thumb) { File.expand_path("spec/files/arrow_thumb.png", Dir.pwd) }
      let(:path_test_pdf) { File.expand_path("spec/files/test.pdf", Dir.pwd) }
      let(:path_test2_pdf) { File.expand_path("spec/files/test2.pdf", Dir.pwd) }
      let(:path_test_txt) { File.expand_path("spec/files/text-file.txt", Dir.pwd) }

      before do
        [path_arrow, path_arrow_thumb, path_test_pdf].each do |path|
          raise "File not found: #{path}" unless File.exist?(path)
        end

        # Fetch shared data and set global instance variables
        resp = client.get "/inventory/manufacturers?type=Model"
        @form_manufacturers = resp.body
        raise "Failed to fetch manufacturers" unless resp.status == 200

        resp = client.get "/inventory/#{pool_id}/entitlement-groups"
        @form_entitlement_groups = resp.body
        raise "Failed to fetch entitlement groups" unless resp.status == 200

        resp = client.get "/inventory/models-compatibles"
        @form_models_compatibles = resp.body["data"]
        raise "Failed to fetch compatible models" unless resp.status == 200

        resp = client.get "/inventory/#{pool_id}/model-groups"
        @form_model_groups = resp.body
        raise "Failed to fetch model groups" unless resp.status == 200
      end

      context "fetch form data" do
        it "ensures form manufacturer data is fetched" do
          expect(@form_manufacturers).not_to be_nil
          expect(@form_manufacturers.count).to eq(2)
        end

        it "ensures entitlement groups data is fetched" do
          expect(@form_entitlement_groups).not_to be_nil
          expect(@form_entitlement_groups.count).to eq(2)
        end

        it "ensures models compatible data is fetched" do
          expect(@form_models_compatibles).not_to be_nil
          expect(@form_models_compatibles.count).to eq(3)
        end

        it "ensures model groups data is fetched" do
          expect(@form_model_groups).not_to be_nil
          expect(@form_model_groups.count).to eq(2)
        end
      end

      context "create model (min)" do
        it "creates a model with all available attributes" do
          compatibles = @form_models_compatibles
          compatibles.first["id"] = compatibles.first.delete("model_id")

          # create model request
          form_data = {
            "product" => Faker::Commerce.product_name
          }

          result = http_multipart_client(
            "/inventory/#{pool_id}/model",
            form_data,
            headers: cookie_header
          )

          expect(result.status).to eq(200)

          # fetch created model
          model_id = result.body["data"]["id"]
          resp = client.get "/inventory/#{pool_id}/model/#{model_id}"

          expect(resp.body[0]["images"].count).to eq(0)
          expect(resp.body[0]["attachments"].count).to eq(0)

          expect(resp.body[0]["entitlement_groups"].count).to eq(0)
          expect(resp.body[0]["compatibles"].count).to eq(0)
          expect(resp.body[0]["categories"].count).to eq(0)
          expect(result.status).to eq(200)

          expect(Image.where(target_id: model_id).count).to eq(0)

          # update model request
          form_data = {
            "product" => "updated product"
          }

          result = http_multipart_client(
            "/inventory/#{pool_id}/model/#{model_id}",
            form_data,
            method: :put,
            headers: cookie_header
          )

          expect(result.status).to eq(200)
          expect(result.body[0]["id"]).to eq(model_id)

          # fetch updated model
          resp = client.get "/inventory/#{pool_id}/model/#{model_id}"

          expect(resp.body[0]["images"].count).to eq(0)
          expect(resp.body[0]["attachments"].count).to eq(0)
          expect(resp.body[0]["entitlement_groups"].count).to eq(0)
          expect(resp.body[0]["entitlement_groups"].count).to eq(0)
          expect(resp.body[0]["compatibles"].count).to eq(0)
          expect(resp.body[0]["categories"].count).to eq(0)
          expect(result.status).to eq(200)
        end
      end

      context "create model" do
        it "creates a model with all available attributes" do
          compatibles = @form_models_compatibles
          compatibles.first["id"] = compatibles.first.delete("model_id")

          # create model request
          form_data = {
            "product" => Faker::Commerce.product_name,
            "images" => [File.open(path_arrow, "rb"), File.open(path_arrow_thumb, "rb")],
            "attachments" => [File.open(path_test_pdf, "rb")],
            "version" => "v1.0",
            "manufacturer" => @form_manufacturers.first, # Use fetched manufacturer name
            "isPackage" => "true",
            "description" => "A sample product",
            "technicalDetails" => "Specs go here",
            "internalDescription" => "Internal notes",
            "importantNotes" => "Important usage notes",
            "entitlements" => [{entitlement_group_id: @form_entitlement_groups.first["id"], entitlement_id: nil, quantity: 33}].to_json,
            "compatibles" => [compatibles.first].to_json,
            "categories" => [@form_model_groups.first].to_json
          }

          result = http_multipart_client(
            "/inventory/#{pool_id}/model",
            form_data,
            headers: cookie_header
          )

          expect(result.status).to eq(200)

          # fetch created model
          model_id = result.body["data"]["id"]
          resp = client.get "/inventory/#{pool_id}/model/#{model_id}"

          expect(resp.body[0]["images"].count).to eq(1)
          expect(resp.body[0]["attachments"].count).to eq(1)

          expect(resp.body[0]["entitlement_groups"].count).to eq(1)
          expect(resp.body[0]["compatibles"].count).to eq(1)
          expect(resp.body[0]["categories"].count).to eq(1)
          expect(result.status).to eq(200)

          expect(Image.where(target_id: model_id).count).to eq(2)

          # update model request
          form_data = {
            "product" => "updated product",
            "images" => [File.open(path_arrow, "rb"), File.open(path_arrow_thumb, "rb")],
            "attachments" => [File.open(path_test_pdf, "rb")],
            "version" => "updated v1.0",
            "manufacturer" => "updated manufacturer",
            "isPackage" => "true",
            "description" => "updated description",
            "technicalDetails" => "updated techDetail",
            "internalDescription" => "updated internalDesc",
            "importantNotes" => "updated notes",
            "entitlements" => [{entitlement_group_id: @form_entitlement_groups.first["id"], entitlement_id: nil, quantity: 11}].to_json,
            "compatibles" => [compatibles.first, compatibles.second].to_json,
            "categories" => [@form_model_groups.first, @form_model_groups.second].to_json
          }

          result = http_multipart_client(
            "/inventory/#{pool_id}/model/#{model_id}",
            form_data,
            method: :put,
            headers: cookie_header
          )
          expect(result.status).to eq(200)
          expect(result.body[0]["id"]).to eq(model_id)

          # fetch updated model
          resp = client.get "/inventory/#{pool_id}/model/#{model_id}"

          expect(resp.body[0]["images"].count).to eq(2)
          expect(resp.body[0]["attachments"].count).to eq(2)
          expect(resp.body[0]["entitlement_groups"].count).to eq(1)
          expect(resp.body[0]["entitlement_groups"][0]["quantity"]).to eq(11)
          expect(resp.body[0]["compatibles"].count).to eq(2)
          expect(resp.body[0]["categories"].count).to eq(2)
          expect(result.status).to eq(200)
        end
      end

      context "create & modify model (max)" do
        it "creates a model with all available attributes" do
          compatibles = @form_models_compatibles
          compatibles.first["id"] = compatibles.first.delete("model_id")

          # create model request
          form_data = {
            "product" => Faker::Commerce.product_name,
            "version" => "v1.0",
            "manufacturer" => @form_manufacturers.first,
            "description" => "A sample product",
            "technicalDetails" => "Specs go here",
            "internalDescription" => "Internal notes",
            "handOverNote" => "Hand over notes",

            "images" => [File.open(path_arrow, "rb"), File.open(path_arrow_thumb, "rb")],

            "properties" => [{key: "prop-1", value: "bar1"}, {key: "prop-2", value: "bar2"}].to_json,
            "accessories" => [{name: "acc1", inventory_pool: false}, {name: "acc2", inventory_pool: true}].to_json,
            "entitlements" => [{entitlement_group_id: @form_entitlement_groups.first["id"], entitlement_id: nil, quantity: 33},
              {entitlement_group_id: @form_entitlement_groups.second["id"], entitlement_id: nil, quantity: 55}].to_json,
            "categories" => [@form_model_groups.first, @form_model_groups.second].to_json,
            "compatibles" => [compatibles.first, compatibles.second].to_json,

            "attachments" => [File.open(path_test_pdf, "rb"), File.open(path_test2_pdf, "rb")],
            "isPackage" => "true"
          }

          result = http_multipart_client(
            "/inventory/#{pool_id}/model",
            form_data,
            headers: cookie_header
          )

          expect(result.status).to eq(200)
          expect(validate_map_structure(result.body["data"], post_response)).to eq(true)

          # fetch created model
          model_id = result.body["data"]["id"]
          result = client.get "/inventory/#{pool_id}/model/#{model_id}"
          expect(validate_map_structure(result.body.first, get_response)).to eq(true)

          images = result.body[0]["images"]
          attachments = result.body[0]["attachments"]

          expect(result.body[0]["images"].count).to eq(1)
          expect(result.body[0]["attachments"].count).to eq(2)

          expect(result.body[0]["entitlement_groups"].count).to eq(2)
          expect(result.body[0]["compatibles"].count).to eq(2)
          expect(result.body[0]["categories"].count).to eq(2)
          expect(result.status).to eq(200)
          expect(Image.where(target_id: model_id).count).to eq(2)

          # create model request
          form_data = {
            "product" => Faker::Commerce.product_name,
            "version" => "v1.0",
            "manufacturer" => @form_manufacturers.first,
            "description" => "A sample product",
            "technicalDetails" => "Specs go here",
            "internalDescription" => "Internal notes",
            "handOverNote" => "Hand over notes",

            "properties" => [{key: "prop-1", value: "bar1"}, add_delete_flag({key: "prop-2", value: "bar2"})].to_json,
            "accessories" => [{name: "acc1", inventory_pool: false}, add_delete_flag({name: "acc2", inventory_pool: true})].to_json,
            "entitlements" => [{entitlement_group_id: @form_entitlement_groups.first["id"], entitlement_id: nil, quantity: 33},
              add_delete_flag({entitlement_group_id: @form_entitlement_groups.second["id"], entitlement_id: nil, quantity: 55})].to_json,
            "categories" => [@form_model_groups.first, add_delete_flag(@form_model_groups.second)].to_json,
            "compatibles" => [compatibles.first, add_delete_flag(compatibles.second)].to_json,

            "attachments" => [],
            "images" => [],
            "attachments-to-delete" => [attachments.first["id"]].to_json,
            "images-to-delete" => [images.first["id"]].to_json,
            "isPackage" => "false"
          }

          result = http_multipart_client(
            "/inventory/#{pool_id}/model/#{model_id}",
            form_data,
            method: :put,
            headers: cookie_header
          )
          expect(validate_map_structure(result.body.first, put_response)).to eq(true)
          expect(result.status).to eq(200)
          expect(result.body[0]["id"]).to eq(model_id)

          # fetch updated model
          result = client.get "/inventory/#{pool_id}/model/#{model_id}"

          expect(validate_map_structure(result.body.first, get_response)).to eq(true)

          expect(result.body[0]["images"].count).to eq(0)
          expect(result.body[0]["attachments"].count).to eq(1)
          expect(result.body[0]["entitlement_groups"].count).to eq(1)
          expect(result.body[0]["compatibles"].count).to eq(1)
          expect(result.body[0]["categories"].count).to eq(1)
          expect(result.status).to eq(200)
        end
      end
    end
  end
end
