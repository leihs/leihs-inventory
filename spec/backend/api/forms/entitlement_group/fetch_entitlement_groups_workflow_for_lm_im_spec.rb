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

      def filter_entitlement_by_key(array, key)
        array.map { |h| h[key] || h[key.to_s] }
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
            name: Faker::Name.name,
            is_verification_required: true,
            users: [users.first.id, users.second.id],
            groups: [group1.id, group2.id],
            models: [{quantity: 20, id: @models.first.id}, {quantity: 30, id: @models.second.id}]
          })
          expect(resp.status).to eq(200)
          eg_id = resp.body["id"]

          resp = json_client_get(
            "/inventory/#{pool_id}/entitlement-groups/#{eg_id}",
            headers: cookie_header
          )
          expect(resp.status).to eq(200)
          expect(resp.body["users"].count).to eq(10)
          expect(resp.body["groups"].count).to eq(2)
          expect(resp.body["models"].count).to eq(2)

          existing_models = filter_entitlement(resp.body["models"], [:id, :model_id, :quantity])

          resp = json_client_put(
            "/inventory/#{pool_id}/entitlement-groups/#{eg_id}",
            body: {
              name: "updated-name",
              is_verification_required: false,
              users: [users.first.id],
              groups: [group2.id],
              models: [existing_models.second]
            },
            headers: cookie_header
          )
          expect(resp.status).to eq(200)
          expect(resp.body["name"]).to eq("updated-name")

          expect(resp.body["users"].count).to eq(6)
          expect(resp.body["groups"].count).to eq(1)
          expect(resp.body["models"].count).to eq(1)
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
              name: Faker::Name.name,
              is_verification_required: true,
              users: [users.first.id, users.second.id],
              groups: [group1.id, group2.id],
              models: [{quantity: 20, id: @models.first.id}, {quantity: 30, id: @models.second.id}]
            })

            expect(resp.status).to eq(200)
            eg_id = resp.body["id"]

            resp = json_client_get(
              "/inventory/#{pool_id}/entitlement-groups/#{eg_id}",
              headers: cookie_header
            )
            expect(resp.status).to eq(200)
            resp
          end

          let(:existing_groups) do
            create_resp.body["groups"]
          end

          let(:eg_id) do
            create_resp.body["id"]
          end

          let(:existing_users) do
            create_resp.body["users"]
          end

          let(:existing_models) do
            models = create_resp.body["models"]
            models.map { |h| h.slice("id", "quantity") }
          end

          it "allow deletion if groups/user/models are referenced" do
            resp = json_client_delete(
              "/inventory/#{pool_id}/entitlement-groups/#{eg_id}",
              headers: cookie_header
            )
            expect(resp.status).to eq(200)
          end

          it "blocks deletion if models only are referenced" do
            resp = json_client_put(
              "/inventory/#{pool_id}/entitlement-groups/#{eg_id}",
              body: {
                name: "updated-name",
                is_verification_required: false,
                users: [],
                groups: [],
                models: existing_models
              },
              headers: cookie_header
            )
            expect(resp.status).to eq(200)

            resp = json_client_delete(
              "/inventory/#{pool_id}/entitlement-groups/#{eg_id}",
              headers: cookie_header
            )
            expect(resp.status).to eq(200)
          end

          context "search for user" do
            it "without pagination" do
              resp = json_client_get(
                "/inventory/#{pool_id}/users/?search=#{existing_users.first["lastname"]}",
                headers: cookie_header
              )
              expect(resp.status).to eq(200)
              expect(resp.body.count).to eq(1)
            end

            it "with pagination" do
              resp = json_client_get(
                "/inventory/#{pool_id}/users/?page=1&search=#{existing_users.first["lastname"]}",
                headers: cookie_header
              )
              expect(resp.status).to eq(200)
              expect(resp.body["data"].count).to eq(1)
            end
          end

          it "blocks deletion if users only are referenced" do
            resp = json_client_get(
              "/inventory/#{pool_id}/users/?search=#{existing_users.first["lastname"]}",
              headers: cookie_header
            )
            expect(resp.status).to eq(200)

            resp = json_client_put(
              "/inventory/#{pool_id}/entitlement-groups/#{eg_id}",
              body: {
                name: "updated-name",
                is_verification_required: false,
                users: [existing_users.first["id"]],
                groups: [],
                models: [existing_models.second]
              },
              headers: cookie_header
            )
            expect(resp.status).to eq(200)

            resp = json_client_delete(
              "/inventory/#{pool_id}/entitlement-groups/#{eg_id}",
              headers: cookie_header
            )
            expect(resp.status).to eq(200)
          end

          context "search for group" do
            it "without pagination" do
              resp = json_client_get(
                "/inventory/#{pool_id}/groups/?search=#{existing_groups.first["name"]}",
                headers: cookie_header
              )
              expect(resp.status).to eq(200)
              expect(resp.body.count).to eq(1)
            end

            it "with pagination" do
              resp = json_client_get(
                "/inventory/#{pool_id}/groups/?page=1&search=#{existing_groups.first["name"]}",
                headers: cookie_header
              )
              expect(resp.status).to eq(200)
              expect(resp.body["data"].count).to eq(1)
            end
          end

          it "allow deletion if groups only are referenced" do
            resp = json_client_put(
              "/inventory/#{pool_id}/entitlement-groups/#{eg_id}",
              body: {
                name: "updated-name",
                is_verification_required: false,
                users: [],
                groups: [existing_groups.first["id"]],
                models: [existing_models.first]
              },
              headers: cookie_header
            )
            expect(resp.status).to eq(200)

            resp = json_client_delete(
              "/inventory/#{pool_id}/entitlement-groups/#{eg_id}",
              headers: cookie_header
            )
            expect(resp.status).to eq(200)
          end

          it "block requests with no model" do
            resp = json_client_put(
              "/inventory/#{pool_id}/entitlement-groups/#{eg_id}",
              body: {
                name: "updated-name",
                is_verification_required: false,
                users: [],
                groups: [existing_groups.first["id"]],
                models: []
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
