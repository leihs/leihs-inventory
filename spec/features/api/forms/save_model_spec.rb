require "spec_helper"
require "pry"
require_relative "../_shared"

feature "Inventory Model Management" do
  context "when interacting with inventory models in a specific inventory pool", driver: :selenium_headless do
    include_context :setup_models_api

    let(:pool_id) { @inventory_pool.id }

    let(:file_path) { File.expand_path("spec/files/arrow.png", Dir.pwd) }
    let(:file_path2) { File.expand_path("spec/files/arrow_thumb.png", Dir.pwd) }
    let(:file_path3) { File.expand_path("spec/files/lock.png", Dir.pwd) }

    before do
      [file_path, file_path2, file_path3].each do |path|
        raise "File not found: #{path}" unless File.exist?(path)
      end
    end

    it "creates a model with only the required product attribute" do
      form_data = { "product" => "New-Product" }

      result = http_multipart_client(
        "/inventory/#{pool_id}/model",
        form_data
      )

      expect(result.status).to eq(200)
      expect(result.body["data"].count).to be
      expect(result.body["validation"].count).to eq(0)
    end

    it "creates a model with one image and the product attribute" do
      form_data = {
        "product" => "New-Product",
        "images" => [File.open(file_path, "rb")]
      }

      result = http_multipart_client(
        "/inventory/#{pool_id}/model",
        form_data
      )

      expect(result.status).to eq(200)
      expect(result.body["data"].count).to be
      expect(result.body["validation"].count).to eq(1)
      expect(result.body["validation"][0]["error"]).to eq("Either image or thumbnail is missing")
    end

    it "creates a model with one attachment and the product attribute" do
      form_data = {
        "product" => "New-Product",
        "attachments" => [File.open(file_path3, "rb")]
      }

      result = http_multipart_client(
        "/inventory/#{pool_id}/model",
        form_data
      )

      expect(result.status).to eq(200)
      expect(result.body["data"].count).to be
      expect(result.body["validation"].count).to eq(0)
    end

    it "creates a model with multiple images and the product attribute" do
      form_data = {
        "product" => "New-Product",
        "images" => [File.open(file_path, "rb"), File.open(file_path2, "rb")]
      }

      result = http_multipart_client(
        "/inventory/#{pool_id}/model",
        form_data
      )

      expect(result.status).to eq(200)
      expect(result.body["data"].count).to be
      expect(result.body["validation"].count).to eq(0)
    end

    it "creates a model with images and attachments and the product attribute" do
      form_data = {
        "product" => "fjdkla22",
        "images" => [File.open(file_path, "rb"), File.open(file_path2, "rb")],
        "attachments" => [File.open(file_path3, "rb")]
      }

      result = http_multipart_client(
        "/inventory/#{pool_id}/model",
        form_data
      )

      expect(result.status).to eq(200)
      expect(result.body["data"].count).to eq(16)
      expect(result.body["validation"].count).to eq(0)
    end

    it "creates a model with a single image and the product attribute, missing a thumbnail" do
      form_data = {
        "product" => "fjdkla22",
        "images" => [File.open(file_path, "rb")]
      }

      result = http_multipart_client(
        "/inventory/#{pool_id}/model",
        form_data
      )

      expect(result.status).to eq(200)
      expect(result.body["data"].count).to eq(16)
      expect(result.body["validation"].count).to eq(1)
      expect(result.body["validation"][0]["error"]).to eq("Either image or thumbnail is missing")
    end

    it "creates a model with all available attributes" do
      form_data = {
        "product" => "New-Product",
        "images" => [File.open(file_path, "rb"), File.open(file_path2, "rb")],
        "attachments" => [File.open(file_path3, "rb")],
        "version" => "v1.0",
        "manufacturer" => "Example Corp",
        "isPackage" => "true",
        "description" => "A sample product",
        "technicalDetails" => "Specs go here",
        "internalDescription" => "Internal notes",
        "importantNotes" => "Important usage notes",
        "compatibles" => "compatible item codes",
        "allocations" => "allocation details",
        "categories" => "category1,category2"
      }

      result = http_multipart_client(
        "/inventory/#{pool_id}/model",
        form_data
      )

      expect(result.status).to eq(200)
      expect(result.body["data"].count).to be
      expect(result.body["validation"].count).to eq(0)
      expect(result.body["data"].keys.count).to eq(16)
    end
  end
end
