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

def expect_correct_url(url)
  resp = client.get url
  expect(resp.status).to eq(200)
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

          it "fetches attachment" do
            resp = client.get "/inventory/#{pool_id}/models/#{model_id}/attachments/#{@attachment_id}"
            expect(resp.status).to eq(200)
          end

          it "verify attachment exists" do
            resp = client.get "/inventory/#{pool_id}/models/#{model_id}"
            expect(resp.status).to eq(200)
            expect(resp.body["attachments"][0]["url"]).to eq("/inventory/#{pool_id}/models/#{model_id}/attachments/#{@attachment_id}")
            expect_correct_url(resp.body["attachments"][0]["url"])
          end
        end

        context "fetch not existing attachment" do
          it "with accept application/json" do
            resp = client.get "/inventory/#{pool_id}/models/#{model_id}/attachments/11111111-1111-1111-1111-111111111111"
            expect(resp.status).to eq(404)
            expect(resp.body["details"]).to eq("No attachment found")
          end

          it "with correct accept-type" do
            ["application/pdf"].each do |accept_type|
              client = plain_faraday_json_client(cookie_header.merge({"Accept" => accept_type}))
              resp = client.get "/inventory/#{pool_id}/models/#{model_id}/attachments/11111111-1111-1111-1111-111111111111"

              expect(resp.status).to eq(404)
              expect(resp.body["details"]).to eq("No attachment found")
            end
          end
        end

        context "upload & fetch attachment" do
          before :each do
            @upload_response = upload_and_expect(path_valid_pdf, true)

            @attachment_id = @upload_response.body["id"]
            expect(@attachment_id).not_to be_nil
          end

          it "with accept application/json" do
            resp = client.get "/inventory/#{pool_id}/models/#{model_id}/attachments/#{@attachment_id}"

            expect(resp.status).to eq(200)
            expect(resp.body["id"]).to eq(@attachment_id)
          end

          it "with correct accept-type" do
            ["application/pdf"].each do |accept_type|
              client = plain_faraday_json_client(cookie_header.merge({"Accept" => accept_type}))
              resp = client.get "/inventory/#{pool_id}/models/#{model_id}/attachments/#{@attachment_id}"

              expect(resp.status).to eq(200)
            end
          end

          it "with incorrect accept-type" do
            ["image/png", "image/jpeg", "text/plain", "image/gif", "text/rtf",
              "image/vnd.dwg", "application/zip"].each do |accept_type|
              client = plain_faraday_json_client(cookie_header.merge({"Accept" => accept_type}))
              resp = client.get "/inventory/#{pool_id}/models/#{model_id}/attachments/#{@attachment_id}"

              expect(resp.status).to eq(406)
            end
          end

          it "with invalid accept-type" do
            ["text/html"].each do |accept_type|
              client = plain_faraday_json_client(cookie_header.merge({"Accept" => accept_type}))
              resp = client.get "/inventory/#{pool_id}/models/#{model_id}/attachments/#{@attachment_id}"

              expect(resp.status).to eq(406)
            end
          end
        end

        context "upload different types & fetch attachment as CHROME" do
          it "with content negotiation" do
            [path_valid_pdf, path_valid_png, path_valid_jpg, path_valid_jpeg, path_valid_txt].each do |file|
              upload_response = upload_and_expect(file, true)
              attachment_id = upload_response.body["id"]

              generic_accept_type = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng," \
                "*/*;q=0.8,application/signed-exchange;v=b3;q=0.7"
              client = plain_faraday_json_client(cookie_header.merge({"Accept" => generic_accept_type}))

              resp = client.get "/inventory/#{pool_id}/models/#{model_id}/attachments/#{attachment_id}"
              expect(resp.status).to eq(200)
            end
          end
        end

        context "upload different types & fetch attachment as FF" do
          it "with content negotiation" do
            [path_valid_pdf, path_valid_png, path_valid_jpg, path_valid_jpeg, path_valid_txt].each do |file|
              upload_response = upload_and_expect(file, true)
              attachment_id = upload_response.body["id"]

              generic_accept_type = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"
              client = plain_faraday_json_client(cookie_header.merge({"Accept" => generic_accept_type}))

              resp = client.get "/inventory/#{pool_id}/models/#{model_id}/attachments/#{attachment_id}"
              expect(resp.status).to eq(200)
            end
          end
        end
      end
    end
  end
end
