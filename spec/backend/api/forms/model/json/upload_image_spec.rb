require "spec_helper"
require "pry"
require_relative "../../../_shared"
require_relative "../../_common"
require "faker"
require "marcel"

describe "Inventory Model" do
  ["inventory_manager", "customer"].each do |role|
    context "when interacting with inventory model as #{role}" do
      include_context :setup_models_api_model, role
      include_context :generate_session_header

      let(:pool_id) { @inventory_pool.id }
      let(:model_id) { @models.first.id }
      let(:cookie_header) { @cookie_header }
      let(:client) { plain_faraday_json_client(cookie_header) }

      let(:path_valid_png) { File.expand_path("spec/files/500-kb.png", Dir.pwd) }
      let(:path_valid_jpg) { File.expand_path("spec/files/600-kb.jpg", Dir.pwd) }
      let(:path_valid_jpeg) { File.expand_path("spec/files/600-kb.jpeg", Dir.pwd) }
      let(:path_valid_pdf) { File.expand_path("spec/files/300-kb.pdf", Dir.pwd) }

      let(:path_invalid_png) { File.expand_path("spec/files/2-mb.png", Dir.pwd) }
      let(:path_invalid_jpg) { File.expand_path("spec/files/2-mb.jpg", Dir.pwd) }
      let(:path_invalid_jpeg) { File.expand_path("spec/files/2-mb.jpeg", Dir.pwd) }
      let(:path_invalid_pdf) { File.expand_path("spec/files/2-mb.pdf", Dir.pwd) }

      before do
        [path_valid_png, path_valid_jpg, path_valid_jpeg, path_valid_pdf,
          path_invalid_png, path_invalid_jpg, path_invalid_jpeg, path_invalid_pdf].each do |path|
          raise "File not found: #{path}" unless File.exist?(path)
        end
      end

      context "image upload" do
        def upload_image(file_path)
          file = File.open(file_path, "rb")
          content_type = Marcel::MimeType.for(file)
          headers = cookie_header.merge(
            "Content-Type" => content_type,
            "X-Filename" => File.basename(file.path),
            "Content-Length" => File.size(file.path).to_s
          )

          response = json_client_post(
            "/inventory/models/#{model_id}/images",
            body: file,
            headers: headers,
            is_binary: true
          )
          file.close
          response
        end

        it "accepts valid image file types" do
          [path_valid_png, path_valid_jpg, path_valid_jpeg].each do |path|
            response = upload_image(path)
            expect(response.status).to eq(200)
          end
        end

        it "rejects unsupported image formats" do
          [path_valid_pdf].each do |path|
            response = upload_image(path)
            expect(response.status).to eq(400)
            expect(response.body["error"]).to eq("Failed to upload image")
          end
        end

        # TODO: Different limit for test env has been dropped. Do we still need this?
        #
        # if ENV["RAILS_ENV"] == "test"
        #   it "rejects images exceeding size limit" do
        #     [path_invalid_png, path_invalid_jpg, path_invalid_jpeg].each do |path|
        #       response = upload_image(path)
        #       expect(response.status).to eq(400)
        #       expect(response.body["error"]).to eq("Failed to upload image")
        #       expect(response.body["details"]).to eq("File size exceeds limit")
        #     end
        #   end
        # end
      end
    end
  end
end
