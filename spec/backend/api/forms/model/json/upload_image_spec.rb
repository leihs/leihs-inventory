require "spec_helper"
require_relative "../../../_shared"
require_relative "../../_common"
require "faker"
require "marcel"

describe "Inventory Model" do
  ["inventory_manager"].each do |role|
    context "when interacting with inventory model with role=#{role}" do
      include_context :setup_models_api_model, role
      include_context :generate_session_header

      let(:pool_id) { @inventory_pool.id }
      let(:model_id) { @models.first.id }
      let(:client) { plain_faraday_json_client(@cookie_header) }

      let(:valid_files) do
        [
          "spec/files/500-kb.png",
          "spec/files/600-kb.jpg",
          "spec/files/600-kb.jpeg"
        ].map { |path| File.expand_path(path, Dir.pwd) }
      end

      let(:invalid_files) do
        [
          "spec/files/2-mb.png",
          "spec/files/2-mb.jpg",
          "spec/files/2-mb.jpeg",
          "spec/files/2-mb.pdf"
        ].map { |path| File.expand_path(path, Dir.pwd) }
      end

      before do
        (valid_files + invalid_files).each do |path|
          raise "File not found: #{path}" unless File.exist?(path)
        end
      end

      def upload_images(images, expected_status, expected_error = nil, expected_details = nil)
        images.each do |image|
          content_type = Marcel::MimeType.for(image)
          headers = @cookie_header.merge(
            "Content-Type" => content_type,
            "X-Filename" => File.basename(image.path),
            "Content-Length" => File.size(image.path).to_s
          )

          resp = json_client_post(
            "/inventory/models/#{model_id}/images",
            body: image,
            headers: headers,
            is_binary: true
          )

          expect(resp.status).to eq(expected_status)
          if expected_error
            expect(resp.body["error"]).to eq(expected_error)
            expect(resp.body["details"]).to eq(expected_details) if expected_details
          end
        end
      end

      context "upload images" do
        it "uploads valid file types" do
          valid_files.each { |path| upload_images([File.open(path, "rb")], 200) }
        end

        it "rejects invalid file types" do
          invalid_files.each do |path|
            upload_images([File.open(path, "rb")], 400, "Failed to upload image")
          end
        end

        it "rejects files exceeding size limit" do
          oversized_files = invalid_files.select { |path| File.size(path) > 1_000_000 }
          oversized_files.each do |path|
            upload_images([File.open(path, "rb")], 400, "Failed to upload image", "File size exceeds limit")
          end
        end
      end
    end
  end
end
