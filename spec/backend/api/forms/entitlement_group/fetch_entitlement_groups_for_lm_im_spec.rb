require "spec_helper"
require_relative "../../_shared"
require "faker"
require_relative "../_common"

["inventory_manager"].each do |role|
  describe "Inventory templates API" do
    context "when interacting with inventory templates as inventory_manager" do
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
          # create
          resp = create_entitlement_group({entitlement_group: {name: Faker::Name.name, is_verification_required: true},
         models: [{quantity: 1, model_id: @models.first.id}],
                                          users: [],
                                          groups: []})
          expect(resp.status).to eq(200)
          expect(resp.body["models"].first["quantity"]).to eq(1)

          template_id = resp.body["entitlement_group"]["id"]
          model_id = resp.body["models"].first["id"]

          resp = json_client_put(
            "/inventory/#{pool_id}/entitlement-groups/#{template_id}",
            body: {entitlement_group: {name: "updated-name", is_verification_required: false},
                   users: [],
                   groups: [],
                   models: [{quantity: 2, model_id: @models.first.id, id: model_id}]},
            headers: cookie_header
          )
          expect(resp.status).to eq(200)
          expect(resp.body["models"]["updated"].first["quantity"]).to eq(2)
          expect(resp.body["models"]["created"].count).to eq(0)
          expect(resp.body["models"]["deleted"].count).to eq(0)
        end

        describe "fetch no entries" do
          it "without pagination" do
            resp = json_client_get(
              "/inventory/#{pool_id}/entitlement-groups/",
              headers: cookie_header
            )
            expect(resp.status).to eq(200)
            expect(resp.body.count).to eq(0)
          end

          it "with pagination" do
            resp = json_client_get(
              "/inventory/#{pool_id}/entitlement-groups/?page=1",
              headers: cookie_header
            )
            expect(resp.status).to eq(200)
            expect(resp.body["data"].count).to eq(0)
            expect(resp.body["pagination"]["total_rows"]).to eq(0)
          end
        end

        it "create and fetch (min)" do
          # create
          resp = create_entitlement_group({
            entitlement_group: {name: Faker::Name.name, is_verification_required: true},
            users: [],
            groups: [],
            models: []
          })
          expect(resp.status).to eq(200)
          expect(resp.body["models"].count).to eq(0)
          template_id = resp.body["entitlement_group"]["id"]

          resp = json_client_get(
            "/inventory/#{pool_id}/entitlement-groups/",
            headers: cookie_header
          )
          expect(resp.status).to eq(200)
          expect(resp.body.count).to eq(1)

          resp = json_client_get(
            "/inventory/#{pool_id}/entitlement-groups/#{template_id}",
            headers: cookie_header
          )
          expect(resp.status).to eq(200)
          expect(resp.body["entitlement_group"]["id"]).to eq(template_id)
          expect(resp.body["users"].count).to eq(0)
          expect(resp.body["groups"].count).to eq(0)
          expect(resp.body["models"].count).to eq(0)
        end

        it "creates and deletes a template" do
          resp = create_entitlement_group({entitlement_group: {name: Faker::Name.name, is_verification_required: true},
                                            users: [],
                                            groups: [],
                                            models: [{quantity: 1, model_id: @models.first.id}]})
          expect(resp.status).to eq(200)
          expect(resp.body["models"].first["quantity"]).to eq(1)

          template_id = resp.body["entitlement_group"]["id"]
          resp = json_client_delete(
            "/inventory/#{pool_id}/entitlement-groups/#{template_id}",
            headers: cookie_header
          )
          expect(resp.status).to eq(409)
        end

        describe "full-sync entitlements" do
          let :create_response do
            # create
            resp = create_entitlement_group({entitlement_group: {name: "updated-name", is_verification_required: false},
                                                            users: [],
                                                            groups: [],
                                              models: [
                                                {quantity: 1, model_id: @models.first.id},
                                                {quantity: 2, model_id: @models.second.id}
                                              ]})
            expect(resp.status).to eq(200)
            expect(resp.body["models"].first["quantity"]).to eq(1)
            expect(resp.body["models"].second["quantity"]).to eq(2)
            resp
          end

          it "to one model" do
            template_id = create_response.body["entitlement_group"]["id"]
            model_id = create_response.body["models"].first["id"]

            resp = json_client_put(
              "/inventory/#{pool_id}/entitlement-groups/#{template_id}",
              body: {
                entitlement_group: {name: "updated-name", is_verification_required: false},
                users: [],
                groups: [],
                models: [{quantity: 3, model_id: @models.first.id, id: model_id}]
              },
              headers: cookie_header
            )
            expect(resp.status).to eq(200)
            expect(resp.body["entitlement_group"]["name"]).to eq("updated-name")
            expect(resp.body["models"]["updated"].first["quantity"]).to eq(3)
            expect(resp.body["models"]["updated"].count).to eq(1)
            expect(resp.body["models"]["created"].count).to eq(0)
            expect(resp.body["models"]["deleted"].count).to eq(1)
          end

          it "to one model" do
            template_id = create_response.body["entitlement_group"]["id"]
            resp = json_client_put(
              "/inventory/#{pool_id}/entitlement-groups/#{template_id}",
              body: {
                entitlement_group: {name: "updated-name", is_verification_required: false},
                models: [],
                users: [],
                groups: []
              },
              headers: cookie_header
            )
            expect(resp.status).to eq(200)
            expect(resp.body["entitlement_group"]["name"]).to eq("updated-name")
            expect(resp.body["models"]["updated"].count).to eq(0)
            expect(resp.body["models"]["created"].count).to eq(0)
            expect(resp.body["models"]["deleted"].count).to eq(2)
          end
        end
      end
    end
  end
end
