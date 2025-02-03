require "spec_helper"
require "pry"
require_relative "../../_shared"
require "faker"

feature "Inventory Model Management" do
  context "when interacting with inventory models in a specific inventory pool", driver: :selenium_headless do
    include_context :setup_models_api_model
    # include_context :setup_models_api_license
    include_context :setup_unknown_building_room_supplier
    include_context :generate_session_header


    # include_context :generate_session_header

    # let(:pool_id) { @inventory_pool.id }
    # let(:cookie_header) { @cookie_header }
    # let(:client) { plain_faraday_json_client(cookie_header) }
    #
    # let(:form_categories) { @form_categories }
    # let(:form_compatible_models) { @form_compatible_models }



    let(:cookie_header) { @cookie_header }
    let(:client) { plain_faraday_json_client(cookie_header) }
    let(:pool_id) { @inventory_pool.id }

    let(:software_model) { @software_model }
    let(:license_item) { @license_item }
    let(:model_id) { @software_model.id }

    # let(:path_test_pdf) { File.expand_path("spec/files/test.pdf", Dir.pwd) }
    # let(:path_test_txt) { File.expand_path("spec/files/text-file.txt", Dir.pwd) }

    # before do
    #   [path_test_pdf, path_test_txt].each do |path|
    #     raise "File not found: #{path}" unless File.exist?(path)
    #   end
    # end


    before do

      resp = client.get "/inventory/owners"
      # binding.pry
      @form_owners = resp.body
      raise "Failed to fetch manufacturers" unless resp.status == 200

      resp = client.get "/inventory/buildings"
      # binding.pry
      @form_buildings = resp.body
      raise "Failed to fetch entitlement groups" unless resp.status == 200

      resp = client.get "/inventory/rooms?building_id=#{@form_buildings[0]["id"]}"
      # binding.pry
      @form_rooms = resp.body
      raise "Failed to fetch entitlement groups" unless resp.status == 200

      resp = client.get "/inventory/manufacturers?type=Model&in-detail=true"
      # binding.pry
      @form_model_names = resp.body
      raise "Failed to fetch compatible models" unless resp.status == 200

      resp = client.get "/inventory/manufacturers?type=Model&in-detail=true&search-term=#{@form_model_names[0]}"
      # binding.pry
      @form_model_data = resp.body
      raise "Failed to fetch compatible models" unless resp.status == 200

      # resp = client.get "/inventory/#{pool_id}/model-groups"
      # @form_model_groups = resp.body
      # raise "Failed to fetch model groups" unless resp.status == 200





      # it "fetch default" do
      #   result = client.get "/inventory/owners"
      #
      #   # binding.pry
      #   expect(result.status).to eq(200)
      #   expect(result.body.count).to eq(2)
      # end
      #
      # it "fetch default" do
      #   result = client.get "/inventory/buildings"
      #
      #   # binding.pry
      #   expect(result.status).to eq(200)
      #   expect(result.body.count).to eq(3)
      # end
      #
      # it "fetch default" do
      #   # result = client.get "/inventory/manufacturers?type=Model&in-detail=true&search-term=a"
      #   result = client.get "/inventory/manufacturers?type=Model&in-detail=true"










      end

    context "create model" do
      it "fetch default" do
        result = client.get "/inventory/#{pool_id}/items-with-model-info?result_type=Normal"

        # FIXME: no result handling
        # result = client.get "/inventory/#{pool_id}/items-with-model-info?result_type=Normal&search_term=podest"

        # binding.pry
        expect(result.status).to eq(200)
        expect(result.body.count).to eq(1)
      end

      it "fetch default" do
        result = client.get "/inventory/#{pool_id}/package"

        # binding.pry
        expect(result.status).to eq(200)
        expect(result.body["data"]["inventory_pool_id"]).to eq(pool_id)
        expect(result.body["fields"].count).to eq(20)
      end

      it "fetch default" do
        result = client.get "/inventory/#{pool_id}/entitlement-groups"

        # binding.pry
        expect(result.status).to eq(200)
        expect(result.body.count).to eq(1)
      end


      it "fetch default" do
        result = client.get "/inventory/owners"

        # binding.pry
        expect(result.status).to eq(200)
        expect(result.body.count).to eq(2)
      end

      it "fetch default" do
        result = client.get "/inventory/buildings"

        # binding.pry
        expect(result.status).to eq(200)
        expect(result.body.count).to eq(3)
      end

      it "fetch default" do
        # result = client.get "/inventory/manufacturers?type=Model&in-detail=true&search-term=a"
        result = client.get "/inventory/manufacturers?type=Model&in-detail=true"

        # binding.pry
        expect(result.status).to eq(200)
        expect(result.body.count).to eq(2)
      end


      it "fetch by form data" do
        result = client.get "/inventory/#{pool_id}/package"

        expect(result.body["data"]["inventory_pool_id"]).to eq(pool_id)
        expect(result.body["fields"].count).to eq(20)

      end


      it "create by form data" do

        # result = client.get "inventory/manufacturers?type=Software&in-detail=true"
        #
        # result = client.get "/inventory/#{pool_id}/models/#{model_id}/package/#{package_id}"
        # result = client.put "/inventory/#{pool_id}/models/#{model_id}/package/#{package_id}"
        # result = client.post "/inventory/#{pool_id}/package/#{package_id}"
        #
        # result = client.post "/inventory/#{pool_id}/package"
        #
        #
        # binding.pry
        # expect(result.body["data"]["inventory_pool_id"]).to eq(pool_id)
        # expect(result.body["fields"].count).to eq(20)
        #
        # supplier_id = result.body[0]["id"]

        form_data = {
          is_inventory_relevant: true,
          last_check: nil,
          user_name: nil,
          price: nil,
          shelf: nil,
          inventory_code: "P-AUS00002",
          retired: false,
          is_broken: false,
          is_incomplete: false,
          is_borrowable: false,
          status_note: nil,
          room_id: @form_rooms[0]["id"],
          # model_id: "c53ba068-423f-4816-8707-623cbb0a3141",
          model_id: @form_model_data[0]["id"],
          # owner_id: "8bd16d45-056d-5590-bc7f-12849f034351",
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
        # expect(result.body.count).to eq(2)
        expect(result.body["data"]).to exit
        expect(result.body["validation"].count).to eq(0)

        item_id=result.body[0]["id"]
        model_id=result.body[0]["model_id"]
        product_name=result.body[0]["product"]







        result = client.get "/inventory/#{pool_id}/models/#{model_id}/package/#{item_id}"
        # binding.pry

        expect(result.body["data"]).to exit
        expect(result.body["validation"].count).to eq(0)








        # expect(result.body["data"]["item_id"]).to be
        # expect(result.body["data"]["id"]).to be
        # expect(result.body["data"]["id"]).to eq(result.body["data"]["item_id"])
        #
        # item_id = result.body["data"]["id"]
        #
        # expect(result.body["data"]["room_id"]).to be
        # expect(result.body["data"]["owner_id"]).to be
        # expect(result.body["data"]["inventory_pool_id"]).to be
        #
        # form_data = {
        #   "serial_number" => "your-serial-number",
        #   "note" => "your-note",
        #   "attachments-to-delete" => [],
        #   "invoice_date" => "2024-12-06",
        #   "price" => "123.45",
        #   "retired" => false.to_s,
        #   "is_borrowable" => false.to_s,
        #   "inventory_code" => "AUS45863",
        #   "item_version" => "v1.0",
        #   "model_id" => model_id,
        #   "supplier_id" => nil.to_s,
        #   "owner_id" => pool_id,
        #   "properties" => {
        #     "activation_type" => "none",
        #     "license_type" => "free",
        #     "total_quantity" => "",
        #     "license_expiration" => "",
        #     "p4u" => "",
        #     "reference" => "invoice",
        #     "procured_by" => "",
        #     "maintenance_contract" => "false",
        #     "maintenance_expiration" => "",
        #     "maintenance_currency" => "CHF",
        #     "maintenance_price" => "",
        #     "quantity_allocations" => []
        #   }.to_json
        # }
        #
        # result = http_multipart_client(
        #   "/inventory/#{pool_id}/models/#{model_id}/licenses/#{item_id}",
        #   form_data,
        #   method: :put,
        #   headers: cookie_header
        # )
        #
        # expect(result.status).to eq(200)
        #
        # # TODO: revise to use data/validation response-format
        # expect(result.body[0]["id"]).to be
        #
        # # item_id = result.body[0]["id"]
        #
        # expect(result.body[0]["room_id"]).to be
        # expect(result.body[0]["owner_id"]).to be
        # expect(result.body[0]["inventory_pool_id"]).to be
      end

      # it "creates and update license (simple)" do
      #   result = client.get "inventory/manufacturers?type=Software&in-detail=true"
      #
      #   expect(result.status).to eq(200)
      #   expect(result.body.count).to eq(1)
      #
      #   supplier_id = result.body[0]["id"]
      #
      #   form_data = {
      #     "serial_number" => "your-serial-number",
      #     "note" => "your-note",
      #     "attachments" => [File.open(path_test_pdf, "rb")],
      #     "invoice_date" => "2024-12-19",
      #     "price" => "100",
      #     "retired" => true.to_s,
      #     "retired_reason" => "your-reason-retired",
      #     "is_borrowable" => false.to_s,
      #     "inventory_code" => "AUS45863",
      #     "item_version" => "your-version",
      #     "supplier_id" => supplier_id,
      #     "owner_id" => pool_id,
      #     "properties" => {
      #       "activation_type" => "dongle",
      #       "dongle_id" => "your-dongle-id",
      #       "license_type" => "single_workplace",
      #       "total_quantity" => "33",
      #       "operating_system" => ["windows", "mac_os_x", "linux", "ios"],
      #       "installation" => ["citrix", "local", "web"],
      #       "license_expiration" => "2024-12-05",
      #       "p4u" => "your-p4u",
      #       "reference" => "investment",
      #       "project_number" => "your-project-number",
      #       "procured_by" => "your-procured-person",
      #       "maintenance_contract" => "true",
      #       "maintenance_expiration" => "2024-12-20",
      #       "maintenance_currency" => "CHF",
      #       "maintenance_price" => "20",
      #       "quantity_allocations" => [
      #         {"quantity" => "your-key", "room" => "your-value"}
      #       ]
      #     }.to_json
      #   }
      #
      #   result = http_multipart_client(
      #     "/inventory/#{pool_id}/models/#{model_id}/licenses",
      #     form_data,
      #     headers: cookie_header
      #   )
      #
      #   expect(result.status).to eq(200)
      #
      #   expect(result.body["data"]["item_id"]).to be
      #   expect(result.body["data"]["id"]).to be
      #   expect(result.body["data"]["id"]).to eq(result.body["data"]["item_id"])
      #
      #   item_id = result.body["data"]["id"]
      #
      #   expect(result.body["data"]["room_id"]).to be
      #   expect(result.body["data"]["owner_id"]).to be
      #   expect(result.body["data"]["inventory_pool_id"]).to be
      #
      #   # attachments_id = result.body["data"]["attachments"][0]["id"]
      #
      #   form_data = {
      #     "serial_number" => "your-serial-number",
      #     "note" => "your-note",
      #     "invoice_date" => "2024-12-19",
      #     "price" => "100",
      #     "retired" => false.to_s,
      #     "is_borrowable" => false.to_s,
      #     "inventory_code" => "AUS45863",
      #     "item_version" => "your-version",
      #     "supplier_id" => nil.to_s,
      #     "owner_id" => pool_id,
      #     "properties" => {
      #       "activation_type" => "none",
      #       "license_type" => "single_workplace",
      #       "total_quantity" => "33",
      #       "operating_system" => [],
      #       "installation" => [],
      #       "license_expiration" => "2024-12-05",
      #       "p4u" => "your-p4u",
      #       "reference" => "investment",
      #       "project_number" => "your-project-number",
      #       "procured_by" => "your-procured-person",
      #       "maintenance_contract" => "true",
      #       "maintenance_expiration" => "2024-12-20",
      #       "maintenance_currency" => "CHF",
      #       "maintenance_price" => "20",
      #       "quantity_allocations" => []
      #     }.to_json
      #   }
      #
      #   result = http_multipart_client(
      #     "/inventory/#{pool_id}/models/#{model_id}/licenses/#{item_id}",
      #     form_data,
      #     method: :put,
      #     headers: cookie_header
      #   )
      #
      #   expect(result.status).to eq(200)
      #
      #   # TODO: revise to use data/validation response-format
      #   expect(result.body[0]["id"]).to be
      #
      #   # item_id = result.body[0]["id"]
      #
      #   expect(result.body[0]["room_id"]).to be
      #   expect(result.body[0]["owner_id"]).to be
      #   expect(result.body[0]["inventory_pool_id"]).to be
      # end


    end

  end
end
