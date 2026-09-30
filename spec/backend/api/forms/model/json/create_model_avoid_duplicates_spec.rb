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

      before do
        # Fetch shared data and set global instance variables
        resp = client.get "/inventory/#{pool_id}/manufacturers/?type=Model"
        @form_manufacturers = resp.body
        raise "Failed to fetch manufacturers" unless resp.status == 200

        resp = client.get "/inventory/#{pool_id}/entitlement-groups/"
        @form_entitlement_groups = resp.body
        raise "Failed to fetch entitlement groups" unless resp.status == 200

        resp = client.get "/inventory/#{pool_id}/models/"
        @form_models_compatibles = resp.body.map do |h|
          h.select { |k, _v| k == "id" || k == "product" }
        end
        raise "Failed to fetch compatible models" unless resp.status == 200

        resp = client.get "/inventory/#{pool_id}/category-tree/"
        @form_model_groups = extract_first_level_of_tree(resp.body)
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

      def convert_to_id_correction(compatibles)
        compatibles.each do |compatible|
          # puts "before: #{compatible}"
          compatible["id"] = compatible.delete("model_id")
          # puts "after: #{compatible}\n\n"
        end
      end

      context "create model (min)" do
        it "blocks creation of model when already exists (product_name)" do
          # create model request
          product = Faker::Commerce.product_name
          form_data = {
            "product" => product
          }

          resp = json_client_post(
            "/inventory/#{pool_id}/models/",
            body: form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(200)

          # retry to create model request
          resp = json_client_post(
            "/inventory/#{pool_id}/models/",
            body: form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(409)
          expect(resp.body["message"]).to eq("Failed to create model")

          # create model with different version
          form_data = {
            "product" => product,
            "version" => "1.0"
          }

          resp = json_client_post(
            "/inventory/#{pool_id}/models/",
            body: form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(200)
        end
      end

      context "create model (min)" do
        it "blocks creation of model when already exists (product_name & version)" do
          # create model request
          product = Faker::Commerce.product_name
          form_data = {
            "product" => product,
            "version" => "1.0"
          }

          resp = json_client_post(
            "/inventory/#{pool_id}/models/",
            body: form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(200)

          # retry to create model request
          resp = json_client_post(
            "/inventory/#{pool_id}/models/",
            body: form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(409)
          expect(resp.body["message"]).to eq("Failed to create model")

          # create model with different version
          form_data = {
            "product" => product,
            "version" => "2.0"
          }

          resp = json_client_post(
            "/inventory/#{pool_id}/models/",
            body: form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(200)
        end
      end

      context "update model nil handling" do
        it "PUT with explicit nil in optional text fields does not wipe existing values" do
          create_resp = json_client_post(
            "/inventory/#{pool_id}/models/",
            body: {
              "product" => "Model-Nil-Guard",
              "manufacturer" => "Example Corp",
              "version" => "1.0",
              "description" => "Desc",
              "hand_over_note" => "Note",
              "internal_description" => "Internal",
              "is_package" => false,
              "categories" => [],
              "compatibles" => [],
              "entitlements" => [],
              "properties" => [],
              "accessories" => []
            },
            headers: cookie_header
          )
          expect(create_resp.status).to eq(200)
          model_id = create_resp.body["id"]

          put_resp = json_client_put(
            "/inventory/#{pool_id}/models/#{model_id}",
            body: {
              "product" => "Model-Nil-Guard-Renamed",
              "manufacturer" => nil,
              "version" => nil,
              "description" => nil,
              "hand_over_note" => nil,
              "internal_description" => nil,
              "is_package" => false,
              "categories" => [],
              "compatibles" => [],
              "entitlements" => [],
              "properties" => [],
              "accessories" => []
            },
            headers: cookie_header
          )
          expect(put_resp.status).to eq(200)

          row = LeihsModel.where(type: "Model", id: model_id).first
          expect(row.product).to eq("Model-Nil-Guard-Renamed")
          expect(row.manufacturer).to eq("Example Corp")
          expect(row.version).to eq("1.0")
          expect(row.description).to eq("Desc")
          expect(row.hand_over_note).to eq("Note")
          expect(row.internal_description).to eq("Internal")
        end

        it "PUT omitting optional text fields keeps existing values unchanged" do
          create_resp = json_client_post(
            "/inventory/#{pool_id}/models/",
            body: {
              "product" => "Model-Omitted-Guard",
              "manufacturer" => "Example Corp",
              "version" => "2.0",
              "description" => "Desc",
              "hand_over_note" => "Note",
              "internal_description" => "Internal",
              "is_package" => false,
              "categories" => [],
              "compatibles" => [],
              "entitlements" => [],
              "properties" => [],
              "accessories" => []
            },
            headers: cookie_header
          )
          expect(create_resp.status).to eq(200)
          model_id = create_resp.body["id"]

          put_resp = json_client_put(
            "/inventory/#{pool_id}/models/#{model_id}",
            body: {
              "product" => "Model-Omitted-Guard-Renamed",
              "is_package" => false,
              "categories" => [],
              "compatibles" => [],
              "entitlements" => [],
              "properties" => [],
              "accessories" => []
            },
            headers: cookie_header
          )
          expect(put_resp.status).to eq(200)

          row = LeihsModel.where(type: "Model", id: model_id).first
          expect(row.product).to eq("Model-Omitted-Guard-Renamed")
          expect(row.manufacturer).to eq("Example Corp")
          expect(row.version).to eq("2.0")
          expect(row.description).to eq("Desc")
          expect(row.hand_over_note).to eq("Note")
          expect(row.internal_description).to eq("Internal")
        end

        it "PUT with a single explicit nil field does not wipe stored text values" do
          create_resp = json_client_post(
            "/inventory/#{pool_id}/models/",
            body: {
              "product" => "Model-Single-Nil-Guard",
              "manufacturer" => "Example Corp",
              "version" => "3.0",
              "description" => "Desc",
              "hand_over_note" => "Note",
              "internal_description" => "Internal",
              "is_package" => false,
              "categories" => [],
              "compatibles" => [],
              "entitlements" => [],
              "properties" => [],
              "accessories" => []
            },
            headers: cookie_header
          )
          expect(create_resp.status).to eq(200)
          model_id = create_resp.body["id"]

          put_resp = json_client_put(
            "/inventory/#{pool_id}/models/#{model_id}",
            body: {
              "product" => "Model-Single-Nil-Guard-Renamed",
              "hand_over_note" => nil,
              "is_package" => false,
              "categories" => [],
              "compatibles" => [],
              "entitlements" => [],
              "properties" => [],
              "accessories" => []
            },
            headers: cookie_header
          )
          expect(put_resp.status).to eq(200)

          row = LeihsModel.where(type: "Model", id: model_id).first
          expect(row.product).to eq("Model-Single-Nil-Guard-Renamed")
          expect(row.manufacturer).to eq("Example Corp")
          expect(row.version).to eq("3.0")
          expect(row.description).to eq("Desc")
          expect(row.hand_over_note).to eq("Note")
          expect(row.internal_description).to eq("Internal")
        end
      end
    end
  end
end
