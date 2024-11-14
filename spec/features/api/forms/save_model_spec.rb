require "spec_helper"
require "pry"
require_relative "../_shared"
require "uri"
require "net/http"

feature "Inventory Model Management" do
  context "when interacting with inventory models in a specific inventory pool", driver: :selenium_headless do
    include_context :setup_models_api

    let(:pool_id) { @inventory_pool.id }

    let(:file_path) { File.expand_path("spec/files/arrow.png", Dir.pwd) }
    let(:file_path2) { File.expand_path("spec/files/arrow_thumb.png", Dir.pwd) }
    let(:file_path3) { File.expand_path("spec/files/lock.png", Dir.pwd) }

    before do
      raise "File not found: #{file_path}" unless File.exist?(file_path)
      raise "File not found: #{file_path2}" unless File.exist?(file_path2)
      raise "File not found: #{file_path3}" unless File.exist?(file_path2)
    end

    let(:file_io) { Faraday::UploadIO.new(file_path, "image/png") }
    let(:file_io2) { Faraday::UploadIO.new(file_path2, "image/png") }
    let(:file_io3) { Faraday::UploadIO.new(file_path3, "image/png") }

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

      expect(res.status).to eq(200)
      expect(res.body["data"].count).to be
      expect(res.body["validation"].count).to eq(0)
    end

    it "creates a model with one image and the product attribute" do
      res = common_plain_faraday_client(
        :post, "/inventory/#{pool_id}/model",
        multipart: true,
        body: {
          "product" => Faraday::ParamPart.new("New-Product", "text/plain"),
          "images" => [file_io]
        }
      )

      expect(res.status).to eq(200)
      expect(res.body["data"].count).to be
      expect(res.body["validation"].count).to eq(0)
    end

    it "creates a model with one attachment and the product attribute" do
      res = common_plain_faraday_client(
        :post, "/inventory/#{pool_id}/model",
        multipart: true,
        body: {
          "product" => Faraday::ParamPart.new("New-Product", "text/plain"),
          "attachments" => [file_io]
        }
      )

      expect(res.status).to eq(200)
      expect(res.body["data"].count).to be
      expect(res.body["validation"].count).to eq(0)
    end

    it "creates a model with one attachment and the product attribute by encoding" do
      res = common_plain_faraday_client(
        :post,
        "/inventory/#{pool_id}/model",
        multipart: true,
        body: {
          "product" => Faraday::ParamPart.new("New-Product", "text/plain"),
          "attachments" => file_io
        }
      )

      expect(res.status).to eq(200)
      expect(res.body["data"].count).to be
      expect(res.body["validation"].count).to eq(0)
    end

    it "fails to creates a model with one attachment and the product attribute because of missing encoding (FIXME)" do
      res = common_plain_faraday_client(
        :post, "/inventory/#{pool_id}/model",
        multipart: true,
        body: {
          "product" => Faraday::ParamPart.new("New-Product", "text/plain"),
          "images" => file_io
        }
      )

      expect(res.status).to eq(200)
      expect(res.body["data"].count).to be
      expect(res.body["validation"].count).to eq(1)
    end

    it "creates a model with one attachment and the product attribute" do
      res = common_plain_faraday_client(
        :post, "/inventory/#{pool_id}/model",
        multipart: true,
        body: {
          "product" => Faraday::ParamPart.new("New-Product", "text/plain"),
          "attachments" => [file_io, file_io2]
        }
      )

      expect(res.status).to eq(200)
      expect(res.body["data"].count).to be
      expect(res.body["validation"].count).to eq(0)
    end

    it "creates a model with multiple images and the product attribute" do
      res = common_plain_faraday_client(
        :post, "/inventory/#{pool_id}/model",
        multipart: true,
        body: {
          "product" => Faraday::ParamPart.new("New-Product", "text/plain"),
          "images" => [file_io, file_io2]
        }
      )

      expect(res.status).to eq(200)
      expect(res.body["data"].count).to be
      expect(res.body["validation"].count).to eq(0)
    end

    # TODO: This request with upload-files works
    it "creates a model with images, attachments, and the product attribute" do
      url = URI.parse("http://localhost:3260/inventory/8bd16d45-056d-5590-bc7f-12849f034351/model")

      http = Net::HTTP.new(url.host, url.port);
      request = Net::HTTP::Post.new(url)
      request["Accept"] = "application/json"
      form_data = [['product', 'fjdkla22'],['images', File.open('/Users/mradl/Documents/new-item.txt')],['images', File.open('/Users/mradl/Documents/legacy-logs.txt')]]
      request.set_form form_data, 'multipart/form-data'
      res = http.request(request)
      parsed_body = JSON.parse(res.body)

      expect(res.code).to eq("200")
      expect(parsed_body["data"].count).to  eq(16)
      expect(parsed_body["validation"].count).to eq(2)
    end

    it "creates a model with all available attributes" do
      res = common_plain_faraday_client(
        :post, "/inventory/#{pool_id}/model",
        multipart: true,
        body: {
          "product" => Faraday::ParamPart.new("New-Product", "text/plain"),
          "images" => [file_io, file_io2],
          "attachments" => [file_io3],
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
      expect(res.body["data"].count).to be
      expect(res.body["validation"].count).to eq(0)
      expect(res.body["data"].keys.count).to eq(16)
      
      expect(res.body["data"].keys).to eq( ["description", "is_package", "maintenance_period", "type", "rental_price",
                                            "cover_image_id", "hand_over_note", "updated_at", "internal_description",
                                            "product", "info_url", "id", "manufacturer", "version", "created_at",
                                            "technical_detail"])
    end
  end
end
