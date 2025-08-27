require "spec_helper"
require "pry"
require_relative "../../_shared"
require_relative "../_common"
require "faker"

["lending_manager"].each do |role|
  describe "Inventory package" do
    context "when interacting with inventory package with role=#{role}" do
      include_context :setup_models_api_model, role
      include_context :setup_unknown_building_room_supplier
      include_context :generate_session_header

      let(:cookie_header) { @cookie_header }
      let(:client) { plain_faraday_json_client(cookie_header) }
      let(:pool_id) { @inventory_pool.id }
      let(:model_id) { @model.id }

      before do
        resp = client.get "/inventory/#{pool_id}/owners/"
        @form_owners = resp.body
        raise "Failed to fetch manufacturers" unless resp.status == 200

        resp = client.get "/inventory/#{pool_id}/buildings/"
        @form_buildings = resp.body
        raise "Failed to fetch entitlement groups" unless resp.status == 200

        resp = client.get "/inventory/#{pool_id}/rooms/?building_id=#{@form_buildings[0]["id"]}"
        @form_rooms = resp.body
        raise "Failed to fetch entitlement groups" unless resp.status == 200

        resp = client.get "/inventory/#{pool_id}/manufacturers/?type=Model&in-detail=true"
        @form_model_names = resp.body
        raise "Failed to fetch compatible models" unless resp.status == 200

        resp = client.get "/inventory/#{pool_id}/manufacturers/?type=Model&in-detail=true&search-term=#{@form_model_names[0]["product"]}"
        @form_model_data = resp.body
        raise "Failed to fetch compatible models" unless resp.status == 200
      end

      context "create model" do
        it "fetch default" do
          resp = client.get "/inventory/#{pool_id}/models/#{model_id}/packages/"

          expect(resp.status).to eq(200)
          expect(resp.body["data"]["inventory_pool_id"]).to eq(pool_id)
          expect(resp.body["fields"].count).to eq(15)
        end

        it "fetch default" do
          resp = client.get "/inventory/#{pool_id}/entitlement-groups/"

          expect(resp.status).to eq(200)
          expect(resp.body.count).to eq(2)
        end

        it "fetch default" do
          resp = client.get "/inventory/#{pool_id}/owners/"

          expect(resp.status).to eq(200)
          expect(resp.body.count).to eq(2)
        end

        it "fetch default" do
          resp = client.get "/inventory/#{pool_id}/buildings/"

          expect(resp.status).to eq(200)
          expect(resp.body.count).to eq(3)
        end

        it "fetch default" do
          resp = client.get "/inventory/#{pool_id}/manufacturers/?type=Model&in-detail=true"

          expect(resp.status).to eq(200)
          expect(resp.body.count).to eq(2)
        end

        it "fetch by form data" do
          resp = client.get "/inventory/#{pool_id}/models/#{model_id}/packages/"

          expect(resp.body["data"]["inventory_pool_id"]).to eq(pool_id)
          expect(resp.body["fields"].count).to eq(15)
        end

        it "blocks requests with invalid inventory_code" do
          # create package
          form_data = {
            is_inventory_relevant: true,
            last_check: nil,
            user_name: nil,
            price: 12.33,
            shelf: nil,
            inventory_code: "P-AUS00002",
            retired: false,
            is_broken: false,
            is_incomplete: false,
            is_borrowable: false,
            status_note: nil,
            note: nil,
            room_id: @form_rooms[0]["id"],
            model_id: @form_model_data[0]["id"],
            owner_id: @form_owners[0]["id"],
            items_attributes: []
          }

          resp = json_client_post(
            "/inventory/#{pool_id}/models/#{model_id}/packages/",
            body: form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(400)
        end

        it "create, fetch & update by form data" do
          # fetch init-data for package-form
          resp = client.get "/inventory/#{pool_id}/models/#{model_id}/packages/"
          inventory_code = resp.body["data"]["inventory_code"]
          expect(resp.status).to eq(200)

          # create package
          form_data = {
            is_inventory_relevant: true,
            last_check: nil,
            user_name: nil,
            price: 12.33,
            shelf: nil,
            inventory_code: inventory_code,
            retired: false,
            is_broken: false,
            is_incomplete: false,
            is_borrowable: false,
            status_note: nil,
            note: nil,
            room_id: @form_rooms[0]["id"],
            model_id: @form_model_data[0]["id"],
            owner_id: @form_owners[0]["id"],
            items_attributes: []
          }

          resp = json_client_post(
            "/inventory/#{pool_id}/models/#{model_id}/packages/",
            body: form_data,
            headers: cookie_header
          )

          expect(resp.status).to eq(200)
          expect(resp.body).to be_present

          item_id = resp.body["id"]
          model_id = resp.body["model_id"]

          # fetch package
          resp = client.get "/inventory/#{pool_id}/models/#{model_id}/packages/#{item_id}"
          expect(resp.body).to be_present

          # update package
          form_data = form_data.merge({price: 999999.99})
          resp = json_client_put(
            "/inventory/#{pool_id}/models/#{model_id}/packages/#{item_id}",
            body: form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(200)
          expect(resp.body).to be_present
        end
      end
    end
  end
end
