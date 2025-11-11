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

      # -------------------------------------------------------
      # Compact filter examples (Semantik syntax)
      #
      # resp = client.get "/inventory/#{pool_id}/list/?filters=[{}]"
      # -------------------------------------------------------
      let(:filter_eq) { [{"inventory_code" => "=INV"}] }
      let(:filter_ilike) { [{"name" => "ilikeZoom"}] }
      let(:filter_not_ilike) { [{"name" => "not ilikeCanon"}] }

      let(:filter_in) { [{"manufacturer" => "in[Zoom,Sony]"}] }
      let(:filter_not_in) { [{"manufacturer" => "not in[Sony]"}] }

      let(:filter_gt) { [{"price" => ">100"}] }
      let(:filter_lt) { [{"price" => "<500"}] }
      let(:filter_isnull) { [{"room_id" => "isnull"}] }
      let(:filter_not_isnull) { [{"room_id" => "not isnull"}] }

      let(:filter_mixed_group) do
        [{
           "inventory_code" => "ilikeINV",
           "name" => "not ilikeTest"
         },{
             "price" => ">50"
          # ,
          #    "manufacturer" => "in[Sony,Zoom]"
          }]
      end

      # let(:filter_mixed_group) do
      #   [{"inventory_code" => "ilikeINV", "price" => ">50"}]
      # end

      # -------------------------------------------------------
      # Basic filter tests
      # -------------------------------------------------------
      describe "fetch data by using different filters" do
        it "accepts various valid filters" do
          [
            filter_eq,
            filter_ilike,
            filter_not_ilike,
            # filter_in,
            # filter_not_in,
            filter_gt,
            filter_lt,
            filter_isnull,
            filter_not_isnull,
            filter_mixed_group
          ].each do |filter_set|
            puts ">>> Testing filter-set: #{filter_set.to_json}"
            resp = client.get "/inventory/#{pool_id}/list/?filters=#{filter_set.to_json}"

            expect(resp.status).to eq(200), "Expected 200 but got #{resp.status} for filter #{filter_set}"
            # expect(resp.body).to be_a(Hash)
            # expect(resp.body).to include("pagination")
          end
        end

        it "fetches data as CSV" do
          resp = client.get "/inventory/#{pool_id}/list/?filters=#{filter_eq.to_json}" do |req|
            req.headers["Accept"] = "text/csv"
          end
          expect(resp.status).to eq(200)
        end

        it "fetches data as Excel (XLSX)" do
          # resp = client.get "/inventory/#{pool_id}/list/?filters=#{filter_eq.to_json}" do |req|
          resp = client.get "/inventory/#{pool_id}/list/" do |req|
            req.headers["Accept"] = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
          end
          expect(resp.status).to eq(200)
          # expect(resp.status).to eq(400) # FIXME: no result
        end
      end

      # -------------------------------------------------------
      # Validation / update tests
      # -------------------------------------------------------
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
            properties: {
              warranty_expiration: "2022-01-01",
              electrical_power: "10",
              ampere: "16"
            },
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
              # TODO: add additional item-fields once schema supports them
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
