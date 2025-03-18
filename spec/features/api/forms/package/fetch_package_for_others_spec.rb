require "spec_helper"
require "pry"
require_relative "../../_shared"
require "faker"

["group_manager", "customer"].each do |role|
  feature "Inventory package" do
    context "when interacting with inventory package with role=#{role}", driver: :selenium_headless do
      include_context :setup_models_api_model, role
      include_context :setup_unknown_building_room_supplier
      include_context :generate_session_header

      let(:cookie_header) { @cookie_header }
      let(:client) { plain_faraday_json_client(cookie_header) }
      let(:pool) { @inventory_pool }
      let(:pool_id) { @inventory_pool.id }

      let(:fake_package) { FactoryBot.create(:package_model_with_items, inventory_pool: pool) }

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

      it "create, fetch & update by form data" do
        # create package, works
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

        resp = http_multipart_client(
          "/inventory/#{pool_id}/package",
          form_data,
          headers: cookie_header
        )
        expect(resp.status).to eq(401)

        model_id = fake_package.id
        item_id = fake_package.items.first.id
        pool_id = fake_package.items.first.inventory_pool_id

        # fetch package
        resp = client.get "/inventory/#{pool_id}/models/#{model_id}/package/#{item_id}"
        expect(resp.status).to eq(401)

        # update package
        resp = http_multipart_client(
          "/inventory/#{pool_id}/models/#{model_id}/package/#{item_id}",
          form_data,
          method: :put,
          headers: cookie_header
        )
        expect(resp.status).to eq(400)
      end
    end
  end
end
