require "spec_helper"
require "pry"
require_relative "../../_shared"
require "faker"

feature "Inventory Model Management" do
  context "when interacting with inventory models in a specific inventory pool", driver: :selenium_headless do
    include_context :setup_models_api_model
    include_context :setup_unknown_building_room_supplier
    include_context :generate_session_header

    let(:cookie_header) { @cookie_header }
    let(:client) { plain_faraday_json_client(cookie_header) }
    let(:pool_id) { @inventory_pool.id }

    let(:software_model) { @software_model }
    let(:license_item) { @license_item }
    let(:model_id) { @software_model.id }

    before do
      resp = client.get "/inventory/owners"
      @form_owners = resp.body
      raise "Failed to fetch manufacturers" unless resp.status == 200

      resp = client.get "/inventory/buildings"
      @form_buildings = resp.body
      raise "Failed to fetch entitlement groups" unless resp.status == 200

      resp = client.get "/inventory/rooms?building_id=#{@form_buildings[0]["id"]}"
      @form_rooms = resp.body
      raise "Failed to fetch entitlement groups" unless resp.status == 200

      resp = client.get "/inventory/manufacturers?type=Model&in-detail=true"
      @form_model_names = resp.body
      raise "Failed to fetch compatible models" unless resp.status == 200

      resp = client.get "/inventory/manufacturers?type=Model&in-detail=true&search-term=#{@form_model_names[0]["product"]}"
      @form_model_data = resp.body
      raise "Failed to fetch compatible models" unless resp.status == 200
    end

    context "create model" do
      it "fetch default" do
        result = client.get "/inventory/#{pool_id}/items-with-model-info?result_type=Normal"

        # FIXME: no result handling
        # result = client.get "/inventory/#{pool_id}/items-with-model-info?result_type=Normal&search_term=podest"

        expect(result.status).to eq(200)
        expect(result.body.count).to eq(1)
      end

      it "fetch default" do
        result = client.get "/inventory/#{pool_id}/package"

        expect(result.status).to eq(200)
        expect(result.body["data"]["inventory_pool_id"]).to eq(pool_id)
        expect(result.body["fields"].count).to eq(20)
      end

      it "fetch default" do
        result = client.get "/inventory/#{pool_id}/entitlement-groups"

        expect(result.status).to eq(200)
        expect(result.body.count).to eq(2)
      end

      it "fetch default" do
        result = client.get "/inventory/owners"

        expect(result.status).to eq(200)
        expect(result.body.count).to eq(2)
      end

      it "fetch default" do
        result = client.get "/inventory/buildings"

        expect(result.status).to eq(200)
        expect(result.body.count).to eq(3)
      end

      it "fetch default" do
        result = client.get "/inventory/manufacturers?type=Model&in-detail=true"

        expect(result.status).to eq(200)
        expect(result.body.count).to eq(2)
      end

      it "fetch by form data" do
        result = client.get "/inventory/#{pool_id}/package"

        expect(result.body["data"]["inventory_pool_id"]).to eq(pool_id)
        expect(result.body["fields"].count).to eq(20)
      end

      it "create, fetch & update by form data" do
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
        }.transform_values { |v| v.nil? ? "" : v.to_s }

        result = http_multipart_client(
          "/inventory/#{pool_id}/package",
          form_data,
          headers: cookie_header
        )
        binding.pry
        expect(result.status).to eq(200)
        expect(result.body["data"]).to be_present
        expect(result.body["validation"].count).to eq(0)

        item_id = result.body["data"]["id"]
        model_id = result.body["data"]["model_id"]

        # fetch package
        result = client.get "/inventory/#{pool_id}/models/#{model_id}/package/#{item_id}"
        expect(result.body["data"]).to be_present
        expect(result.body["fields"].count).to eq(20)

        # update package
        result = http_multipart_client(
          "/inventory/#{pool_id}/models/#{model_id}/package/#{item_id}",
          form_data,
          method: :put,
          headers: cookie_header
        )

        expect(result.status).to eq(200)
        expect(result.body["data"]).to be_present
        expect(result.body["validation"].count).to eq(0)
      end
    end
  end
end
