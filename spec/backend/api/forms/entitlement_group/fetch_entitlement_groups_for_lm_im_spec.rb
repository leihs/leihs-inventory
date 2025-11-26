require "spec_helper"
require_relative "../../_shared"
require "faker"
require_relative "../_common"

["inventory_manager"].each do |role|
  describe "Inventory entitlement-groups API" do
    context "when interacting with inventory entitlement-groups as inventory_manager" do
      include_context :setup_models_api_model, [role, false]
      include_context :generate_session_header

      let(:cookie_header) { @cookie_header }
      let(:client) { plain_faraday_json_client(cookie_header) }
      let(:pool_id) { @inventory_pool.id }
      let(:model_id) { @models.first.id }

      def create_entitlement_group(body)
        json_client_post(
          "/inventory/#{pool_id}/entitlement-groups/",
          body: body,
          headers: cookie_header
        )
      end

      describe "create / update / delete" do
        it "creates and updates quantity" do
          resp = create_entitlement_group({name: Faker::Name.name, is_verification_required: true,
         models: [{quantity: 1, id: @models.first.id}],
                                          users: [],
                                          groups: []})
          expect(resp.status).to eq(200)
          expect(resp.body["models"].first["quantity"]).to eq(1)

          eg_id = resp.body["id"]
          resp.body["models"].first["id"]

          resp = json_client_put(
            "/inventory/#{pool_id}/entitlement-groups/#{eg_id}",
            body: {name: "updated-name", is_verification_required: false,
                   users: [],
                   groups: [],
                   models: [{quantity: 2, id: @models.first.id}]},
            headers: cookie_header
          )
          expect(resp.status).to eq(200)
          expect(resp.body["models"].first["quantity"]).to eq(2)
          expect(resp.body["models"].count).to eq(1)
        end

        describe "fetch no entries" do
          it "without pagination" do
            resp = json_client_get(
              "/inventory/#{pool_id}/entitlement-groups/?type=all",
              headers: cookie_header
            )
            expect(resp.status).to eq(200)
            expect(resp.body.count).to eq(0)
          end

          it "with pagination" do
            resp = json_client_get(
              "/inventory/#{pool_id}/entitlement-groups/?type=all&page=1",
              headers: cookie_header
            )
            expect(resp.status).to eq(200)
            expect(resp.body["data"].count).to eq(0)
            expect(resp.body["pagination"]["total_rows"]).to eq(0)
          end
        end

        it "create and fetch (min)" do
          resp = create_entitlement_group({
            name: Faker::Name.name, is_verification_required: true,
            users: [],
            groups: [],
            models: [{quantity: 1, id: @models.first.id}]
          })
          expect(resp.status).to eq(200)
          expect(resp.body["models"].count).to eq(1)
          eg_id = resp.body["id"]

          resp = json_client_get(
            "/inventory/#{pool_id}/entitlement-groups/?type=all",
            headers: cookie_header
          )
          expect(resp.status).to eq(200)
          expect(resp.body.count).to eq(1)

          resp = json_client_get(
            "/inventory/#{pool_id}/entitlement-groups/#{eg_id}",
            headers: cookie_header
          )
          expect(resp.status).to eq(200)
          expect(resp.body["id"]).to eq(eg_id)
          expect(resp.body["users"].count).to eq(0)
          expect(resp.body["groups"].count).to eq(0)
          expect(resp.body["models"].count).to eq(1)
        end

        it "creates and deletes a template" do
          resp = create_entitlement_group({name: Faker::Name.name, is_verification_required: true,
                                            users: [],
                                            groups: [],
                                            models: [{quantity: 1, id: @models.first.id}]})
          expect(resp.status).to eq(200)
          expect(resp.body["models"].first["quantity"]).to eq(1)

          eg_id = resp.body["id"]
          resp = json_client_delete(
            "/inventory/#{pool_id}/entitlement-groups/#{eg_id}",
            headers: cookie_header
          )
          expect(resp.status).to eq(200)
        end

        describe "full-sync entitlements" do
          let :create_response do
            resp = create_entitlement_group({name: "updated-name", is_verification_required: false,
                                                            users: [],
                                                            groups: [],
                                              models: [
                                                {quantity: 1, id: @models.first.id},
                                                {quantity: 2, id: @models.second.id}
                                              ]})
            expect(resp.status).to eq(200)
            expect(resp.body["models"].first["quantity"]).to eq(1)
            expect(resp.body["models"].second["quantity"]).to eq(2)
            resp
          end

          it "to one model" do
            eg_id = create_response.body["id"]
            create_response.body["models"].first["id"]

            resp = json_client_put(
              "/inventory/#{pool_id}/entitlement-groups/#{eg_id}",
              body: {
                name: "updated-name", is_verification_required: false,
                users: [],
                groups: [],
                models: [{quantity: 3, id: @models.first.id}]
              },
              headers: cookie_header
            )
            expect(resp.status).to eq(200)
            expect(resp.body["name"]).to eq("updated-name")
            expect(resp.body["models"].first["quantity"]).to eq(3)
            expect(resp.body["models"].count).to eq(1)
          end

          it "to one model" do
            eg_id = create_response.body["id"]
            resp = json_client_put(
              "/inventory/#{pool_id}/entitlement-groups/#{eg_id}",
              body: {
                name: "updated-name", is_verification_required: false,
                models: [],
                users: [],
                groups: []
              },
              headers: cookie_header
            )
            expect(resp.status).to eq(400)
          end
        end
      end
    end
  end
end
