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
  ["inventory_manager"].each do |role|
    context "when interacting with inventory model as #{role}" do
      include_context :setup_models_api_model, role
      include_context :generate_session_header

      let(:pool_id) { @inventory_pool.id }
      let(:first_model) { @models.first }
      let(:model) { @models.first }
      let(:model_id) { @models.first.id }
      let(:client) { plain_faraday_json_client(cookie_header) }
      let(:cookie_header) { @cookie_header }
      let(:html_client) { plain_faraday_html_client(cookie_header.merge({"Accept" => "text/html"})) }

      let(:form_categories) { @form_categories }
      let(:form_compatible_models) { @form_compatible_models }

      let(:path_valid_png) { File.expand_path("spec/files/500-kb.png", Dir.pwd) }
      let(:path_valid_jpg) { File.expand_path("spec/files/600-kb.jpg", Dir.pwd) }
      let(:path_valid_jpeg) { File.expand_path("spec/files/600-kb.jpeg", Dir.pwd) }
      let(:path_valid_pdf) { File.expand_path("spec/files/300-kb.pdf", Dir.pwd) }
      let(:path_valid_txt) { File.expand_path("spec/files/text-file.txt", Dir.pwd) }

      let(:pool_id) { @inventory_pool.id }

      before do
        [path_valid_png, path_valid_jpg, path_valid_jpeg, path_valid_pdf].each do |path|
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
            upload_and_expect(file_path, true)
          end
        end

        context "upload & fetch attachments" do
          before :each do
            @upload_response = upload_and_expect(path_valid_pdf, true)

            @attachment_id = @upload_response.body["id"]
            expect(@attachment_id).not_to be_nil
          end

          context "upload & fetch attachment" do
            before :each do
              @upload_response = upload_and_expect(path_valid_pdf, true)

              @attachment_id = @upload_response.body["id"]
              expect(@attachment_id).not_to be_nil
            end

            it "with accept text/html" do
              resp = html_client.get "/inventory/#{pool_id}/models/#{model_id}/attachments/#{@attachment_id}"
              expect(resp.status).to eq(200)
            end

            it "with not existing uuid" do
              non_existing_image_id = "00000000-0000-0000-0000-000000000000"

              resp = html_client.get "/inventory/#{pool_id}/models/#{model_id}/attachments/#{non_existing_image_id}"
              expect(resp.status).to eq(404)
              expect(resp.headers["content-type"]).to eq("text/plain; charset=utf-8")
              expect(resp.body).to eq("No attachment found")
            end

            it "with invalid uuid (coercion)" do
              invalid_uuid_coercion_error = "00000000-0000-0000-0000-00000000000s"

              resp = html_client.get "/inventory/#{pool_id}/models/#{model_id}/attachments/#{invalid_uuid_coercion_error}"
              expect(resp.status).to eq(404)
              expect(resp.headers["content-type"]).to eq("text/plain; charset=utf-8")
              expect(resp.body).to eq("Request coercion failed")
            end
          end
        end
      end
    end
  end
end
