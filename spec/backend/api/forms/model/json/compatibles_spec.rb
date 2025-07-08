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
  ["inventory_manager"].each do |role|
    context "when interacting with inventory model as #{role}" do
      include_context :setup_models_api_model_compatible, role
      include_context :generate_session_header

      let(:pool_id) { @inventory_pool.id }
      let(:model) { @model }
      let(:model_id) { @model.id }
      let(:client) { plain_faraday_json_client(cookie_header) }
      let(:cookie_header) { @cookie_header }

      # let(:form_categories) { @form_categories }
      # let(:form_compatible_models) { @compatible_model }

      # let(:path_valid_png) { File.expand_path("spec/files/500-kb.png", Dir.pwd) }
      # let(:path_valid_jpg) { File.expand_path("spec/files/600-kb.jpg", Dir.pwd) }
      # let(:path_valid_jpeg) { File.expand_path("spec/files/600-kb.jpeg", Dir.pwd) }
      # let(:path_valid_pdf) { File.expand_path("spec/files/300-kb.pdf", Dir.pwd) }
      #
      # let(:path_invalid_png) { File.expand_path("spec/files/2-mb.png", Dir.pwd) }
      # let(:path_invalid_jpg) { File.expand_path("spec/files/2-mb.jpg", Dir.pwd) }
      # let(:path_invalid_jpeg) { File.expand_path("spec/files/2-mb.jpeg", Dir.pwd) }
      # let(:path_invalid_pdf) { File.expand_path("spec/files/2-mb.pdf", Dir.pwd) }

      let(:pool_id) { @inventory_pool.id }

      # before do
      #   # Ensure all fixture files exist
      #   [path_valid_png, path_valid_jpg, path_valid_jpeg, path_valid_pdf,
      #    path_invalid_png, path_invalid_jpg, path_invalid_jpeg, path_invalid_pdf].each do |path|
      #     raise "File not found: #{path}" unless File.exist?(path)
      #   end
      # end

      context "upload attachments" do
        it "fetches compatibles without images" do
          resp = client.get "/inventory/#{pool_id}/models/#{model_id}"
          expect(resp.status).to eq(200)

          expect(resp.body["compatibles"][0]["cover_image_url"]).to be_nil
          expect(resp.body["compatibles"][0]["cover_image_id"]).to be_nil
        end

        it "fetches compatibles with images without thumb (default image)" do

          @image = FactoryBot.create(:image, :for_leihs_model,
                                     target: @compatible_model,
                                     real_filename: "anon.jpg")

          resp = client.get "/inventory/#{pool_id}/models/#{model_id}"
          expect(resp.status).to eq(200)

          expect(resp.body["compatibles"][0]["cover_image_url"]).to be_nil
          expect(resp.body["compatibles"][0]["cover_image_id"]).to be_nil
        end

        it "fetches compatibles with two images (cover_image)" do
          @thumbnail = FactoryBot.create(:image, :for_leihs_model,
                                         thumbnail: true,
                                         target: @compatible_model,
                                         filename: "anon_thumb.jpg",
                                         real_filename: "anon.jpg")

          @image = FactoryBot.create(:image, :for_leihs_model,
                                     target: @compatible_model,
                                     thumbnails: [@thumbnail],
                                     real_filename: "anon.jpg")

          @thumbnail2 = FactoryBot.create(:image, :for_leihs_model,
                                          thumbnail: true,
                                          target: @compatible_model,
                                          filename: "anon_thumb.jpg",
                                          real_filename: "anon.jpg")

          @image2 = FactoryBot.create(:image, :for_leihs_model,
                                      target: @compatible_model,
                                      thumbnails: [@thumbnail2],
                                      real_filename: "anon.jpg")

          @compatible_model.update(cover_image_id: @thumbnail2.id)

          resp = client.get "/inventory/#{pool_id}/models/#{model_id}"
          expect(resp.status).to eq(200)

          expect(resp.body["compatibles"][0]["cover_image_url"]).to eq("/inventory/#{pool_id}/models/#{@compatible_model.id}/images/#{@thumbnail2.id}/thumbnail")
          expect(resp.body["compatibles"][0]["cover_image_id"]).to eq(@thumbnail2.id)
        end

        it "fetches compatibles with single image (default image)" do
          @thumbnail = FactoryBot.create(:image, :for_leihs_model,
                                         thumbnail: true,
                                         target: @compatible_model,
                                         filename: "anon_thumb.jpg",
                                         real_filename: "anon.jpg")

          @image = FactoryBot.create(:image, :for_leihs_model,
                                     target: @compatible_model,
                                     # thumbnails: [@thumbnail, @thumbnail2],
                                     thumbnails: [@thumbnail],
                                     real_filename: "anon.jpg")

          resp = client.get "/inventory/#{pool_id}/models/#{model_id}"
          expect(resp.status).to eq(200)

          expect(resp.body["compatibles"][0]["cover_image_url"]).to eq("/inventory/#{pool_id}/models/#{@compatible_model.id}/images/#{@thumbnail.id}/thumbnail")
          expect(resp.body["compatibles"][0]["cover_image_id"]).to be_nil
        end

        it "fetches compatibles and verifies order (default image)" do
          # add image with two thumbnails
          @thumbnail = FactoryBot.create(:image, :for_leihs_model,
                                         thumbnail: true,
                                         target: @compatible_model,
                                         filename: "anon_thumb.jpg",
                                         real_filename: "anon.jpg")

          @thumbnail2 = FactoryBot.create(:image, :for_leihs_model,
                                          thumbnail: true,
                                          target: @compatible_model,
                                          filename: "sap_thumb.jpg",
                                          real_filename: "sap.png")

          @image = FactoryBot.create(:image, :for_leihs_model,
                                     target: @compatible_model,
                                     thumbnails: [@thumbnail, @thumbnail2],
                                     real_filename: "anon.jpg")

          # add image with one thumbnail only
          @thumbnail3 = FactoryBot.create(:image, :for_leihs_model,
                                          thumbnail: true,
                                          target: @compatible_model,
                                          filename: "anon_thumb.jpg",
                                          real_filename: "anon.jpg")

          @image2 = FactoryBot.create(:image, :for_leihs_model,
                                      target: @compatible_model,
                                      thumbnails: [@thumbnail3],
                                      real_filename: "anon.jpg")

          resp = client.get "/inventory/#{pool_id}/models/#{model_id}"
          expect(resp.status).to eq(200)

          expect(resp.body["compatibles"][0]["cover_image_url"]).to eq("/inventory/#{pool_id}/models/#{@compatible_model.id}/images/#{@thumbnail.id}/thumbnail")
          expect(resp.body["compatibles"][0]["cover_image_id"]).to be_nil
        end

      end
    end
  end
end

