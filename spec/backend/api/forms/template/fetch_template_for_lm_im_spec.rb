require "spec_helper"
require_relative "../../_shared"
require "faker"
require_relative "../_common"

["inventory_manager", "lending_manager"].each do |role|
  describe "Inventory templates API" do
    context "when interacting with inventory templates as inventory_manager" do
      include_context :setup_models_api_model, role
      include_context :generate_session_header

      let(:cookie_header) { @cookie_header }
      let(:client) { plain_faraday_json_client(cookie_header) }
      let(:pool_id) { @inventory_pool.id }
      let(:model_id) { @models.first.id }

      def create_template(quantity:)
        json_client_post(
          "/inventory/#{pool_id}/templates/",
          body: {
            name: Faker::Commerce.product_name,
            models: [{quantity: quantity, id: @models.second.id}]
          },
          headers: cookie_header
        )
      end

      describe "fetching templates" do
        before do
          22.times { FactoryBot.create(:template, inventory_pool: @inventory_pool) }
        end

        it "returns all templates when no pagination is provided" do
          resp = client.get "/inventory/#{pool_id}/templates/"
          expect(resp.status).to eq(200)
          expect(resp.body.count).to eq(22)
        end

        it "returns all models used by model-selection" do
          resp = client.get "/inventory/#{pool_id}/list/?borrowable=true&type=model&retired=false"
          expect(resp.status).to eq(200)
          expect(resp.body.count).to eq(12)
        end

        it "paginates results" do
          resp = client.get "/inventory/#{pool_id}/templates/?size=5&page=2"

          expect(resp.status).to eq(200)
          expect(resp.body["data"].count).to eq(5)
          expect(resp.body["pagination"]["total_rows"]).to eq(22)
        end
      end

      describe "validation" do
        it "rejects requests with negative quantity" do
          params = {
            name: Faker::Commerce.product_name,
            quantity: -1,
            model_id: model_id
          }

          # create
          resp = json_client_post(
            "/inventory/#{pool_id}/templates/",
            body: params,
            headers: cookie_header
          )
          expect(resp.status).to eq(422)

          # update
          template_id = FactoryBot.create(:option, inventory_pool_id: pool_id).id
          resp = json_client_put(
            "/inventory/#{pool_id}/templates/#{template_id}",
            body: params,
            headers: cookie_header
          )
          expect(resp.status).to eq(422)
        end

        it "blocks updates with empty models" do
          resp = create_template(quantity: 0)
          expect(resp.status).to eq(200)
          expect(resp.body["models"].first["quantity"]).to eq(0)

          template_id = resp.body["id"]
          resp = json_client_put(
            "/inventory/#{pool_id}/templates/#{template_id}",
            body: {name: "updated-name", models: []},
            headers: cookie_header
          )

          expect(resp.status).to eq(400)
          expect(resp.body["details"]).to eq("Template must have at least one model")
        end
      end

      describe "create / update / delete" do
        it "creates and updates quantity" do
          # create
          resp = create_template(quantity: 0)
          expect(resp.status).to eq(200)
          expect(resp.body["models"].first["quantity"]).to eq(0)

          # update
          template_id = resp.body["id"]
          created_model_id = resp.body["models"].first["id"]

          resp = json_client_put(
            "/inventory/#{pool_id}/templates/#{template_id}",
            body: {
              name: "updated-name",
              models: [{id: created_model_id, quantity: 33}]
            },
            headers: cookie_header
          )
          expect(resp.status).to eq(200)
        end

        it "creates and deletes a template" do
          resp = create_template(quantity: 3)
          expect(resp.status).to eq(200)
          expect(resp.body["models"].first["quantity"]).to eq(3)

          template_id = resp.body["id"]
          resp = json_client_delete(
            "/inventory/#{pool_id}/templates/#{template_id}",
            headers: cookie_header
          )
          expect(resp.status).to eq(200)
        end

        it "full-syncs models on update" do
          resp = create_template(quantity: 0)
          expect(resp.status).to eq(200)
          expect(resp.body["models"].first["quantity"]).to eq(0)

          template_id = resp.body["id"]

          # creates 2 new model entries
          resp = json_client_put(
            "/inventory/#{pool_id}/templates/#{template_id}",
            body: {
              name: "template-with-2-created-models",
              models: [
                {quantity: 11, id: @models.second.id},
                {quantity: 22, id: @models.first.id}
              ]
            },
            headers: cookie_header
          )

          expect(resp.status).to eq(200)
          expect(resp.body["models"].count).to eq(2)

          # sync to one model entry
          resp = json_client_put(
            "/inventory/#{pool_id}/templates/#{template_id}",
            body: {
              name: "template-with-1-updated-model",
              models: [{quantity: 15, id: @models.second.id}]
            },
            headers: cookie_header
          )
          expect(resp.status).to eq(200)
          expect(resp.body["models"].count).to eq(1)
        end
      end

      describe "coercion of template" do
        include_context :setup_template_with_model

        let(:expected_status) { 200 }

        it "allows index, pagination, create, update, and delete" do
          endpoints = [
            [:get, "/inventory/#{pool_id}/templates/"],
            [:get, "/inventory/#{pool_id}/templates/?size=5&page=2"],
            [:post, "/inventory/#{pool_id}/templates/", {name: Faker::Commerce.product_name, models: [{id: model_id, quantity: 15}]}],
            [:post, "/inventory/#{pool_id}/templates/", {name: Faker::Commerce.product_name, models: [{id: model_id, quantity: 0}]}],
            [:put, "/inventory/#{pool_id}/templates/#{template_id}", {name: "updated-name", models: [{id: model_id, quantity: 0}]}],
            [:put, "/inventory/#{pool_id}/templates/#{template_id}", {name: "updated-name", models: [{id: model_id, quantity: 15}]}],
            [:delete, "/inventory/#{pool_id}/templates/#{template_id}", nil]
          ]

          endpoints.each do |method, path, body|
            resp = call(method, path, body: body)
            expect(resp.status).to eq(expected_status),
              "Expected #{method.upcase} #{path} => #{expected_status}, got #{resp.status} (body: #{resp.body.inspect})"
          end
        end

        it "denies create, update caused by invalid coercion" do
          endpoints = [
            [:post, "/inventory/#{pool_id}/templates/", {name: Faker::Commerce.product_name, models: [{id: model_id, quantity: -1}]}],
            [:post, "/inventory/#{pool_id}/templates/", {name: "", models: [{id: model_id, quantity: 0}]}],
            [:put, "/inventory/#{pool_id}/templates/#{template_id}", {name: "updated-name", models: [{id: model_id, quantity: -1}]}],
            [:put, "/inventory/#{pool_id}/templates/#{template_id}", {name: "", models: [{id: model_id, quantity: 0}]}]
          ]

          endpoints.each do |method, path, body|
            resp = call(method, path, body: body)
            expect(resp.status).to eq(422),
              "Expected #{method.upcase} #{path} => #{expected_status}, got #{resp.status} (body: #{resp.body.inspect})"
          end
        end
      end
    end
  end
end
