require "spec_helper"
require "pry"
require_relative "../_shared"

feature "Inventory Model Management" do
  context "when interacting with inventory models in a specific inventory pool", driver: :selenium_headless do
    include_context :setup_models_api

    let(:pool_id) { @inventory_pool.id }

    let(:path_arrow) { File.expand_path("spec/files/arrow.png", Dir.pwd) }
    let(:path_arrow_thumb) { File.expand_path("spec/files/arrow_thumb.png", Dir.pwd) }
    let(:path_test_pdf) { File.expand_path("spec/files/test.pdf", Dir.pwd) }
    let(:path_test_txt) { File.expand_path("spec/files/text-file.txt", Dir.pwd) }

    before do
      [path_arrow, path_arrow_thumb, path_test_pdf].each do |path|
        raise "File not found: #{path}" unless File.exist?(path)
      end
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

      model_id = result.body["data"]["id"]
      expect(Image.where(target_id: model_id).count).to eq(2)
    end
  end
end
