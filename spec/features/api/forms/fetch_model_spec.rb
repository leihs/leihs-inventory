require "spec_helper"
require "pry"
require_relative "../_shared"
require 'faker'

feature "Inventory Model Management" do
  context "when interacting with inventory models in a specific inventory pool", driver: :selenium_headless do
    include_context :setup_models_api_model

    let(:client) { plain_faraday_json_client }
    let(:pool_id) { @inventory_pool.id }

    let(:form_categories) { @form_categories }
    let(:form_compatible_models) { @form_compatible_models }

    let(:path_arrow) { File.expand_path("spec/files/arrow.png", Dir.pwd) }
    let(:path_arrow_thumb) { File.expand_path("spec/files/arrow_thumb.png", Dir.pwd) }
    let(:path_test_pdf) { File.expand_path("spec/files/test.pdf", Dir.pwd) }
    let(:path_test_txt) { File.expand_path("spec/files/text-file.txt", Dir.pwd) }

    before do
      # Ensure files exist
      [path_arrow, path_arrow_thumb, path_test_pdf].each do |path|
        raise "File not found: #{path}" unless File.exist?(path)
      end

      # Fetch shared data and set global instance variables
      resp = client.get "/inventory/manufacturers"
      @form_manufacturer = resp.body
      raise "Failed to fetch manufacturers" unless resp.status == 200

      resp = client.get "/inventory/#{pool_id}/entitlement-groups"
      @form_entitlement_groups = resp.body
      raise "Failed to fetch entitlement groups" unless resp.status == 200

      resp = client.get "/inventory/models-compatibles"
      @form_models_compatibles = resp.body["data"]
      raise "Failed to fetch compatible models" unless resp.status == 200

      resp = client.get "/inventory/#{pool_id}/model-groups"
      @form_model_groups = resp.body
      raise "Failed to fetch model groups" unless resp.status == 200
    end

    context "fetch form data" do
      it "ensures form manufacturer data is fetched" do
        expect(@form_manufacturer).not_to be_nil
        expect(@form_manufacturer.count).to eq(2)
      end

      it "ensures entitlement groups data is fetched" do
        expect(@form_entitlement_groups).not_to be_nil
        expect(@form_entitlement_groups.count).to eq(1)
      end

      it "ensures models compatible data is fetched" do
        expect(@form_models_compatibles).not_to be_nil
        expect(@form_models_compatibles.count).to eq(2)
      end

      it "ensures model groups data is fetched" do
        expect(@form_model_groups).not_to be_nil
        expect(@form_model_groups.count).to eq(2)
      end
    end

    # ;; FIXME
    context "create model" do
      it "creates a model with all available attributes" do
        form_data = {
          "product" => Faker::Commerce.product_name,
          "images" => [File.open(path_arrow, "rb"), File.open(path_arrow_thumb, "rb")],
          "attachments" => [File.open(path_test_pdf, "rb")],
          "version" => "v1.0",
          "manufacturer" => @form_manufacturer.first, # Use fetched manufacturer name
          "isPackage" => "true",
          "description" => "A sample product",
          "technicalDetails" => "Specs go here",
          "internalDescription" => "Internal notes",
          "importantNotes" => "Important usage notes",
        # ,
          # "compatibles" => @form_models_compatibles.map { |m| m["code"] }.join(","),
          # "entitlements" => [{entitle @form_entitlement_groups.first["id"] }],
          # "categories" => [{id: @form_model_groups.first["id"], type: "Category", name: "New-Product"}],

          # "entitlements" => [{entitlement_group_id: @form_entitlement_groups.first["id"], entitlement_id: nil, quantity: 1}].to_s,
           "categories" => [@form_model_groups.first].to_s
          # "compatibles" => [{id:  @form_models_compatibles.first["model_id"], product: "New-Product"}].to_json
        }



        # binding.pry
        result = http_multipart_client(
          "/inventory/#{pool_id}/model",
          form_data
        )
        binding.pry

        puts "Result: #{ result.body["data"]["id"]}"


        expect(result.status).to eq(200)
        # expect(result.body["data"].count).to be
        # expect(result.body["validation"].count).to eq(0)
        # expect(result.body["data"].keys.count).to eq(16)
        #
        # model_id = result.body["data"]["id"]
        # expect(Image.where(target_id: model_id).count).to eq(2)
      end
    end
  end
end