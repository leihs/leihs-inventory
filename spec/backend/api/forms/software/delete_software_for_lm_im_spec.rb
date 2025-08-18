require "spec_helper"
require "pry"
require_relative "../../_shared"
require_relative "../_common"
require "faker"
require "marcel"

post_response = {
  "description" => [NilClass, String],
  "is_package" => [TrueClass, FalseClass],
  "maintenance_period" => [NilClass, Integer],
  "type" => String,
  "rental_price" => [NilClass, Numeric],
  "cover_image_id" => [NilClass, String],
  "hand_over_note" => [NilClass, String],
  "updated_at" => String,
  "internal_description" => [NilClass, String],
  "product" => String,
  "info_url" => [NilClass, String],
  "id" => String,
  "manufacturer" => [NilClass, String],
  "version" => [NilClass, String],
  "created_at" => String,
  "technical_detail" => [NilClass, String]
}

get_response = {
  "description" => [NilClass, String],
  "is_package" => [TrueClass, FalseClass],
  "attachments" => Array,
  "type" => String,
  "hand_over_note" => [NilClass, String],
  "internal_description" => [NilClass, String],
  "product" => String,
  "id" => String,
  "manufacturer" => [NilClass, String],
  "version" => [NilClass, String],
  "technical_detail" => [NilClass, String],
  "is_deletable" => [TrueClass, FalseClass]
}

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
      expect(response.body["error"]).to eq("Failed to upload attachment")
    end
    response
  end
end

def expect_correct_url(url)
  resp = client.get url
  expect(resp.status).to eq(200)
end

describe "Inventory Software" do
  ["inventory_manager", "lending_manager"].each do |role|
    context "when interacting with inventory software with role=#{role}" do
      include_context :setup_models_api_model, role
      include_context :generate_session_header

      let(:pool_id) { @inventory_pool.id }
      let(:cookie_header) { @cookie_header }
      let(:client) { plain_faraday_json_client(cookie_header) }

      let(:path_test_pdf) { File.expand_path("spec/files/test.pdf", Dir.pwd) }
      let(:path_test2_pdf) { File.expand_path("spec/files/test2.pdf", Dir.pwd) }
      let(:path_test_txt) { File.expand_path("spec/files/text-file.txt", Dir.pwd) }

      before do
        [path_test_pdf, path_test_txt].each do |path|
          raise "File not found: #{path}" unless File.exist?(path)
        end

        # Fetch shared data and set global instance variables
        resp = client.get "/inventory/#{pool_id}/manufacturers/?type=Software"
        @form_manufacturers = resp.body
        raise "Failed to fetch manufacturers" unless resp.status == 200
      end

      context "fetch form data" do
        it "ensures form manufacturer data is fetched" do
          expect(@form_manufacturers).not_to be_nil
          expect(@form_manufacturers.count).to eq(1)
        end
      end

      it "delete software with all available attributes" do
        # create software request
        form_data = {
          "product" => Faker::Commerce.product_name,
          # "attachments" => [File.open(path_test_pdf, "rb"), File.open(path_test2_pdf, "rb")],
          "version" => "v1.0",
          "manufacturer" => @form_manufacturers.first,
          "technical_details" => "Specs go here"
        }

        resp = json_client_post(
          "/inventory/#{pool_id}/software/",
          body: form_data,
          headers: cookie_header
        )
        expect(resp.status).to eq(200)
        validate_map_structure(resp.body, post_response)

        model_id = resp.body["id"]
        [File.open(path_test_pdf, "rb"), File.open(path_test2_pdf, "rb")].each do |file_path|
          upload_and_expect(file_path, model_id, true)
        end

        # fetch created software
        model_id = resp.body["id"]
        resp = client.get "/inventory/#{pool_id}/software/#{model_id}"

        validate_map_structure(resp.body, get_response)
        attachments = resp.body["attachments"]
        expect(attachments.count).to eq(2)
        expect(resp.status).to eq(200)
        expect(Attachment.where(model_id: model_id).count).to eq(2)

        # delete software request
        resp = json_client_delete(
          "/inventory/#{pool_id}/software/#{model_id}",
          headers: cookie_header
        )
        expect(resp.status).to eq(200)
        expect(resp.body["deleted_attachments"].count).to eq(2)
        expect(resp.body["deleted_model"].count).to eq(1)

        # retry to delete not existing software
        resp = json_client_delete(
          "/inventory/#{pool_id}/software/#{model_id}",
          headers: cookie_header
        )
        expect(resp.status).to eq(404)
        expect(resp.body["error"]).to eq("Request to delete software blocked: software not found")

        # fetch deleted model
        resp = client.get "/inventory/#{pool_id}/software/#{model_id}"
        expect(resp.body["error"]).to eq("Failed to fetch software")
        expect(resp.status).to eq(404)
      end
    end
  end
end
