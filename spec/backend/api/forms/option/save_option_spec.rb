require "spec_helper"
require "pry"
require_relative "../../_shared"
require "faker"
require_relative "../_common"

response = {
  "id" => String,
  "inventory_code" => String,
  "product" => String,
  "version" => [NilClass, String],
  "name" => String,
  "price" => [NilClass, Numeric]
}

get_response = response.merge("is_deletable" => [TrueClass, FalseClass])

["inventory_manager", "lending_manager"].each do |role|
  describe "Inventory option" do
    context "when interacting with inventory option with role=#{role}" do
      include_context :setup_models_api_model, role
      include_context :generate_session_header

      let(:cookie_header) { @cookie_header }
      let(:client) { plain_faraday_json_client(cookie_header) }
      let(:pool_id) { @inventory_pool.id }

      context "create and update" do
        it "create, fetch & update with all fields" do
          # create option
          form_data = {
            product: Faker::Commerce.product_name,
            version: "v1",
            price: 999999.99,
            inventory_code: "O-1001"
          }

          resp = json_client_post(
            "/inventory/#{pool_id}/options/",
            body: form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(200)
          expect(validate_map_structure(resp.body, response)).to eq(true)
          expect(resp.body["id"]).to be_present
          option_id = resp.body["id"]

          # fetch option
          resp = client.get "/inventory/#{pool_id}/options/#{option_id}"
          expect(resp.status).to eq(200)
          expect(validate_map_structure(resp.body, get_response)).to eq(true)

          # update option
          form_data = {
            product: Faker::Commerce.product_name,
            inventory_code: "INV-1001",
            version: "v2",
            price: 0
          }

          resp = json_client_put(
            "/inventory/#{pool_id}/options/#{option_id}",
            body: form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(200)
          expect(validate_map_structure(resp.body, response)).to eq(true)
          expect(resp.body["version"]).to eq("v2")
          expect(resp.body["price"]).to eq(0)
        end

        it "create, fetch & update with required fields only" do
          # create option
          form_data = {
            product: Faker::Commerce.product_name,
            inventory_code: "O-1001"
          }

          resp = json_client_post(
            "/inventory/#{pool_id}/options/",
            body: form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(200)
          expect(validate_map_structure(resp.body, response)).to eq(true)
          expect(resp.body["id"]).to be_present
          option_id = resp.body["id"]

          # fetch option
          resp = client.get "/inventory/#{pool_id}/options/#{option_id}"
          expect(resp.status).to eq(200)
          expect(validate_map_structure(resp.body, get_response)).to eq(true)

          # update option
          form_data = {
            product: Faker::Commerce.product_name,
            inventory_code: "INV-1001"
          }

          resp = json_client_put(
            "/inventory/#{pool_id}/options/#{option_id}",
            body: form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(200)
          expect(validate_map_structure(resp.body, response)).to eq(true)
        end
      end

      context "price validation" do
        it "rejects requests with negative prices" do
          # create option
          form_data = {
            product: Faker::Commerce.product_name,
            version: "v1",
            price: -5,
            inventory_code: "O-1001"
          }

          resp = json_client_post(
            "/inventory/#{pool_id}/options/",
            body: form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(422)

          # update option
          option_id = FactoryBot.create(:option, inventory_pool_id: pool_id).id
          resp = json_client_put(
            "/inventory/#{pool_id}/options/#{option_id}",
            body: form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(422)
        end

        it "allow positive prices only" do
          [0, 1].each do |price|
            # create option
            form_data = {
              product: Faker::Commerce.product_name,
              version: "v1",
              price: price,
              inventory_code: Random.alphanumeric(5)
            }

            resp = json_client_post(
              "/inventory/#{pool_id}/options/",
              body: form_data,
              headers: cookie_header
            )
            expect(resp.status).to eq(200)

            # update option
            form_data["price"] = price * 2
            option_id = resp.body["id"]
            resp = json_client_put(
              "/inventory/#{pool_id}/options/#{option_id}",
              body: form_data,
              headers: cookie_header
            )
            expect(resp.status).to eq(200)
          end
        end
      end

      context "inventory_code uniqueness" do
        it "reject on create if inventory_code already exists" do
          form_data = {
            product: Faker::Commerce.product_name,
            inventory_code: "O-1001"
          }

          resp = json_client_post(
            "/inventory/#{pool_id}/options/",
            body: form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(200)

          resp = json_client_post(
            "/inventory/#{pool_id}/options/",
            body: form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(409)
        end

        it "reject on update if inventory_code already exists" do
          init_form_data = {
            product: Faker::Commerce.product_name,
            inventory_code: "O-1001"
          }

          resp = json_client_post(
            "/inventory/#{pool_id}/options/",
            body: init_form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(200)

          form_data = init_form_data.merge({inventory_code: "O-1002"})
          resp = json_client_post(
            "/inventory/#{pool_id}/options/",
            body: form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(200)
          option_id = resp.body["id"]

          resp = json_client_put(
            "/inventory/#{pool_id}/options/#{option_id}",
            body: init_form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(409)
        end
      end

      context "blank required fields" do
        [:product, :inventory_code].each do |field|
          ["", "  "].each do |blank|
            it "rejects POST and PUT with blank #{blank.inspect} #{field} with 422" do
              valid_data = {product: Faker::Commerce.product_name, inventory_code: Random.alphanumeric(5)}
              count_before = Option.count

              resp = json_client_post(
                "/inventory/#{pool_id}/options/",
                body: valid_data.merge(field => blank),
                headers: cookie_header
              )
              expect(resp.status).to eq(422)
              expect(Option.count).to eq(count_before)

              resp = json_client_post(
                "/inventory/#{pool_id}/options/",
                body: valid_data,
                headers: cookie_header
              )
              expect(resp.status).to eq(200)
              option_id = resp.body["id"]

              resp = json_client_put(
                "/inventory/#{pool_id}/options/#{option_id}",
                body: valid_data.merge(field => blank),
                headers: cookie_header
              )
              expect(resp.status).to eq(422)
              expect(Option.where(id: option_id).first[field]).to eq(valid_data[field])
            end
          end
        end
      end

      context "blank version" do
        ["", "  "].each do |blank|
          it "stores blank #{blank.inspect} version as NULL on create and update" do
            product = Faker::Commerce.product_name

            resp = json_client_post(
              "/inventory/#{pool_id}/options/",
              body: {product: product, version: blank, inventory_code: Random.alphanumeric(5)},
              headers: cookie_header
            )
            expect(resp.status).to eq(200)
            option_id = resp.body["id"]

            row = Option.where(id: option_id).first
            expect(row.version).to be_nil
            expect(row.name).to eq(product)

            resp = json_client_put(
              "/inventory/#{pool_id}/options/#{option_id}",
              body: {product: product, version: "v1", inventory_code: row.inventory_code},
              headers: cookie_header
            )
            expect(resp.status).to eq(200)
            expect(Option.where(id: option_id).first.version).to eq("v1")

            resp = json_client_put(
              "/inventory/#{pool_id}/options/#{option_id}",
              body: {product: product, version: blank, inventory_code: row.inventory_code},
              headers: cookie_header
            )
            expect(resp.status).to eq(200)

            row = Option.where(id: option_id).first
            expect(row.version).to be_nil
            expect(row.name).to eq(product)
          end
        end
      end
    end
  end
end
