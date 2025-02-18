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

    # let(:form_categories) { @form_categories }
    # let(:form_compatible_models) { @form_compatible_models }

    let(:path_test_pdf) { File.expand_path("spec/files/test.pdf", Dir.pwd) }
    let(:path_test_txt) { File.expand_path("spec/files/text-file.txt", Dir.pwd) }

    before do
      [path_test_pdf, path_test_txt].each do |path|
        raise "File not found: #{path}" unless File.exist?(path)
      end

      # Fetch shared data and set global instance variables
      result = client.get "/inventory/manufacturers?type=Software"
      @form_manufacturer = result.body
      raise "Failed to fetch manufacturers" unless result.status == 200

      # result = client.get "/inventory/#{pool_id}/entitlement-groups"
      # @form_entitlement_groups = result.body
      # raise "Failed to fetch entitlement groups" unless result.status == 200
      #
      # result = client.get "/inventory/models-compatibles"
      # @form_models_compatibles = result.body["data"]
      # raise "Failed to fetch compatible models" unless result.status == 200
      #
      # result = client.get "/inventory/#{pool_id}/model-groups"
      # @form_model_groups = result.body
      # raise "Failed to fetch model groups" unless result.status == 200
    end

    context "fetch form data" do
      it "ensures form manufacturer data is fetched" do
        expect(@form_manufacturer).not_to be_nil
        expect(@form_manufacturer.count).to eq(1)
      end

      # it "ensures entitlement groups data is fetched" do
      #   expect(@form_entitlement_groups).not_to be_nil
      #   expect(@form_entitlement_groups.count).to eq(1)
      # end
      #
      # it "ensures models compatible data is fetched" do
      #   expect(@form_models_compatibles).not_to be_nil
      #   expect(@form_models_compatibles.count).to eq(2)
      # end
      #
      # it "ensures model groups data is fetched" do
      #   expect(@form_model_groups).not_to be_nil
      #   expect(@form_model_groups.count).to eq(2)
      # end
    end

    it "creates software with all available attributes" do
      # create software request
      form_data = {
        "product" => Faker::Commerce.product_name,
        # "images" => [File.open(path_arrow, "rb"), File.open(path_arrow_thumb, "rb")],
        "attachments" => [File.open(path_test_pdf, "rb")],
        "version" => "v1.0",
        "manufacturer" => @form_manufacturer.first, # Use fetched manufacturer name
        # "isPackage" => "true",
        # "description" => "A sample product",
        "technicalDetails" => "Specs go here"
        # "internalDescription" => "Internal notes",
        # "importantNotes" => "Important usage notes",
        # "entitlements" => [{entitlement_group_id: @form_entitlement_groups.first["id"], entitlement_id: nil, quantity: 33}].to_json,
        # "compatibles" => [compatibles.first].to_json,
        # "categories" => [@form_model_groups.first].to_json
      }

      result = http_multipart_client(
        "/inventory/#{pool_id}/software",
        form_data,
        headers: cookie_header
      )

      # puts "Result.model_id: #{result.body["data"]["id"]}"
      # puts "Result.pool_id: #{pool_id}"
      # puts "Result.body: #{result.body}"

      expect(result.status).to eq(200)

      # fetch created software
      model_id = result.body["data"]["id"]
      result = client.get "/inventory/#{pool_id}/software/#{model_id}"

      # expect(result.body[0]["images"].count).to eq(1)
      expect(result.body[0]["attachments"].count).to eq(1)

      # expect(result.body[0]["entitlement_groups"].count).to eq(1)
      # expect(result.body[0]["compatibles"].count).to eq(1)
      # expect(result.body[0]["categories"].count).to eq(1)
      expect(result.status).to eq(200)

      expect(Attachment.where(model_id: model_id).count).to eq(1)

      # update software request
      form_data = {
        "product" => "updated product",
        # "images" => [File.open(path_arrow, "rb"), File.open(path_arrow_thumb, "rb")],
        "attachments" => [File.open(path_test_pdf, "rb")],
        "version" => "updated v1.0",
        "manufacturer" => "updated manufacturer",
        # "isPackage" => "true",
        # "description" => "updated description",
        "technicalDetails" => "updated techDetail"
        # "internalDescription" => "updated internalDesc",
        # "importantNotes" => "updated notes",
        # "entitlements" => [{entitlement_group_id: @form_entitlement_groups.first["id"], entitlement_id: nil, quantity: 11}].to_json,
        # "compatibles" => [compatibles.first, compatibles.second].to_json,
        # "categories" => [@form_model_groups.first, @form_model_groups.second].to_json
      }

      result = http_multipart_client(
        "/inventory/#{pool_id}/software/#{model_id}",
        form_data,
        method: :put,
        headers: cookie_header
      )
      expect(result.status).to eq(200)
      expect(result.body[0]["id"]).to eq(model_id)

      # fetch updated model
      result = client.get "/inventory/#{pool_id}/software/#{model_id}"

      # expect(result.body[0]["images"].count).to eq(2)
      expect(result.body[0]["attachments"].count).to eq(2)
      # expect(result.body[0]["entitlement_groups"].count).to eq(1)
      # expect(result.body[0]["entitlement_groups"][0]["quantity"]).to eq(11)
      # expect(result.body[0]["compatibles"].count).to eq(2)
      # expect(result.body[0]["categories"].count).to eq(2)
      expect(result.status).to eq(200)
    end

    context "create software (min)" do
      it "creates software with all available attributes" do
        # create software request
        form_data = {
          "product" => Faker::Commerce.product_name
        }

        result = http_multipart_client(
          "/inventory/#{pool_id}/software",
          form_data,
          headers: cookie_header
        )
        expect(result.status).to eq(200)
        binding.pry

        # fetch created software
        model_id = result.body["data"]["id"]
        result = client.get "/inventory/#{pool_id}/software/#{model_id}"

        binding.pry
        expect(result.body[0]["attachments"].count).to eq(0)
        expect(result.status).to eq(200)


        # update software request
        form_data = {
          "product" => "updated product"
        }

        result = http_multipart_client(
          "/inventory/#{pool_id}/software/#{model_id}",
          form_data,
          method: :put,
          headers: cookie_header
        )
        binding.pry
        expect(result.status).to eq(200)
        expect(result.body[0]["id"]).to eq(model_id)


        # fetch updated model
        result = client.get "/inventory/#{pool_id}/software/#{model_id}"
        binding.pry

        expect(result.body[0]["product"]).to eq("updated product")
        expect(result.body[0]["attachments"].count).to eq(0)
        expect(result.status).to eq(200)
      end
    end

  end
end
