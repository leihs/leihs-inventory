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
      let(:pool) { @inventory_pool }
      let(:pool_id) { @inventory_pool.id }
      let(:model_id) { @models.first.id }

      def create_entitlement_group(body)
        json_client_post(
          "/inventory/#{pool_id}/entitlement-groups/",
          body: body,
          headers: cookie_header
        )
      end

      def filter_direct_entitlements(array, keys, type = "direct_entitlement")
        key_strings = keys.map(&:to_s)

        array
          .select { |h| h["type"] == type || h[:type] == type }
          .map { |h| h.select { |k, _| key_strings.include?(k.to_s) } }
      end

      def filter_entitlement(array, keys)
        key_strings = keys.map(&:to_s)

        array.map { |h| h.select { |k, _| key_strings.include?(k.to_s) } }
      end

      def expect_inventory_groups_and_users(pool_id, cookie_header, expected_groups:, expected_users:)
        groups_resp = json_client_get(
          "/inventory/#{pool_id}/groups/",
          headers: cookie_header
        )
        expect(groups_resp.status).to eq(200), "Expected 200 for groups but got #{groups_resp.status}"
        expect(groups_resp.body.count).to eq(expected_groups),
          "Expected #{expected_groups} groups but got #{groups_resp.body.count}"

        users_resp = json_client_get(
          "/inventory/#{pool_id}/users/",
          headers: cookie_header
        )
        expect(users_resp.status).to eq(200), "Expected 200 for users but got #{users_resp.status}"
        expect(users_resp.body.count).to eq(expected_users),
          "Expected #{expected_users} users but got #{users_resp.body.count}"

        {groups: groups_resp, users: users_resp}
      end

      context "test /groups and /users" do
        it "creates group with users" do
          expect_inventory_groups_and_users(
            pool_id,
            cookie_header,
            expected_groups: 1,
            expected_users: 1
          )

          group = FactoryBot.create(:group, name: "entitlement-group-#{Faker::Company.unique.name}")
          create_and_add_group_permission_for_x_users(pool, group, "inventory_manager", 3)

          expect_inventory_groups_and_users(
            pool_id,
            cookie_header,
            expected_groups: 2,
            expected_users: 4
          )
        end

        it "creates new users" do
          expect_inventory_groups_and_users(
            pool_id,
            cookie_header,
            expected_groups: 1,
            expected_users: 1
          )

          3.times.map do
            FactoryBot.create(:user, login: Faker::Name.unique.name, password: "password")
          end

          expect_inventory_groups_and_users(
            pool_id,
            cookie_header,
            expected_groups: 1,
            expected_users: 4
          )
        end
      end

      context "test /groups and /users" do
        let! :group1 do
          group = FactoryBot.create(:group, name: "entitlement-group1")
          create_and_add_group_permission_for_x_users(pool, group, "inventory_manager", 3)
          group
        end

        let! :group2 do
          group = FactoryBot.create(:group, name: "entitlement-group2")
          create_and_add_group_permission_for_x_users(pool, group, "inventory_manager", 5)
          group
        end

        let! :users do
          3.times.map do
            FactoryBot.create(:user, login: Faker::Name.unique.name, password: "password")
          end
        end

        it "creates new users" do
          resp = create_entitlement_group({
            entitlement_group: {name: Faker::Name.name, is_verification_required: true},
            users: [{user_id: users.first.id}, {user_id: users.second.id}],
            groups: [{group_id: group1.id}, {group_id: group2.id}],
            models: [{quantity: 20, model_id: @models.first.id}, {quantity: 30, model_id: @models.second.id}]
          })
          expect(resp.status).to eq(200)
          template_id = resp.body["entitlement_group"]["id"]

          resp = json_client_get(
            "/inventory/#{pool_id}/entitlement-groups/#{template_id}",
            headers: cookie_header
          )
          expect(resp.status).to eq(200)
          expect(resp.body["users"].count).to eq(10)
          expect(resp.body["groups"].count).to eq(2)
          expect(resp.body["models"].count).to eq(2)

          existing_users = filter_direct_entitlements(resp.body["users"], [:id, :user_id])
          existing_groups = filter_entitlement(resp.body["groups"], [:id, :group_id])
          existing_models = filter_entitlement(resp.body["models"], [:id, :model_id, :quantity])

          resp = json_client_put(
            "/inventory/#{pool_id}/entitlement-groups/#{template_id}",
            body: {
              entitlement_group: {name: "updated-name", is_verification_required: false},
              users: [existing_users.second],
              groups: [existing_groups.second],
              models: [existing_models.second]
            },
            headers: cookie_header
          )
          expect(resp.status).to eq(200)
          expect(resp.body["entitlement_group"]["name"]).to eq("updated-name")

          ["users", "groups"].each do |key|
            expect(resp.body[key]["created"].count).to eq(0)
            expect(resp.body[key]["deleted"].count).to eq(1)
          end
          expect(resp.body["models"]["updated"].count).to eq(1)
          expect(resp.body["models"]["created"].count).to eq(0)
          expect(resp.body["models"]["deleted"].count).to eq(1)
        end

        context "search fields" do
          it "allows deletion if no groups are referenced anymore" do
            resp = json_client_get(
              "/inventory/#{pool_id}/users/",
              headers: cookie_header
            )
            expect(resp.status).to eq(200)
            expect(resp.body.count).to eq(12)
          end

          it "allows deletion if no groups are referenced anymore" do
            resp = json_client_get(
              "/inventory/#{pool_id}/groups/",
              headers: cookie_header
            )
            expect(resp.status).to eq(200)
            expect(resp.body.count).to eq(3)
          end

          it "allows deletion if no groups are referenced anymore" do
            resp = json_client_get(
              "/inventory/#{pool_id}/models/",
              headers: cookie_header
            )
            expect(resp.status).to eq(200)
            expect(resp.body.count).to eq(13)
          end
        end

        context "try to delete" do
          let(:create_resp) do
            resp = create_entitlement_group({
              entitlement_group: {name: Faker::Name.name, is_verification_required: true},
              users: [{user_id: users.first.id}, {user_id: users.second.id}],
              groups: [{group_id: group1.id}, {group_id: group2.id}],
              models: [{quantity: 20, model_id: @models.first.id}, {quantity: 30, model_id: @models.second.id}]
            })
            expect(resp.status).to eq(200)
            template_id = resp.body["entitlement_group"]["id"]

            resp = json_client_get(
              "/inventory/#{pool_id}/entitlement-groups/#{template_id}",
              headers: cookie_header
            )
            expect(resp.status).to eq(200)
            resp
          end

          let(:existing_groups) do
            filter_direct_entitlements(create_resp.body["groups"], [:id, :group_id])
          end
          let(:template_id) do
            create_resp.body["entitlement_group"]["id"]
          end
          let(:existing_users) do
            filter_direct_entitlements(create_resp.body["groups"], [:id, :group_id])
          end
          let(:existing_models) do
            filter_direct_entitlements(create_resp.body["groups"], [:id, :group_id])
          end

          it "blocks deletion if groups/user/models are referenced" do
            resp = json_client_delete(
              "/inventory/#{pool_id}/entitlement-groups/#{template_id}",
              headers: cookie_header
            )
            expect(resp.status).to eq(409)
          end

          it "blocks deletion if models only are referenced" do
            json_client_put(
              "/inventory/#{pool_id}/entitlement-groups/#{template_id}",
              body: {
                entitlement_group: {name: "updated-name", is_verification_required: false},
                users: [],
                groups: [],
                models: existing_models
              },
              headers: cookie_header
            )
            resp = json_client_delete(
              "/inventory/#{pool_id}/entitlement-groups/#{template_id}",
              headers: cookie_header
            )
            expect(resp.status).to eq(200)
          end

          it "blocks deletion if users only are referenced" do
            json_client_put(
              "/inventory/#{pool_id}/entitlement-groups/#{template_id}",
              body: {
                entitlement_group: {name: "updated-name", is_verification_required: false},
                users: existing_users,
                groups: [],
                models: []
              },
              headers: cookie_header
            )
            resp = json_client_delete(
              "/inventory/#{pool_id}/entitlement-groups/#{template_id}",
              headers: cookie_header
            )
            expect(resp.status).to eq(200)
          end

          it "allow deletion if groups only are referenced" do
            json_client_put(
              "/inventory/#{pool_id}/entitlement-groups/#{template_id}",
              body: {
                entitlement_group: {name: "updated-name", is_verification_required: false},
                users: [],
                groups: existing_groups,
                models: []
              },
              headers: cookie_header
            )
            resp = json_client_delete(
              "/inventory/#{pool_id}/entitlement-groups/#{template_id}",
              headers: cookie_header
            )
            expect(resp.status).to eq(200)
          end
        end
      end
    end
  end
end
