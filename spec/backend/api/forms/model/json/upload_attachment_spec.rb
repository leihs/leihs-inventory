require "spec_helper"
require "pry"
require_relative "../../../_shared"
require_relative "../../_common"
require "faker"
require "marcel"

describe "Inventory Model" do
  # ["inventory_manager", "lending_manager"].each do |role|
  ["inventory_manager"].each do |role|
    context "when interacting with inventory model with role=#{role}" do
      include_context :setup_models_api_model, role
      include_context :generate_session_header

      let(:pool_id) { @inventory_pool.id }
      let(:model_id) { @models.first.id }
      let(:cookie_header) { @cookie_header }
      let(:client) { plain_faraday_json_client(cookie_header) }

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

      let(:path_arrow) { File.expand_path("spec/files/arrow.png", Dir.pwd) }
      let(:path_arrow_thumb) { File.expand_path("spec/files/arrow_thumb.png", Dir.pwd) }
      let(:path_test_pdf) { File.expand_path("spec/files/test.pdf", Dir.pwd) }
      let(:path_test2_pdf) { File.expand_path("spec/files/test2.pdf", Dir.pwd) }
      let(:path_test_txt) { File.expand_path("spec/files/text-file.txt", Dir.pwd) }

      before do
        [path_arrow, path_arrow_thumb, path_test_pdf, path_test2_pdf, path_test_txt].each do |path|
          raise "File not found: #{path}" unless File.exist?(path)
        end

        # Fetch shared data and set global instance variables
        resp = client.get "/inventory/manufacturers?type=Model"
        @form_manufacturers = resp.body
        raise "Failed to fetch manufacturers" unless resp.status == 200

        resp = client.get "/inventory/#{pool_id}/entitlement-groups"
        @form_entitlement_groups = resp.body
        raise "Failed to fetch entitlement groups" unless resp.status == 200

        resp = client.get "/inventory/models-compatibles"
        @form_models_compatibles = convert_to_id_correction(resp.body)
        raise "Failed to fetch compatible models" unless resp.status == 200

        resp = client.get "/inventory/#{pool_id}/model-groups"
        @form_model_groups = resp.body
        raise "Failed to fetch model groups" unless resp.status == 200
      end

      context "fetch form data" do
        it "ensures form manufacturer data is fetched" do
          expect(@form_manufacturers).not_to be_nil
          expect(@form_manufacturers.count).to eq(2)
        end

        it "ensures entitlement groups data is fetched" do
          expect(@form_entitlement_groups).not_to be_nil
          expect(@form_entitlement_groups.count).to eq(2)
        end

        it "ensures models compatible data is fetched" do
          expect(@form_models_compatibles).not_to be_nil
          expect(@form_models_compatibles.count).to eq(3)
        end

        it "ensures model groups data is fetched" do
          expect(@form_model_groups).not_to be_nil
          expect(@form_model_groups.count).to eq(2)
        end
      end

      def convert_to_id_correction(compatibles)
        compatibles.each do |compatible|
          compatible["id"] = compatible.delete("model_id")
        end
      end

      context "upload images" do
        it "upload valid file-types" do
          # create image
          images = [
            # File.open(path_valid_png, "rb"),
            # File.open(path_valid_jpg, "rb"),
            # File.open(path_valid_jpeg, "rb")
            File.open(path_valid_pdf, "rb")
          ]
          @image_id = nil
          images.each do |image|
            # binding.pry
            content_type = Marcel::MimeType.for(image)

            headers = cookie_header.merge(
              # "Content-Type" => "image/png",
              "Content-Type" => content_type,

              "X-Filename" => File.basename(image.path),
              "Content-Length" => File.size(image.path).to_s
            )

            puts "headers #{headers}"

            resp = json_client_post(
              "/inventory/models/#{model_id}/attachments",
              body: image,
              headers: headers,
              is_binary: true
            )

            puts "Image response: #{resp.body}"

            binding.pry
            expect(resp.status).to eq(200)

            # @image_id = resp.body["image"]["id"]
          end
        end

        it "upload invalid file-types" do
          # create image
          images = [
            File.open(path_valid_png, "rb"),
            File.open(path_valid_jpg, "rb"),
            File.open(path_valid_jpeg, "rb")
            # File.open(path_invalid_pdf, "rb"),
            # File.open(path_valid_pdf, "rb")
          ]
          @image_id = nil
          images.each do |image|
            content_type = Marcel::MimeType.for(image)

            headers = cookie_header.merge(
              "Content-Type" => content_type,
              "X-Filename" => File.basename(image.path),
              "Content-Length" => File.size(image.path).to_s
            )

            puts "headers #{headers}"

            resp = json_client_post(
              "/inventory/models/#{model_id}/attachments",
              body: image,
              headers: headers,
              is_binary: true
            )

            puts "Image response: #{resp.body}"

            # binding.pry
            expect(resp.status).to eq(400)
            expect(resp.body["error"]).to eq("Failed to upload attachment")
            # expect(resp.body["details"]).to eq("Invalid file type")
            # @image_id = resp.body["image"]["id"]
          end
        end

        it "upload invalid file-size" do
          # create image
          images = [
            # File.open(path_invalid_png, "rb"),
            # File.open(path_invalid_jpg, "rb"),
            # File.open(path_invalid_jpeg, "rb"),
            File.open(path_invalid_pdf, "rb")
          ]
          @image_id = nil
          images.each do |image|
            content_type = Marcel::MimeType.for(image)

            headers = cookie_header.merge(
              # "Content-Type" => "image/png",
              "Content-Type" => content_type,

              "X-Filename" => File.basename(image.path),
              "Content-Length" => File.size(image.path).to_s
            )

            puts "headers #{headers}"

            resp = json_client_post(
              "/inventory/models/#{model_id}/attachments",
              body: image,
              headers: headers,
              is_binary: true
            )

            puts "Image response: #{resp.body}"

            # binding.pry
            expect(resp.status).to eq(400)
            expect(resp.body["error"]).to eq("Failed to upload attachment")
            expect(resp.body["details"]).to eq("File size exceeds limit")
            # @image_id = resp.body["image"]["id"]
          end
        end
      end
    end
  end
end
