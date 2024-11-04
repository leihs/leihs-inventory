require "spec_helper"
require "pry"
require_relative "../_shared"

feature "Inventory Model Management" do
  context "when interacting with inventory models in a specific inventory pool", driver: :selenium_headless do
    include_context :setup_models_api

    let(:pool_id) { @inventory_pool.id }

    let(:file_path) { File.expand_path("spec/files/arrow.png", Dir.pwd) }
    let(:file_path2) { File.expand_path("spec/files/lock.png", Dir.pwd) }

    before do
      raise "File not found: #{file_path}" unless File.exist?(file_path)
      raise "File not found: #{file_path2}" unless File.exist?(file_path2)
    end

    let(:file_io) { Faraday::UploadIO.new(file_path, "image/png") }
    let(:file_io2) { Faraday::UploadIO.new(file_path2, "image/png") }

    it "creates a model with only the required product attribute" do
      res = common_plain_faraday_client(
        :post,
        "/inventory/#{pool_id}/model",
        multipart: true,
        # headers: { "Accept" => "multipart/form-data" },
        body: {
          "product" => Faraday::ParamPart.new("New-Product", "text/plain")
        }
      )



      # binding.pry
      expect(res.status).to eq(200)
      expect(res.body["params-keys"].count).to eq(1)
      expect(res.body["params-keys"]).to eq(["product"])
    end

    it "creates a model with one image and the product attribute" do
      res = common_plain_faraday_client(
        :post, "/inventory/#{pool_id}/model",
        multipart: true,
        body: {
          "product" => Faraday::ParamPart.new("New-Product", "text/plain"),
          "images" => file_io
        }
      )

      expect(res.status).to eq(200)
      expect(res.body["params-keys"].count).to eq(2)
      expect(res.body["params-keys"]).to eq(["images", "product"])
    end

    it "creates a model with multiple images and the product attribute" do
      res = common_plain_faraday_client(
        :post, "/inventory/#{pool_id}/model",
        multipart: true,
        body: {
          "product" => Faraday::ParamPart.new("New-Product", "text/plain"),
          "images" => file_io,
          "images" => file_io2
        }
      )

      expect(res.status).to eq(200)
      expect(res.body["params-keys"].count).to eq(2)
      expect(res.body["params-keys"]).to eq(["images", "product"])
    end

    it "creates a model with images, attachments, and the product attribute" do
      res = common_plain_faraday_client(
        :post, "/inventory/#{pool_id}/model",
        multipart: true,
        body: {
          "product" => Faraday::ParamPart.new("New-Product", "text/plain"),
          "images" => file_io,
          "images" => file_io2,
          "attachments" => file_io
        }
      )

      expect(res.status).to eq(200)
      expect(res.body["params-keys"].count).to eq(3)
      expect(res.body["params-keys"]).to eq(["images", "attachments", "product"])
    end

    it "creates a model with all available attributes" do
      res = common_plain_faraday_client(
        :post, "/inventory/#{pool_id}/model",
        multipart: true,
        body: {
          "product" => Faraday::ParamPart.new("New-Product", "text/plain"),
          "images" => file_io,
          "images" => file_io2,
          "attachments" => file_io,
          "version" => Faraday::ParamPart.new("v1.0", "text/plain"),
          "manufacturer" => Faraday::ParamPart.new("Example Corp", "text/plain"),
          "isPackage" => Faraday::ParamPart.new("true", "text/plain"),
          "description" => Faraday::ParamPart.new("A sample product", "text/plain"),
          "technicalDetails" => Faraday::ParamPart.new("Specs go here", "text/plain"),
          "internalDescription" => Faraday::ParamPart.new("Internal notes", "text/plain"),
          "importantNotes" => Faraday::ParamPart.new("Important usage notes", "text/plain"),
          "compatibles" => Faraday::ParamPart.new("compatible item codes", "text/plain"),
          "allocations" => Faraday::ParamPart.new("allocation details", "text/plain"),
          "categories" => Faraday::ParamPart.new("category1,category2", "text/plain")
        }
      )

      expect(res.status).to eq(200)
      expect(res.body["params-keys"].count).to eq(13)
      expect(res.body["params-keys"]).to eq(["description", "importantNotes", "images", "attachments", "product",
        "categories", "technicalDetails", "internalDescription", "isPackage",
        "allocations", "compatibles", "manufacturer", "version"])
    end
  end
end
