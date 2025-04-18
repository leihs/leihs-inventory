require "spec_helper"
require "pry"
require_relative "../../../_shared"
require_relative "../../_common"
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

describe "Inventory Model" do
  ["inventory_manager", "lending_manager"].each do |role|
    context "when interacting with inventory model with role=#{role}" do
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
        [path_arrow, path_arrow_thumb, path_test_pdf, path_test2_pdf, path_test_txt].each do |path|
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
        @form_models_compatibles = convert_to_id_correction(resp.body)
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

      def convert_to_id_correction(compatibles)
        compatibles.each do |compatible|
          compatible["id"] = compatible.delete("model_id")
        end
      end

      context "create model (min)" do
        it "creates a model with all available attributes" do
          # create model request
          form_data = {
            "product" => Faker::Commerce.product_name
          }

          resp = http_multipart_client(
            "/inventory/#{pool_id}/model",
            form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(200)

          # fetch created model
          model_id = resp.body["data"]["id"]
          resp = client.get "/inventory/#{pool_id}/model/#{model_id}"

          expect(resp.body[0]["images"].count).to eq(0)
          expect(resp.body[0]["attachments"].count).to eq(0)

          expect(resp.body[0]["entitlement_groups"].count).to eq(0)
          expect(resp.body[0]["compatibles"].count).to eq(0)
          expect(resp.body[0]["categories"].count).to eq(0)
          expect(resp.status).to eq(200)

          expect(Image.where(target_id: model_id).count).to eq(0)

          # update model request
          form_data = {
            "product" => "updated product"
          }

          resp = http_multipart_client(
            "/inventory/#{pool_id}/model/#{model_id}",
            form_data,
            method: :put,
            headers: cookie_header
          )

          expect(resp.status).to eq(200)
          expect(resp.body[0]["id"]).to eq(model_id)

          # fetch updated model
          resp = client.get "/inventory/#{pool_id}/model/#{model_id}"

          expect(resp.body[0]["images"].count).to eq(0)
          expect(resp.body[0]["attachments"].count).to eq(0)
          expect(resp.body[0]["entitlement_groups"].count).to eq(0)
          expect(resp.body[0]["entitlement_groups"].count).to eq(0)
          expect(resp.body[0]["compatibles"].count).to eq(0)
          expect(resp.body[0]["categories"].count).to eq(0)
          expect(resp.status).to eq(200)
        end
      end

      context "create model" do
        it "creates a model with all available attributes" do
          compatibles = @form_models_compatibles

          # create model request
          form_data = {
            "product" => Faker::Commerce.product_name,
            "images" => [File.open(path_arrow, "rb"), File.open(path_arrow_thumb, "rb")],
            "attachments" => [File.open(path_test_pdf, "rb")],
            "version" => "v1.0",
            "manufacturer" => @form_manufacturers.first, # Use fetched manufacturer name
            "is_package" => "true",
            "description" => "A sample product",
            "technical_detail" => "Specs go here",
            "internal_description" => "Internal notes",
            "important_notes" => "Important usage notes",
            "entitlements" => [{entitlement_group_id: @form_entitlement_groups.first["id"], entitlement_id: nil, quantity: 33}].to_json,
            "compatibles" => [compatibles.first].to_json,
            "categories" => [@form_model_groups.first].to_json
          }

          resp = http_multipart_client(
            "/inventory/#{pool_id}/model",
            form_data,
            headers: cookie_header
          )

          expect(resp.status).to eq(200)

          # fetch created model
          model_id = resp.body["data"]["id"]
          resp = client.get "/inventory/#{pool_id}/model/#{model_id}"

          expect(resp.body[0]["images"].count).to eq(2)
          expect(resp.body[0]["attachments"].count).to eq(1)

          expect(resp.body[0]["entitlement_groups"].count).to eq(1)
          expect(resp.body[0]["compatibles"].count).to eq(1)
          expect(resp.body[0]["categories"].count).to eq(1)
          expect(resp.status).to eq(200)

          expect(Image.where(target_id: model_id).count).to eq(4)

          # update model request
          form_data = {
            "product" => "updated product",
            "images" => [File.open(path_arrow, "rb"), File.open(path_arrow_thumb, "rb")],
            "attachments" => [File.open(path_test_pdf, "rb")],
            "version" => "updated v1.0",
            "manufacturer" => "updated manufacturer",
            "is_package" => "true",
            "description" => "updated description",
            "technical_detail" => "updated techDetail",
            "internal_description" => "updated internalDesc",
            "important_notes" => "updated notes",
            "entitlements" => [{entitlement_group_id: @form_entitlement_groups.first["id"], entitlement_id: nil, quantity: 11}].to_json,
            "compatibles" => [compatibles.first, compatibles.second].to_json,
            "categories" => [@form_model_groups.first, @form_model_groups.second].to_json
          }

          resp = http_multipart_client(
            "/inventory/#{pool_id}/model/#{model_id}",
            form_data,
            method: :put,
            headers: cookie_header
          )
          expect(resp.status).to eq(200)
          expect(resp.body[0]["id"]).to eq(model_id)

          # fetch updated model
          resp = client.get "/inventory/#{pool_id}/model/#{model_id}"
          expect(resp.body[0]["images"].count).to eq(4)
          expect(resp.body[0]["attachments"].count).to eq(2)
          expect(resp.body[0]["entitlement_groups"].count).to eq(1)
          expect(resp.body[0]["entitlement_groups"][0]["quantity"]).to eq(11)

          compatibles = resp.body[0]["compatibles"]
          expect(compatibles.count).to eq(2)
          expect(resp.body[0]["categories"].count).to eq(2)
          expect(resp.status).to eq(200)
        end
      end

      def find_with_cover(compatibles)
        compatibles.find { |c| !c["cover_image_url"].nil? }
      end

      def find_without_cover(compatibles)
        compatibles.find { |c| c["cover_image_url"].nil? }
      end

      def select_with_cover(compatibles)
        compatibles.select { |c| !c["cover_image_url"].nil? }
      end

      def select_without_cover(compatibles)
        compatibles.select { |c| c["cover_image_url"].nil? }
      end

      def select_two_variants_of_compatibles(compatibles)
        compatible_with_cover_image = find_with_cover(compatibles)
        compatible_without_cover_image = find_without_cover(compatibles)

        [compatible_with_cover_image, compatible_without_cover_image]
      end

      context "create & modify model (max)" do
        it "creates a model with all available attributes" do
          compatibles = @form_models_compatibles

          # FIXME - id is valid, but what happens with model_id-entry? Why is coercion not working?
          two_variants_of_compatibles = select_two_variants_of_compatibles(compatibles)

          form_data = {
            "product" => Faker::Commerce.product_name,
            "version" => "v1.0",
            "manufacturer" => @form_manufacturers.first,
            "description" => "A sample product",
            "technical_detail" => "Specs go here",
            "internal_description" => "Internal notes",
            "hand_over_note" => "Hand over notes",

            "images" => [File.open(path_arrow, "rb"), File.open(path_arrow_thumb, "rb")],

            "properties" => [{key: "prop-1", value: "bar1"}, {key: "prop-2", value: "bar2"}].to_json,
            "accessories" => [{name: "acc1", inventory_pool: false}, {name: "acc2", inventory_pool: true}].to_json,
            "entitlements" => [{entitlement_group_id: @form_entitlement_groups.first["id"], entitlement_id: nil, quantity: 33},
              {entitlement_group_id: @form_entitlement_groups.second["id"], entitlement_id: nil, quantity: 55}].to_json,
            "categories" => [@form_model_groups.first, @form_model_groups.second].to_json,
            "compatibles" => two_variants_of_compatibles.to_json,

            "attachments" => [File.open(path_test_pdf, "rb"), File.open(path_test2_pdf, "rb")],
            "is_package" => "true"
          }

          resp = http_multipart_client(
            "/inventory/#{pool_id}/model",
            form_data,
            headers: cookie_header
          )
          expect(compare_values(resp.body["data"], form_data,
            ["version", "description", "technical_detail", "internal_description", "hand_over_note",
              "is_package"])).to eq(true)

          expect(resp.status).to eq(200)
          expect(validate_map_structure(resp.body["data"], post_response)).to eq(true)

          # fetch created model
          model_id = resp.body["data"]["id"]
          resp = client.get "/inventory/#{pool_id}/model/#{model_id}"
          images = resp.body[0]["images"]
          attachments = resp.body[0]["attachments"]

          expect(resp.body[0]["images"].count).to eq(2)
          expect(resp.body[0]["attachments"].count).to eq(2)
          expect(resp.body[0]["entitlement_groups"].count).to eq(2)
          expect(resp.body[0]["compatibles"].count).to eq(2)

          expected_compatibles = resp.body[0]["compatibles"]
          expect(select_with_cover(expected_compatibles).count).to eq(1)
          expect(select_without_cover(expected_compatibles).count).to eq(1)

          expect(resp.body[0]["categories"].count).to eq(2)
          expect(resp.status).to eq(200)
          expect(Image.where(target_id: model_id).count).to eq(4)

          # create model request
          form_data = {
            "product" => Faker::Commerce.product_name,
            "version" => "v1.0",
            "manufacturer" => @form_manufacturers.first,
            "description" => "A sample product",
            "technical_detail" => "Specs go here",
            "internal_description" => "Internal notes",
            "hand_over_note" => "Hand over notes",

            "properties" => [{key: "prop-1", value: "bar1"}, add_delete_flag({key: "prop-2", value: "bar2"})].to_json,
            "accessories" => [{name: "acc1", inventory_pool: false}, add_delete_flag({name: "acc2", inventory_pool: true})].to_json,
            "entitlements" => [{entitlement_group_id: @form_entitlement_groups.first["id"], entitlement_id: nil, quantity: 33},
              add_delete_flag({entitlement_group_id: @form_entitlement_groups.second["id"], entitlement_id: nil, quantity: 55})].to_json,
            "categories" => [@form_model_groups.first, add_delete_flag(@form_model_groups.second)].to_json,
            "compatibles" => [two_variants_of_compatibles.first, add_delete_flag(two_variants_of_compatibles.second)].to_json,

            "attachments" => [],
            "images" => [],
            "attachments_to_delete" => [attachments.first["id"]].to_json,
            "images_to_delete" => [images.first["id"]].to_json,
            "is_package" => "false"
          }

          resp = http_multipart_client(
            "/inventory/#{pool_id}/model/#{model_id}",
            form_data,
            method: :put,
            headers: cookie_header
          )

          expect(compare_values(resp.body[0], form_data,
            ["product", "version", "manufacturer", "description", "technical_detail",
              "internal_description", "hand_over_note", "is_package"])).to eq(true)

          expect(validate_map_structure(resp.body.first, put_response)).to eq(true)
          expect(resp.status).to eq(200)
          expect(resp.body[0]["id"]).to eq(model_id)

          # fetch updated model
          resp = client.get "/inventory/#{pool_id}/model/#{model_id}"

          expect(validate_map_structure(resp.body.first, get_response)).to eq(true)
          expect(compare_values(resp.body[0], form_data,
            ["product", "version", "manufacturer", "description", "technical_detail",
              "internal_description", "hand_over_note", "is_package"])).to eq(true)

          expect(resp.body[0]["images"].count).to eq(2)
          expect(resp.body[0]["attachments"].count).to eq(1)
          expect(resp.body[0]["entitlement_groups"].count).to eq(1)
          expect(resp.body[0]["compatibles"].count).to eq(1)
          expect(resp.body[0]["categories"].count).to eq(1)
          expect(resp.status).to eq(200)
        end
      end
    end
  end
end
