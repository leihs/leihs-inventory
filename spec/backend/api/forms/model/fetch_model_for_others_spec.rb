require "spec_helper"
require "pry"
require_relative "../../_shared"
require_relative "../_common"
require "faker"

def compact_compatibles(compatibles)
  compatibles.map do |item|
    item = item.compact
    item["id"] ||= item.delete("model_id")
    item
  end
end

describe "Inventory Model" do
  ["group_manager", "customer"].each do |role|
    context "when interacting with inventory model with role=#{role}" do
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
        resp = client.get "/inventory/manufacturers?type=Model"
        @form_manufacturers = resp.body
        raise "Failed to fetch manufacturers" unless resp.status == 200

        resp = client.get "/inventory/#{pool_id}/entitlement-groups"
        @form_entitlement_groups = resp.body
        raise "Failed to fetch entitlement groups" unless resp.status == 200

        resp = client.get "/inventory/models-compatibles"
        @form_models_compatibles = resp.body
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
          expect(@form_models_compatibles.count).to eq(LeihsModel.count)
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
          resp = json_client_post(
            "/inventory/#{pool_id}/model/",
            body: form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(401)

          resp = client.get "/inventory/#{pool_id}/model/#{model_id}"
          expect(resp.status).to eq(401)

          # update model request
          form_data = {
            "product" => "updated product"
          }

          resp = json_client_put(
            "/inventory/#{pool_id}/model/#{model_id}/",
            body: form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(401)

          # fetch updated model
          resp = client.get "/inventory/#{pool_id}/model/#{model_id}"
          expect(resp.status).to eq(401)
        end
      end

      context "create model" do
        it "creates a model with all available attributes" do
          compatibles = @form_models_compatibles
          compatibles.first["id"] = compatibles.first.delete("model_id")

          # create model request
          form_data = {
            "product" => Faker::Commerce.product_name,
            "version" => "v1.0",
            "manufacturer" => @form_manufacturers.first, # Use fetched manufacturer name
            "is_package" => true,
            "description" => "A sample product",
            "technical_details" => "Specs go here",
            "internal_description" => "Internal notes",
            "important_notes" => "Important usage notes",
            "entitlements" => [{group_id: @form_entitlement_groups.first["id"], quantity: 33}],
            "compatibles" => [compatibles.first],
            "categories" => [@form_model_groups.first]
          }

          resp = json_client_post(
            "/inventory/#{pool_id}/model/",
            body: form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(401)

          # fetch created model
          resp = client.get "/inventory/#{pool_id}/model/#{model_id}"
          expect(resp.status).to eq(401)

          # update model request
          form_data = {
            "product" => "updated product",
            "version" => "updated v1.0",
            "manufacturer" => "updated manufacturer",
            "is_package" => true,
            "description" => "updated description",
            "technical_details" => "updated techDetail",
            "internal_description" => "updated internalDesc",
            "important_notes" => "updated notes",
            "entitlements" => [{group_id: @form_entitlement_groups.first["id"], quantity: 11}],
            "compatibles" => compact_compatibles([compatibles.first, compatibles.second]),
            "categories" => [@form_model_groups.first, @form_model_groups.second]
          }

          resp = json_client_put(
            "/inventory/#{pool_id}/model/#{model_id}/",
            body: form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(401)

          # fetch updated model
          resp = client.get "/inventory/#{pool_id}/model/#{model_id}"
          expect(resp.status).to eq(401)
        end
      end
    end
  end
end
