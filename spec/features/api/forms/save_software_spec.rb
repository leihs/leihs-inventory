require "spec_helper"
require "pry"
require_relative "../_shared"

feature "Inventory Model Management" do
  context "when interacting with inventory models in a specific inventory pool", driver: :selenium_headless do
    include_context :setup_models_api

    let(:pool_id) { @inventory_pool.id }

    let(:path_test_pdf) { File.expand_path("spec/files/test.pdf", Dir.pwd) }
    let(:path_test_txt) { File.expand_path("spec/files/text-file.txt", Dir.pwd) }

    before do
      [path_test_pdf].each do |path|
        raise "File not found: #{path}" unless File.exist?(path)
      end
    end

    it "creates a model with only the required product attribute" do
      form_data = {"product" => "New-Product"}

      result = http_multipart_client(
        "/inventory/#{pool_id}/software",
        form_data
      )

      expect(result.status).to eq(200)
      expect(result.body["data"].count).to be
      expect(result.body["validation"].count).to eq(0)
    end

    it "creates a model with one attachment and the product attribute" do
      form_data = {
        "product" => "New-Product",
        "attachments" => [File.open(path_test_pdf, "rb")]
      }

      result = http_multipart_client(
        "/inventory/#{pool_id}/software",
        form_data
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
        "/inventory/#{pool_id}/software",
        form_data
      )

      expect(result.status).to eq(200)
      expect(result.body["data"].count).to be
      expect(result.body["validation"].count).to eq(0)

      model_id = result.body["data"]["id"]
      expect(Attachment.where(model_id: model_id).count).to eq(2)
    end

    it "creates a model with all available attributes" do
      form_data = {
        "product" => "New-Product",
        "attachments" => [File.open(path_test_pdf, "rb")],
        "version" => "v1.0",
        "manufacturer" => "Example Corp",
        # "isPackage" => "true",
        # "description" => "A sample product",
        "technicalDetails" => "Specs go here"
        # "internalDescription" => "Internal notes",
        # "importantNotes" => "Important usage notes",
        # "entitlements" => [],
        # "compatibles" => [],
        # "categories" => []
      }

      result = http_multipart_client(
        "/inventory/#{pool_id}/software",
        form_data
      )

      expect(result.status).to eq(200)
      expect(result.body["data"].count).to be
      expect(result.body["validation"].count).to eq(0)
      expect(result.body["data"].keys.count).to eq(16)

      model_id = result.body["data"]["id"]
      expect(Attachment.where(model_id: model_id).count).to eq(1)
    end
  end
end
