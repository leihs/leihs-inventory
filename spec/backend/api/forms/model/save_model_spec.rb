require "spec_helper"
require "pry"
require_relative "../../_shared"
require "faker"

describe "Inventory Model" do
  ["inventory_manager", "lending_manager"].each do |role|
    context "when interacting with inventory model with role=#{role}" do
      include_context :setup_models_api_model, role
      include_context :generate_session_header

      let(:pool_id) { @inventory_pool.id }
      let(:cookie_header) { @cookie_header }

      context "create model duplicates" do
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

        it "blocks update of model to the name of an existing model" do
          product = Faker::Commerce.product_name
          resp = json_client_post(
            "/inventory/#{pool_id}/models/",
            body: {"product" => product, "version" => "1.0"},
            headers: cookie_header
          )
          expect(resp.status).to eq(200)

          resp = json_client_post(
            "/inventory/#{pool_id}/models/",
            body: {"product" => product, "version" => "2.0"},
            headers: cookie_header
          )
          expect(resp.status).to eq(200)
          model_id = resp.body["id"]

          resp = json_client_put(
            "/inventory/#{pool_id}/models/#{model_id}",
            body: {
              "product" => product,
              "version" => "1.0",
              "is_package" => false,
              "categories" => [],
              "compatibles" => [],
              "entitlements" => [],
              "properties" => [],
              "accessories" => []
            },
            headers: cookie_header
          )
          expect(resp.status).to eq(409)
          expect(resp.body["message"]).to eq("Failed to update model")
        end

        it "rejects update with an empty property key and returns 400" do
          resp = json_client_post(
            "/inventory/#{pool_id}/models/",
            body: {"product" => Faker::Commerce.product_name},
            headers: cookie_header
          )
          expect(resp.status).to eq(200)

          resp = json_client_put(
            "/inventory/#{pool_id}/models/#{resp.body["id"]}",
            body: {
              "product" => Faker::Commerce.product_name,
              "is_package" => false,
              "categories" => [],
              "compatibles" => [],
              "entitlements" => [],
              "properties" => [{"key" => "", "value" => "x"}],
              "accessories" => []
            },
            headers: cookie_header
          )
          expect(resp.status).to eq(400)
          expect(resp.body["message"]).to eq("Field ':key' cannot be an empty string.")
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

      context "blank text fields" do
        let(:relations) do
          {
            "is_package" => false,
            "categories" => [],
            "compatibles" => [],
            "entitlements" => [],
            "properties" => [],
            "accessories" => []
          }
        end

        ["", "  "].each do |blank|
          it "blocks creation of model with blank version #{blank.inspect} when same product without version exists" do
            product = Faker::Commerce.product_name

            resp = json_client_post(
              "/inventory/#{pool_id}/models/",
              body: {"product" => product},
              headers: cookie_header
            )
            expect(resp.status).to eq(200)

            resp = json_client_post(
              "/inventory/#{pool_id}/models/",
              body: {"product" => product, "version" => blank},
              headers: cookie_header
            )
            expect(resp.status).to eq(409)
            expect(LeihsModel.where(product: product).count).to eq(1)
          end

          it "POST with blank #{blank.inspect} optional text fields stores NULL" do
            resp = json_client_post(
              "/inventory/#{pool_id}/models/",
              body: {
                "product" => "Model-Blank-Post",
                "manufacturer" => blank,
                "version" => blank,
                "description" => blank,
                "technical_detail" => blank,
                "hand_over_note" => blank,
                "internal_description" => blank
              }.merge(relations),
              headers: cookie_header
            )
            expect(resp.status).to eq(200)

            row = LeihsModel.where(type: "Model", id: resp.body["id"]).first
            expect(row.manufacturer).to be_nil
            expect(row.version).to be_nil
            expect(row.description).to be_nil
            expect(row.technical_detail).to be_nil
            expect(row.hand_over_note).to be_nil
            expect(row.internal_description).to be_nil
            expect(row.name).to eq("Model-Blank-Post")
          end

          it "PUT with blank #{blank.inspect} optional text fields clears them to NULL" do
            create_resp = json_client_post(
              "/inventory/#{pool_id}/models/",
              body: {
                "product" => "Model-Blank-Put",
                "manufacturer" => "Example Corp",
                "version" => "1.0",
                "description" => "Desc",
                "technical_detail" => "Specs",
                "hand_over_note" => "Note",
                "internal_description" => "Internal"
              }.merge(relations),
              headers: cookie_header
            )
            expect(create_resp.status).to eq(200)
            model_id = create_resp.body["id"]

            put_resp = json_client_put(
              "/inventory/#{pool_id}/models/#{model_id}",
              body: {
                "product" => "Model-Blank-Put",
                "manufacturer" => blank,
                "version" => blank,
                "description" => blank,
                "technical_detail" => blank,
                "hand_over_note" => blank,
                "internal_description" => blank
              }.merge(relations),
              headers: cookie_header
            )
            expect(put_resp.status).to eq(200)

            row = LeihsModel.where(type: "Model", id: model_id).first
            expect(row.manufacturer).to be_nil
            expect(row.version).to be_nil
            expect(row.description).to be_nil
            expect(row.technical_detail).to be_nil
            expect(row.hand_over_note).to be_nil
            expect(row.internal_description).to be_nil
            expect(row.name).to eq("Model-Blank-Put")
          end

          it "rejects POST and PUT with blank #{blank.inspect} product with 422" do
            count_before = LeihsModel.count

            resp = json_client_post(
              "/inventory/#{pool_id}/models/",
              body: {"product" => blank},
              headers: cookie_header
            )
            expect(resp.status).to eq(422)
            expect(LeihsModel.count).to eq(count_before)

            create_resp = json_client_post(
              "/inventory/#{pool_id}/models/",
              body: {"product" => "Model-Blank-Product"}.merge(relations),
              headers: cookie_header
            )
            expect(create_resp.status).to eq(200)
            model_id = create_resp.body["id"]

            resp = json_client_put(
              "/inventory/#{pool_id}/models/#{model_id}",
              body: {"product" => blank}.merge(relations),
              headers: cookie_header
            )
            expect(resp.status).to eq(422)
            expect(LeihsModel.where(id: model_id).first.product).to eq("Model-Blank-Product")
          end
        end
      end
    end
  end
end
