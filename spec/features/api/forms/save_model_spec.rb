require "spec_helper"
require "pry"
require_relative "../_shared"

feature "Inventory Model Management" do
  context "when interacting with inventory models in a specific inventory pool", driver: :selenium_headless do
    include_context :setup_models_api, "inventory_manager"
    include_context :generate_session_header

    let(:pool_id) { @inventory_pool.id }
    let(:cookie_header) { @cookie_header }

    let(:path_arrow) { File.expand_path("spec/files/arrow.png", Dir.pwd) }
    let(:path_arrow_thumb) { File.expand_path("spec/files/arrow_thumb.png", Dir.pwd) }
    let(:path_test_pdf) { File.expand_path("spec/files/test.pdf", Dir.pwd) }
    let(:path_test_txt) { File.expand_path("spec/files/text-file.txt", Dir.pwd) }

    before do
      [path_arrow, path_arrow_thumb, path_test_pdf].each do |path|
        raise "File not found: #{path}" unless File.exist?(path)
      end
    end

    it "creates a model with only the required product attribute" do
      form_data = {"product" => "New-Product"}

      result = http_multipart_client(
        "/inventory/#{pool_id}/model",
        form_data,
        headers: cookie_header
      )

      expect(result.status).to eq(200)
      expect(result.body["data"].count).to be
      expect(result.body["validation"].count).to eq(0)
    end

    it "creates a model with one image and the product attribute" do
      form_data = {
        "product" => "New-Product",
        "images" => [File.open(path_arrow, "rb")]
      }

      result = http_multipart_client(
        "/inventory/#{pool_id}/model",
        form_data,
        headers: cookie_header
      )

      expect(result.status).to eq(200)
      expect(result.body["data"].count).to be
      expect(result.body["validation"].count).to eq(1)
      expect(result.body["validation"][0]["error"]).to eq("Either image or thumbnail is missing")

      model_id = result.body["data"]["id"]
      expect(Image.where(target_id: model_id).count).to eq(0)
    end

    it "creates a model with one attachment and the product attribute" do
      form_data = {
        "product" => "New-Product",
        "attachments" => [File.open(path_test_pdf, "rb")]
      }

      result = http_multipart_client(
        "/inventory/#{pool_id}/model",
        form_data,
        headers: cookie_header
      )

      expect(result.status).to eq(200)
      expect(result.body["data"].count).to be
      expect(result.body["validation"].count).to eq(0)

      model_id = result.body["data"]["id"]
      expect(Attachment.where(model_id: model_id).count).to eq(1)
    end

    it "creates a model with one attachment and the product attribute" do
      form_data = {
        "product" => "New-Product",
        "attachments" => [File.open(path_test_pdf, "rb"), File.open(path_test_pdf, "rb")]
      }

      result = http_multipart_client(
        "/inventory/#{pool_id}/model",
        form_data,
        headers: cookie_header
      )

      expect(result.status).to eq(200)
      expect(result.body["data"].count).to be
      expect(result.body["validation"].count).to eq(0)

      model_id = result.body["data"]["id"]
      expect(Attachment.where(model_id: model_id).count).to eq(2)
    end

    it "creates a model with one attachment and the product attribute" do
      form_data = {
        "product" => "New-Product",
        "images" => [File.open(path_test_pdf, "rb"), File.open(path_arrow_thumb, "rb")]
      }

      result = http_multipart_client(
        "/inventory/#{pool_id}/model",
        form_data,
        headers: cookie_header
      )

      expect(result.status).to eq(200)
      expect(result.body["data"].count).to be
      expect(result.body["validation"].count).to eq(2)

      model_id = result.body["data"]["id"]
      expect(Image.where(target_id: model_id).count).to eq(0)
    end

    it "creates a model with multiple images and the product attribute" do
      form_data = {
        "product" => "New-Product",
        "images" => [File.open(path_arrow, "rb"), File.open(path_arrow_thumb, "rb")]
      }

      result = http_multipart_client(
        "/inventory/#{pool_id}/model",
        form_data,
        headers: cookie_header
      )

      expect(result.status).to eq(200)
      expect(result.body["data"].count).to be
      expect(result.body["validation"].count).to eq(0)

      model_id = result.body["data"]["id"]
      expect(Image.where(target_id: model_id).count).to eq(2)
    end

    it "creates a model with images and attachments and the product attribute" do
      form_data = {
        "product" => "New-Product",
        "images" => [File.open(path_arrow, "rb"), File.open(path_arrow_thumb, "rb")],
        "attachments" => [File.open(path_test_pdf, "rb")]
      }

      result = http_multipart_client(
        "/inventory/#{pool_id}/model",
        form_data,
        headers: cookie_header
      )

      expect(result.status).to eq(200)
      expect(result.body["data"].count).to eq(16)
      expect(result.body["validation"].count).to eq(0)

      model_id = result.body["data"]["id"]
      expect(Attachment.where(model_id: model_id).count).to eq(1)
    end

    it "creates a model with a single image and the product attribute, missing a thumbnail" do
      form_data = {
        "product" => "New-Product",
        "images" => [File.open(path_arrow, "rb")]
      }

      result = http_multipart_client(
        "/inventory/#{pool_id}/model",
        form_data,
        headers: cookie_header
      )

      expect(result.status).to eq(200)
      expect(result.body["data"].count).to eq(16)
      expect(result.body["validation"].count).to eq(1)
      expect(result.body["validation"][0]["error"]).to eq("Either image or thumbnail is missing")

      model_id = result.body["data"]["id"]
      expect(Image.where(target_id: model_id).count).to eq(0)
    end

    it "creates a model with all available attributes" do
      form_data = {
        "product" => "New-Product",
        "images" => [File.open(path_arrow, "rb"), File.open(path_arrow_thumb, "rb")],
        "attachments" => [File.open(path_test_pdf, "rb")],
        "version" => "v1.0",
        "manufacturer" => "Example Corp",
        "isPackage" => "true",
        "description" => "A sample product",
        "technicalDetails" => "Specs go here",
        "internalDescription" => "Internal notes",
        "importantNotes" => "Important usage notes",
        "entitlements" => [],
        "compatibles" => [],
        "categories" => []
      }

      result = http_multipart_client(
        "/inventory/#{pool_id}/model",
        form_data,
        headers: cookie_header
      )

      expect(result.status).to eq(200)
      expect(result.body["data"].count).to be
      expect(result.body["validation"].count).to eq(0)
      expect(result.body["data"].keys.count).to eq(16)

      model_id = result.body["data"]["id"]
      expect(Image.where(target_id: model_id).count).to eq(2)
    end
  end
end
