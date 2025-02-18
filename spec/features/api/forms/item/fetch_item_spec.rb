require "spec_helper"
require "pry"
require_relative "../../_shared"
require "faker"

feature "Inventory Model Management" do
  context "when interacting with inventory models in a specific inventory pool", driver: :selenium_headless do
    include_context :setup_models_api_model
    include_context :generate_session_header

    let(:pool_id) { @inventory_pool.id }
    let(:cookie_header) { @cookie_header }
    let(:client) { plain_faraday_json_client(cookie_header) }

    let(:form_categories) { @form_categories }
    let(:form_compatible_models) { @form_compatible_models }

    let(:path_arrow) { File.expand_path("spec/files/arrow.png", Dir.pwd) }
    let(:path_arrow_thumb) { File.expand_path("spec/files/arrow_thumb.png", Dir.pwd) }
    let(:path_test_pdf) { File.expand_path("spec/files/test.pdf", Dir.pwd) }
    let(:path_test_txt) { File.expand_path("spec/files/text-file.txt", Dir.pwd) }

    before do
      [path_arrow, path_arrow_thumb, path_test_pdf].each do |path|
        raise "File not found: #{path}" unless File.exist?(path)
      end

      resp = client.get "/inventory/#{pool_id}/entitlement-groups"
      @form_entitlement_groups = resp.body
      raise "Failed to fetch entitlement groups" unless resp.status == 200

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

    context "fetch of form" do
      it "ensures form manufacturer data is fetched" do
        resp = client.get "/inventory/#{pool_id}/item"
        @form_entitlement_groups = resp.body
        expect(resp.status).to be(200)

        expect(@form_entitlement_groups["data"]["inventory_pool_id"]).to eq(pool_id)
        expect(@form_entitlement_groups["fields"].count).to eq(32)

        expect(@form_entitlement_groups).not_to be_nil
        expect(@form_entitlement_groups.count).to eq(2)
      end
    end

    context "create model" do
      it "creates a model with all available attributes" do
        # create item
        form_data = {
          serial_number: nil,
          note: nil,

          # attachments: [], # binary data
          # attachments_to_delete: [],

          is_inventory_relevant: "true",
          last_check: nil,
          user_name: nil,
          # undefined: nil,
          invoice_number: nil,
          invoice_date: nil,
          price: nil,
          shelf: nil,
          inventory_code: "AUS45859",
          retired: "false",
          is_broken: "false",
          is_incomplete: "false",
          is_borrowable: "false",
          status_note: nil,
          room_id: @form_rooms[0]["id"],
          model_id: @form_model_data[0]["id"],
          owner_id: @form_owners[0]["id"],
          properties: {
            electrical_power: "",
            imei_number: "",
            p4u: "",
            reference: "invoice",
            warranty_expiration: "",
            quantity_allocations: []
          }.to_json
        }.transform_values { |v| v.nil? ? "" : v.to_s }

        result = http_multipart_client(
          "/inventory/#{pool_id}/item",
          form_data,
          method: :post,
          headers: cookie_header
        )

        expect(result.status).to eq(200)
        model_id = result.body["data"]["model_id"]
        item_id = result.body["data"]["id"]

        # fetch created item
        resp = client.get "/inventory/#{pool_id}/models/#{model_id}/item/#{item_id}"
        @form_entitlement_groups = resp.body
        expect(resp.status).to be(200)

        expect(@form_entitlement_groups).not_to be_nil
        expect(@form_entitlement_groups.count).to eq(2)

        expect(result.status).to eq(200)
        expect(@form_entitlement_groups["data"]).to be_present
        expect(@form_entitlement_groups["fields"].count).to eq(31)

        # update item request
        result = http_multipart_client(
          "/inventory/#{pool_id}/models/#{model_id}/item/#{item_id}",
          form_data,
          method: :put,
          headers: cookie_header
        )

        expect(result.status).to eq(200)
      end

      # TODO: write tests with attachments
    end
  end
end
