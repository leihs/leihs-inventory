require "spec_helper"
require "pry"
require_relative "../../../_shared"
require_relative "../../_common"
require "faker"

def add_delete_flag(map)
  map["delete"] = true
  map
end

describe "Inventory Model" do
  ["inventory_manager", "lending_manager"].each do |role|
    context "when interacting with inventory model with role=#{role}" do
      include_context :setup_models_api_model, role
      include_context :generate_session_header

      let(:pool_id) { @inventory_pool.id }
      let(:cookie_header) { @cookie_header }
      let(:client) { plain_faraday_json_client(cookie_header) }

      let(:path_arrow) { File.expand_path("spec/files/arrow.png", Dir.pwd) }
      let(:path_arrow_thumb) { File.expand_path("spec/files/arrow_thumb.png", Dir.pwd) }
      let(:path_test_pdf) { File.expand_path("spec/files/test.pdf", Dir.pwd) }

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
          # puts "before: #{compatible}"
          compatible["id"] = compatible.delete("model_id")
          # puts "after: #{compatible}\n\n"
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
            "technical_details" => "Specs go here",
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
            "technical_details" => "updated techDetail",
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

      def rename_model_id_to_id(compatible)
        # puts "compatible.before: #{compatible}"
        compatible["id"] = compatible["model_id"]
        # puts "compatible.after: #{compatible}"
        compatible
      end

      def find_with_cover2(compatibles)
        compatibles.find { |c| !c["id"].nil? }
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

      context "create model (min)" do
        it "creates a model with all available attributes" do
          # create model request
          product = Faker::Commerce.product_name
          form_data = {
            "product" => product
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
          expect(resp.status).to eq(200)
          expect(resp.body.count).to eq(1)

          # delete model request
          resp = json_client_delete(
            "/inventory/#{pool_id}/model/#{model_id}",
            headers: cookie_header
          )
          expect(resp.status).to eq(200)
          expect(resp.body["deleted_attachments"].count).to eq(0)
          expect(resp.body["deleted_model"].count).to eq(1)

          # retry to delete model request
          resp = json_client_delete(
            "/inventory/#{pool_id}/model/#{model_id}",
            headers: cookie_header
          )
          expect(resp.status).to eq(404)
          # expect(resp.body["error"]).to eq("Request to delete model blocked: model not found")

          # no results when fetching deleted model
          resp = client.get "/inventory/#{pool_id}/model/#{model_id}"
          expect(resp.status).to eq(200)
          expect(resp.body.count).to eq(0)
        end
      end
    end
  end
end
