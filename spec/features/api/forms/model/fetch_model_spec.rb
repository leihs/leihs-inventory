require "spec_helper"
require "pry"
require_relative "../../_shared"
require "faker"

post_response = {
  "type" => String,
  "rental_price" => [NilClass, Numeric], # Allow nil or Numeric
  "cover_image_id" => [NilClass, String],
  "hand_over_note" => String,
  "updated_at" => String,
  "internal_description" => String,
  "product" => String,
  "info_url" => [NilClass, String],
  "id" => String,
  "manufacturer" => String,
  "version" => String,
  "created_at" => String,
  "technical_detail" => String
}

def validate_map_structure(map, required_keys)

  errors = []

  required_keys.each do |key, expected_classes|
    unless map.key?(key)
      errors << "Missing key: #{key}"
      next
    end

    value = map[key]
    expected_classes = Array(expected_classes)

    unless expected_classes.any? { |cls| value.is_a?(cls) }
      errors << "Invalid type for key '#{key}': expected #{expected_classes.join(' or ')}, got #{value.class}"
    end
  end

  if errors.empty?
    puts "Map structure is valid."
    true
  else
    puts "Validation failed with errors:"
    errors.each { |error| puts "- #{error}" }
    false
  end
end

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
    let(:path_test2_pdf) { File.expand_path("spec/files/test2.pdf", Dir.pwd) }
    let(:path_test_txt) { File.expand_path("spec/files/text-file.txt", Dir.pwd) }

    before do
      [path_arrow, path_arrow_thumb, path_test_pdf].each do |path|
        raise "File not found: #{path}" unless File.exist?(path)
      end

      # Fetch shared data and set global instance variables
      resp = client.get "/inventory/manufacturers?type=Model"
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
        expect(@form_entitlement_groups.count).to eq(2)
      end

      it "ensures models compatible data is fetched" do
        expect(@form_models_compatibles).not_to be_nil
        expect(@form_models_compatibles.count).to eq(3)
      end

      it "ensures model groups data is fetched" do
        expect(@form_model_groups).not_to be_nil
        expect(@form_model_groups.count).to eq(2)
      end
    end

    context "create model (min)" do
      it "creates a model with all available attributes" do
        compatibles = @form_models_compatibles
        compatibles.first["id"] = compatibles.first.delete("model_id")

        # create model request
        form_data = {
          "product" => Faker::Commerce.product_name }

        result = http_multipart_client(
          "/inventory/#{pool_id}/model",
          form_data,
          headers: cookie_header
        )

        expect(result.status).to eq(200)

        # fetch created model
        model_id = result.body["data"]["id"]
        resp = client.get "/inventory/#{pool_id}/model/#{model_id}"

        expect(resp.body[0]["images"].count).to eq(0)
        expect(resp.body[0]["attachments"].count).to eq(0)

        expect(resp.body[0]["entitlement_groups"].count).to eq(0)
        expect(resp.body[0]["compatibles"].count).to eq(0)
        expect(resp.body[0]["categories"].count).to eq(0)
        expect(result.status).to eq(200)

        expect(Image.where(target_id: model_id).count).to eq(0)

        # update model request
        form_data = {
          "product" => "updated product" }

        result = http_multipart_client(
          "/inventory/#{pool_id}/model/#{model_id}",
          form_data,
          method: :put,
          headers: cookie_header
        )

        expect(result.status).to eq(200)
        expect(result.body[0]["id"]).to eq(model_id)

        # fetch updated model
        resp = client.get "/inventory/#{pool_id}/model/#{model_id}"

        expect(resp.body[0]["images"].count).to eq(0)
        expect(resp.body[0]["attachments"].count).to eq(0)
        expect(resp.body[0]["entitlement_groups"].count).to eq(0)
        expect(resp.body[0]["entitlement_groups"].count).to eq(0)
        expect(resp.body[0]["compatibles"].count).to eq(0)
        expect(resp.body[0]["categories"].count).to eq(0)
        expect(result.status).to eq(200)
      end
    end

    context "create model" do
      it "creates a model with all available attributes" do
        compatibles = @form_models_compatibles
        compatibles.first["id"] = compatibles.first.delete("model_id")

        # create model request
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
          "entitlements" => [{ entitlement_group_id: @form_entitlement_groups.first["id"], entitlement_id: nil, quantity: 33 }].to_json,
          "compatibles" => [compatibles.first].to_json,
          "categories" => [@form_model_groups.first].to_json
        }

        result = http_multipart_client(
          "/inventory/#{pool_id}/model",
          form_data,
          headers: cookie_header
        )

        expect(result.status).to eq(200)

        # fetch created model
        model_id = result.body["data"]["id"]
        resp = client.get "/inventory/#{pool_id}/model/#{model_id}"

        expect(resp.body[0]["images"].count).to eq(1)
        expect(resp.body[0]["attachments"].count).to eq(1)

        expect(resp.body[0]["entitlement_groups"].count).to eq(1)
        expect(resp.body[0]["compatibles"].count).to eq(1)
        expect(resp.body[0]["categories"].count).to eq(1)
        expect(result.status).to eq(200)

        expect(Image.where(target_id: model_id).count).to eq(2)

        # update model request
        form_data = {
          "product" => "updated product",
          "images" => [File.open(path_arrow, "rb"), File.open(path_arrow_thumb, "rb")],
          "attachments" => [File.open(path_test_pdf, "rb")],
          "version" => "updated v1.0",
          "manufacturer" => "updated manufacturer",
          "isPackage" => "true",
          "description" => "updated description",
          "technicalDetails" => "updated techDetail",
          "internalDescription" => "updated internalDesc",
          "importantNotes" => "updated notes",
          "entitlements" => [{ entitlement_group_id: @form_entitlement_groups.first["id"], entitlement_id: nil, quantity: 11 }].to_json,
          "compatibles" => [compatibles.first, compatibles.second].to_json,
          "categories" => [@form_model_groups.first, @form_model_groups.second].to_json
        }

        result = http_multipart_client(
          "/inventory/#{pool_id}/model/#{model_id}",
          form_data,
          method: :put,
          headers: cookie_header
        )
        expect(result.status).to eq(200)
        expect(result.body[0]["id"]).to eq(model_id)

        # fetch updated model
        resp = client.get "/inventory/#{pool_id}/model/#{model_id}"

        expect(resp.body[0]["images"].count).to eq(2)
        expect(resp.body[0]["attachments"].count).to eq(2)
        expect(resp.body[0]["entitlement_groups"].count).to eq(1)
        expect(resp.body[0]["entitlement_groups"][0]["quantity"]).to eq(11)
        expect(resp.body[0]["compatibles"].count).to eq(2)
        expect(resp.body[0]["categories"].count).to eq(2)
        expect(result.status).to eq(200)
      end
    end

    context "create & modify model (max)" do
      it "creates a model with all available attributes" do
        compatibles = @form_models_compatibles
        compatibles.first["id"] = compatibles.first.delete("model_id")

        # create model request
        form_data = {
          "product" => Faker::Commerce.product_name,
          "version" => "v1.0",
          "manufacturer" => @form_manufacturer.first, # Use fetched manufacturer name
          "description" => "A sample product",
          "technicalDetails" => "Specs go here",
          "internalDescription" => "Internal notes",
          "handOverNote" => "Hand over notes",

          "images" => [File.open(path_arrow, "rb"), File.open(path_arrow_thumb, "rb")],

          "properties" => [{ key: "prop-1", value: "bar1" }, { key: "prop-2", value: "bar2" }].to_json,
          "accessories" => [{ name: "acc1", inventory_pool: false }, { name: "acc2", inventory_pool: true }].to_json,
          "entitlements" => [{ entitlement_group_id: @form_entitlement_groups.first["id"], entitlement_id: nil, quantity: 33 },
                             { entitlement_group_id: @form_entitlement_groups.second["id"], entitlement_id: nil, quantity: 55 }].to_json,
          "categories" => [@form_model_groups.first, @form_model_groups.second].to_json,
          "compatibles" => [compatibles.first, compatibles.second].to_json,

          "attachments" => [File.open(path_test_pdf, "rb"), File.open(path_test2_pdf, "rb")],
          # "importantNotes" => "Important usage notes",
          "isPackage" => "true",
        }

        result = http_multipart_client(
          "/inventory/#{pool_id}/model",
          form_data,
          headers: cookie_header
        )

        expect(result.status).to eq(200)

        validate_map_structure(result.body["data"], post_response)

        binding.pry
        # fetch created model
        model_id = result.body["data"]["id"]
        resp = client.get "/inventory/#{pool_id}/model/#{model_id}"

        expect(resp.body[0]["images"].count).to eq(1)
        expect(resp.body[0]["attachments"].count).to eq(2)

        expect(resp.body[0]["entitlement_groups"].count).to eq(2)
        expect(resp.body[0]["compatibles"].count).to eq(2)
        expect(resp.body[0]["categories"].count).to eq(2)
        expect(result.status).to eq(200)

        expect(Image.where(target_id: model_id).count).to eq(2)

        # update model request
        form_data = {
          "product" => "updated product",
          "images" => [File.open(path_arrow, "rb"), File.open(path_arrow_thumb, "rb")],
          "attachments" => [File.open(path_test_pdf, "rb")],
          "version" => "updated v1.0",
          "manufacturer" => "updated manufacturer",
          "isPackage" => "true",
          "description" => "updated description",
          "technicalDetails" => "updated techDetail",
          "internalDescription" => "updated internalDesc",
          "importantNotes" => "updated notes",
          "entitlements" => [{ entitlement_group_id: @form_entitlement_groups.first["id"], entitlement_id: nil, quantity: 11 }].to_json,
          "compatibles" => [compatibles.first, compatibles.second].to_json,
          "categories" => [@form_model_groups.first, @form_model_groups.second].to_json
        }

        result = http_multipart_client(
          "/inventory/#{pool_id}/model/#{model_id}",
          form_data,
          method: :put,
          headers: cookie_header
        )
        expect(result.status).to eq(200)
        expect(result.body[0]["id"]).to eq(model_id)
        #
        # # fetch updated model
        # resp = client.get "/inventory/#{pool_id}/model/#{model_id}"
        #
        # expect(resp.body[0]["images"].count).to eq(2)
        # expect(resp.body[0]["attachments"].count).to eq(2)
        # expect(resp.body[0]["entitlement_groups"].count).to eq(1)
        # expect(resp.body[0]["entitlement_groups"][0]["quantity"]).to eq(11)
        # expect(resp.body[0]["compatibles"].count).to eq(2)
        # expect(resp.body[0]["categories"].count).to eq(2)
        # expect(result.status).to eq(200)
      end
    end

  end
end
