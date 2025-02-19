require "spec_helper"
require "pry"
require_relative "../../_shared"
require_relative "../_common"
require "faker"

def add_delete_flag(map)
  map["delete"] = true
  map
end

feature "Inventory Model Management" do
  ["group_manager", "customer"].each do |role|
    context "when interacting with inventory models in a specific inventory pool", driver: :selenium_headless do
      include_context :setup_models_api_model, role
      include_context :generate_session_header

      let(:pool_id) { @inventory_pool.id }
      let(:model_id) { @model.id }
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
        result = client.get "/inventory/manufacturers?type=Model"
        @form_manufacturer = result.body
        raise "Failed to fetch manufacturers" unless result.status == 200

        result = client.get "/inventory/#{pool_id}/entitlement-groups"
        @form_entitlement_groups = result.body
        raise "Failed to fetch entitlement groups" unless result.status == 200

        result = client.get "/inventory/models-compatibles"
        @form_models_compatibles = result.body["data"]
        raise "Failed to fetch compatible models" unless result.status == 200

        result = client.get "/inventory/#{pool_id}/model-groups"
        @form_model_groups = result.body
        raise "Failed to fetch model groups" unless result.status == 200
      end

      context "fetch form data" do
        it "ensures form manufacturer data is fetched" do
          expect(@form_manufacturer).not_to be_nil
          expect(@form_manufacturer.count).to eq(2)
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
          form_data = {"product" => Faker::Commerce.product_name}

          result = http_multipart_client(
            "/inventory/#{pool_id}/model",
            form_data,
            headers: cookie_header
          )
          expect(result.status).to eq(401)

          result = client.get "/inventory/#{pool_id}/model/#{model_id}"
          expect(result.status).to eq(401)

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
          expect(result.status).to eq(401)

          # fetch updated model
          result = client.get "/inventory/#{pool_id}/model/#{model_id}"
          expect(result.status).to eq(401)
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
            "manufacturer" => @form_manufacturer.first, # Use fetched manufacturer name
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

          expect(result.status).to eq(401)

          # fetch created model
          result = client.get "/inventory/#{pool_id}/model/#{model_id}"
          expect(result.status).to eq(401)

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
          expect(result.status).to eq(401)

          # fetch updated model
          result = client.get "/inventory/#{pool_id}/model/#{model_id}"
          expect(result.status).to eq(401)
        end
      end
    end
  end
end
