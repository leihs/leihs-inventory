require "spec_helper"
require "pry"
require_relative "../../_shared"
require "faker"

feature "Inventory Model Management2" do
  context "when interacting with inventory models in a specific inventory pool2", driver: :selenium_headless do
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


    # before do
    #
    #   resp = client.get "/inventory/owners"
    #   # binding.pry
    #   @form_owners = resp.body
    #   raise "Failed to fetch manufacturers" unless resp.status == 200
    #
    #   resp = client.get "/inventory/buildings"
    #   # binding.pry
    #   @form_buildings = resp.body
    #   raise "Failed to fetch entitlement groups" unless resp.status == 200
    #
    #   resp = client.get "/inventory/rooms?building_id=#{@form_buildings[0]["id"]}"
    #   # binding.pry
    #   @form_rooms = resp.body
    #   raise "Failed to fetch entitlement groups" unless resp.status == 200
    #
    #   resp = client.get "/inventory/manufacturers?type=Model&in-detail=true"
    #   # binding.pry
    #   @form_model_names = resp.body
    #   raise "Failed to fetch compatible models" unless resp.status == 200
    #
    #   resp = client.get "/inventory/manufacturers?type=Model&in-detail=true&search-term=#{@form_model_names[0]}"
    #   # binding.pry
    #   @form_model_data = resp.body
    #   raise "Failed to fetch compatible models" unless resp.status == 200
    #
    #   # resp = client.get "/inventory/#{pool_id}/model-groups"
    #   # @form_model_groups = resp.body
    #   # raise "Failed to fetch model groups" unless resp.status == 200
    #
    #
    #
    #
    #
    #   # it "fetch default" do
    #   #   result = client.get "/inventory/owners"
    #   #
    #   #   # binding.pry
    #   #   expect(result.status).to eq(200)
    #   #   expect(result.body.count).to eq(2)
    #   # end
    #   #
    #   # it "fetch default" do
    #   #   result = client.get "/inventory/buildings"
    #   #
    #   #   # binding.pry
    #   #   expect(result.status).to eq(200)
    #   #   expect(result.body.count).to eq(3)
    #   # end
    #   #
    #   # it "fetch default" do
    #   #   # result = client.get "/inventory/manufacturers?type=Model&in-detail=true&search-term=a"
    #   #   result = client.get "/inventory/manufacturers?type=Model&in-detail=true"
    #
    #
    #
    #
    #
    #
    #
    #
    #
    #
    #   end



    # GET http://localhost:3260/inventory/8bd16d45-056d-5590-bc7f-12849f034351/option/dab818e5-1ae0-4259-a661-8871dd980217
    #
    #
    #
    # PUT http://localhost:3260/inventory/8bd16d45-056d-5590-bc7f-12849f034351/option/dab818e5-1ae0-4259-a661-8871dd980217
    #
    # product: Zapfen Kupplung 2x 16mm2233334444
    # version: 22344
    # price: 202344
    # inventory_code: zapkup22344
    #
    #
    #
    # POST http://localhost:3260/inventory/8bd16d45-056d-5590-bc7f-12849f034351/option
    #
    # product: fdsa
    # version: fda
    # price: 222
    # inventory_code: fda


    context "create model" do
      # it "fetch default" do
      #   result = client.get "/inventory/#{pool_id}/items-with-model-info?result_type=Normal"
      #
      #   # FIXME: no result handling
      #   # result = client.get "/inventory/#{pool_id}/items-with-model-info?result_type=Normal&search_term=podest"
      #
      #   # binding.pry
      #   expect(result.status).to eq(200)
      #   expect(result.body.count).to eq(1)
      # end
      #
      # it "fetch default" do
      #   result = client.get "/inventory/#{pool_id}/package"
      #
      #   # binding.pry
      #   expect(result.status).to eq(200)
      #   expect(result.body["data"]["inventory_pool_id"]).to eq(pool_id)
      #   expect(result.body["fields"].count).to eq(20)
      # end
      #
      # it "fetch default" do
      #   result = client.get "/inventory/#{pool_id}/entitlement-groups"
      #
      #   # binding.pry
      #   expect(result.status).to eq(200)
      #   expect(result.body.count).to eq(1)
      # end
      #
      #
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
      #
      #   # binding.pry
      #   expect(result.status).to eq(200)
      #   expect(result.body.count).to eq(2)
      # end
      #
      #
      # it "fetch by form data" do
      #   result = client.get "/inventory/#{pool_id}/package"
      #
      #   expect(result.body["data"]["inventory_pool_id"]).to eq(pool_id)
      #   expect(result.body["fields"].count).to eq(20)
      #
      # end


      it "create, fetch & update by form data" do


        # create
        form_data = {
          product: Faker::Commerce.product_name,
          version: "v1",
          price: "111",
          inventory_code: "O-1001",
        }


        result = http_multipart_client(
          "/inventory/#{pool_id}/option",
          form_data,
          headers: cookie_header
        )


        expect(result.status).to eq(200)
        expect(result.body["data"]["id"]).to be_present
        expect(result.body["validation"].count).to eq(0)

        option_id=result.body["data"]["id"]







        # fetch
        result = client.get "/inventory/#{pool_id}/option/#{option_id}"
        expect(result.body.count).to eq(1)

        binding.pry



        # update
        form_data = {
          product: Faker::Commerce.product_name,
          version: "v2",
          price: "222"
        }

        result = http_multipart_client(
          "/inventory/#{pool_id}/option/#{option_id}",
          form_data,
          method: :put,
          headers: cookie_header
        )

        # binding.pry
        expect(result.status).to eq(200)

        expect(result.body[0]["version"]).to eq("v2")
        expect(result.body[0]["price"]).to eq(222.0)

      end




    end

  end
end
