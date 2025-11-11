require "spec_helper"
require_relative "../../_shared"
require "faker"
require_relative "../_common"

# ["inventory_manager", "lending_manager"].each do |role|
["inventory_manager"].each do |role|
  describe "Inventory templates API" do
    context "when interacting with inventory advanced-search" do
      include_context :setup_models_api_model, role
      include_context :generate_session_header

      let(:cookie_header) { @cookie_header }
      let(:client) { plain_faraday_json_client(cookie_header) }
      let(:pool_id) { @inventory_pool.id }
      let(:model_id) { @models.first.id }

      let :fields do
        resp = client.get("/inventory/#{pool_id}/fields/?target_type=advanced_search")

        expect(resp.status).to eq(200)
        expect(resp.body["fields"].count).to eq(46)

        puts "FIELDS: #{resp.body["fields"].keys}"
        resp.body["fields"]
      end

      describe "fetch data by using different filters" do
        let :filter_set1 do
          [{"inventory_code" => "INV"}]
        end

        let :filter_set2 do
          [{"note" => "test", "inventory_code" => "INV"}, {"inventory_code" => "ABC"}]
        end

        it "returns all models used by model-selection" do
          [filter_set1, filter_set2].each do |filter_set|
            resp = client.get "/inventory/#{pool_id}/list/?filters=#{filter_set.to_json}"
            expect(resp.status).to eq(200)
            # expect(resp.body.count).to eq(12)
          end
        end

        it "fetch data as csv" do
          resp = client.get "/inventory/#{pool_id}/list/?filters=#{filter_set1.to_json}" do |req|
            req.headers["Accept"] = "text/csv"
          end
          expect(resp.status).to eq(200)
        end

        it "fetch data as excel" do
          # FIXME: resp = client.get "/inventory/#{pool_id}/list/?filters=#{filter_set1.to_json}" do |req|
          resp = client.get "/inventory/#{pool_id}/list/" do |req|
            req.headers["Accept"] = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
          end
          expect(resp.status).to eq(200)
        end
      end

      describe "validation" do
        before :each do
          @user, @user_cookies, @user_cookies_str, @cookie_token = create_and_login(:user)
          FactoryBot.create(:access_right,
            inventory_pool_id: @inventory_pool.id,
            user_id: @user.id,
            role: "inventory_manager")

          @model = FactoryBot.create(:leihs_model,
            product: "Test Product",
            is_package: false)

          @building = FactoryBot.create(:building, name: "Test Building")
          @room = FactoryBot.create(:room,
            name: "Test Room",
            building_id: @building.id)

          @item = FactoryBot.create(:item,
            inventory_code: "TEST-ORIGINAL",
            model_id: @model.id,
            room_id: @room.id,
            inventory_pool_id: @inventory_pool.id,
            owner_id: @inventory_pool.id)
        end

        it "bulk update of assigned items" do
          params = {
            "ids" => [@item.id],
            "data" => {
              "note" => "Updated note",
              "status_note" => "Updated status note",
              "properties_warranty_expiration" => "2020-02-02"
              # FIXME
              # "properties_electrical_power" => "5",
              # "properties_ampere" => "5"
              #   TODO: add remaining item-fields
            }
          }

          resp = json_client_patch(
            "/inventory/#{pool_id}/items/",
            body: params,
            headers: cookie_header
          )
          expect(resp.status).to eq(200)
        end
      end
    end
  end
end
