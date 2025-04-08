require "spec_helper"
require "pry"
require_relative "../../_shared"

feature "Inventory Model Management" do
  context "when interacting with inventory models in a specific inventory pool", driver: :selenium_headless do
    include_context :setup_models_api, "inventory_manager"
    include_context :generate_session_header

    let(:pool_id) { @inventory_pool.id }
    let(:cookie_header) { @cookie_header }

    let(:path_test_pdf) { File.expand_path("spec/files/test.pdf", Dir.pwd) }
    let(:path_test_txt) { File.expand_path("spec/files/text-file.txt", Dir.pwd) }

    before do
      [path_test_pdf].each do |path|
        raise "File not found: #{path}" unless File.exist?(path)
      end
    end

    it "creates a model with only the required product attribute" do
      form_data = {"product" => "New-Product"}

      resp = http_multipart_client(
        "/inventory/#{pool_id}/software",
        form_data,
        headers: cookie_header
      )

      expect(resp.status).to eq(200)
      expect(resp.body["data"].count).to be
      expect(resp.body["validation"].count).to eq(0)
    end

    it "creates a model with one attachment and the product attribute" do
      form_data = {
        "product" => "New-Product",
        "attachments" => [File.open(path_test_pdf, "rb")]
      }

      resp = http_multipart_client(
        "/inventory/#{pool_id}/software",
        form_data,
        headers: cookie_header
      )

      expect(resp.status).to eq(200)
      expect(resp.body["data"].count).to be
      expect(resp.body["validation"].count).to eq(0)

      model_id = resp.body["data"]["id"]
      expect(Attachment.where(model_id: model_id).count).to eq(1)
    end

    it "creates a model with one attachment and the product attribute" do
      form_data = {
        "product" => "New-Product",
        "attachments" => [File.open(path_test_pdf, "rb"), File.open(path_test_pdf, "rb")]
      }

      resp = http_multipart_client(
        "/inventory/#{pool_id}/software",
        form_data,
        headers: cookie_header
      )

      expect(resp.status).to eq(200)
      expect(resp.body["data"].count).to be
      expect(resp.body["validation"].count).to eq(0)

      model_id = resp.body["data"]["id"]
      expect(Attachment.where(model_id: model_id).count).to eq(2)
    end

    it "creates a model with all available attributes" do
      form_data = {
        "product" => "New-Product",
        "attachments" => [File.open(path_test_pdf, "rb")],
        "version" => "v1.0",
        "manufacturer" => "Example Corp",
        "technical_details" => "Specs go here"
      }

      resp = http_multipart_client(
        "/inventory/#{pool_id}/software",
        form_data,
        headers: cookie_header
      )

      expect(resp.status).to eq(200)
      expect(resp.body["data"].count).to be
      expect(resp.body["validation"].count).to eq(0)
      expect(resp.body["data"].keys.count).to eq(16)

      model_id = resp.body["data"]["id"]
      expect(Attachment.where(model_id: model_id).count).to eq(1)
    end
  end
end
