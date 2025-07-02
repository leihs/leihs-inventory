require "spec_helper"
require "pry"
require_relative "../../../_shared"
require_relative "../../_common"
require "faker"
require "marcel"

def upload_and_expect(file_path, expected_ok)
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

describe "Inventory Model" do
  # ['inventory_manager', 'customer'].each do |role|
  ['inventory_manager'].each do |role|
    context "when interacting with inventory model as #{role}" do
      include_context :setup_models_api_model, role
      include_context :generate_session_header

      let(:pool_id) { @inventory_pool.id }
      let(:model_id) { @models.first.id }
      let(:client) { plain_faraday_json_client(cookie_header) }
      let(:cookie_header) { @cookie_header }

      let(:form_categories) { @form_categories }
      let(:form_compatible_models) { @form_compatible_models }

      let(:path_valid_png) { File.expand_path("spec/files/500-kb.png", Dir.pwd) }
      let(:path_valid_jpg) { File.expand_path("spec/files/600-kb.jpg", Dir.pwd) }
      let(:path_valid_jpeg) { File.expand_path("spec/files/600-kb.jpeg", Dir.pwd) }
      let(:path_valid_pdf) { File.expand_path("spec/files/300-kb.pdf", Dir.pwd) }

      let(:path_invalid_png) { File.expand_path("spec/files/2-mb.png", Dir.pwd) }
      let(:path_invalid_jpg) { File.expand_path("spec/files/2-mb.jpg", Dir.pwd) }
      let(:path_invalid_jpeg) { File.expand_path("spec/files/2-mb.jpeg", Dir.pwd) }
      let(:path_invalid_pdf) { File.expand_path("spec/files/2-mb.pdf", Dir.pwd) }

      let(:pool_id) { @inventory_pool.id }

      before do
        # Ensure all fixture files exist
        [path_valid_png, path_valid_jpg, path_valid_jpeg, path_valid_pdf,
          path_invalid_png, path_invalid_jpg, path_invalid_jpeg, path_invalid_pdf].each do |path|
          raise "File not found: #{path}" unless File.exist?(path)
        end
      end

      def convert_to_id_correction(compatibles)
        compatibles.each do |item|
          item["id"] = item.delete("model_id")
        end
      end

      context "upload attachments" do
        it "uploads valid attachment types and sizes" do
          [path_valid_pdf].each do |file_path|
            upload_and_expect(file_path, true)
          end
        end

        it "rejects invalid attachment file types" do
          [path_valid_png, path_valid_jpg, path_valid_jpeg].each do |file_path|
            upload_and_expect(file_path, false)
          end
        end

        context "upload & fetch attachments" do
          before :each do
            @upload_response = upload_and_expect(path_valid_pdf, true)
          end

          it "fetches attachment" do
            image_id = @upload_response.body[0]["id"]

            puts "pool_id: #{pool_id}"
            puts "model_id: #{model_id}"
            puts "image_id: #{image_id}"

            resp = client.get "/inventory/#{pool_id}/models/#{model_id}/attachments/#{image_id}"
            expect(resp.status).to eq(200)
          end
        end
      end
    end
  end
end
