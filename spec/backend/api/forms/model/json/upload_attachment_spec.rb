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

      before do
        # Ensure all fixture files exist
        [path_valid_png, path_valid_jpg, path_valid_jpeg, path_valid_pdf,
          path_invalid_png, path_invalid_jpg, path_invalid_jpeg, path_invalid_pdf].each do |path|
          raise "File not found: #{path}" unless File.exist?(path)
        end

        # Fetch and verify form data
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
        it "retrieves manufacturers list" do
          expect(@form_manufacturers).not_to be_nil
          expect(@form_manufacturers.count).to eq(2)
        end

        it "retrieves entitlement groups list" do
          expect(@form_entitlement_groups).not_to be_nil
          expect(@form_entitlement_groups.count).to eq(2)
        end

        it "retrieves compatible models list" do
          expect(@form_models_compatibles).not_to be_nil
          expect(@form_models_compatibles.count).to eq(3)
        end

        it "retrieves model groups list" do
          expect(@form_model_groups).not_to be_nil
          expect(@form_model_groups.count).to eq(2)
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
            file = File.open(file_path, "rb")
            content_type = Marcel::MimeType.for(file)

            headers = cookie_header.merge(
              "Content-Type" => content_type,
              "X-Filename" => File.basename(file.path),
              "Content-Length" => File.size(file.path).to_s
            )

            response = json_client_post(
              "/inventory/models/#{model_id}/attachments",
              body: file,
              headers: headers,
              is_binary: true
            )

            expect(response.status).to eq(200)
            file.close
          end
        end

        it "rejects invalid attachment file types" do
          [path_valid_png, path_valid_jpg, path_valid_jpeg].each do |file_path|
            file = File.open(file_path, "rb")
            content_type = Marcel::MimeType.for(file)

            headers = cookie_header.merge(
              "Content-Type" => content_type,
              "X-Filename" => File.basename(file.path),
              "Content-Length" => File.size(file.path).to_s
            )

            response = json_client_post(
              "/inventory/models/#{model_id}/attachments",
              body: file,
              headers: headers,
              is_binary: true
            )

            expect(response.status).to eq(400)
            expect(response.body["error"]).to eq("Failed to upload attachment")
            file.close
          end
        end

        if ENV["RAILS_ENV"] == "test"
          it "rejects attachments exceeding maximum size" do
            [path_invalid_pdf].each do |file_path|
              file = File.open(file_path, "rb")
              content_type = Marcel::MimeType.for(file)

              headers = cookie_header.merge(
                "Content-Type" => content_type,
                "X-Filename" => File.basename(file.path),
                "Content-Length" => File.size(file.path).to_s
              )

              response = json_client_post(
                "/inventory/models/#{model_id}/attachments",
                body: file,
                headers: headers,
                is_binary: true
              )

              expect(response.status).to eq(400)
              expect(response.body["error"]).to eq("Failed to upload attachment")
              expect(response.body["details"]).to eq("File size exceeds limit")
              file.close
            end
          end
        end
      end
    end
  end
end
