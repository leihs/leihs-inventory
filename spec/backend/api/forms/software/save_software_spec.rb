require "spec_helper"
require "pry"
require_relative "../../_shared"
require "marcel"

def upload_and_expect(file_path, model_id, expected_ok)
  File.open(file_path, "rb") do |file|
    content_type = Marcel::MimeType.for(file)
    headers = cookie_header.merge(
      "Content-Type" => content_type,
      "X-Filename" => File.basename(file.path),
      "Content-Length" => file.size.to_s
    )

    response = json_client_post(
      "/inventory/#{@inventory_pool.id}/models/#{model_id}/attachments/",
      body: file,
      headers: headers,
      is_binary: true
    )

    if expected_ok
      expect(response.status).to eq(200)
    else
      expect(response.status).to eq(400)
      expect(response.body["message"]).to eq("Failed to upload attachment")
    end
    response
  end
end

describe "Inventory Model Management" do
  context "when interacting with inventory models in a specific inventory pool" do
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

      resp = json_client_post(
        "/inventory/#{pool_id}/software/",
        body: form_data,
        headers: cookie_header
      )

      expect(resp.status).to eq(200)
      expect(resp.body).to be
    end

    it "creates a model with one attachment and the product attribute" do
      form_data = {
        "product" => "New-Product"
      }

      resp = json_client_post(
        "/inventory/#{pool_id}/software/",
        body: form_data,
        headers: cookie_header
      )
      expect(resp.status).to eq(200)
      expect(resp.body).to be

      model_id = resp.body["id"]
      [path_test_pdf].each do |file_path|
        upload_and_expect(file_path, model_id, true)
      end

      expect(Attachment.where(model_id: model_id).count).to eq(1)
    end

    it "creates a model with one attachment and the product attribute" do
      form_data = {
        "product" => "New-Product"
      }

      # create software
      resp = json_client_post(
        "/inventory/#{pool_id}/software/",
        body: form_data,
        headers: cookie_header
      )
      expect(resp.status).to eq(200)
      expect(resp.body).to be

      # upload attachments
      model_id = resp.body["id"]
      [path_test_pdf, path_test_pdf].each do |file_path|
        upload_and_expect(file_path, model_id, true)
      end
      expect(Attachment.where(model_id: model_id).count).to eq(2)

      # fetch software
      resp = json_client_get(
        "/inventory/#{pool_id}/software/#{model_id}",
        headers: cookie_header
      )
      expect(resp.status).to eq(200)
      expect(resp.body["attachments"].first.keys).to eq(["content_type", "filename", "id", "url"])
      expect(resp.body.keys).to eq(["attachments", "type", "product", "id", "manufacturer", "is_deletable", "version",
        "technical_detail"])
    end

    it "creates a model with all available attributes" do
      form_data = {
        "product" => "New-Product",
        "attachments" => [File.open(path_test_pdf, "rb")],
        "version" => "v1.0",
        "manufacturer" => "Example Corp",
        "technical_details" => "Specs go here"
      }

      resp = json_client_post(
        "/inventory/#{pool_id}/software/",
        body: form_data,
        headers: cookie_header
      )
      expect(resp.status).to eq(200)
      expect(resp.body).to be

      model_id = resp.body["id"]
      [path_test_pdf].each do |file_path|
        upload_and_expect(file_path, model_id, true)
      end
      expect(Attachment.where(model_id: model_id).count).to eq(1)
    end
  end
end
