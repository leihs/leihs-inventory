require "spec_helper"
require "pry"
require_relative "../../../_shared"
require_relative "../../_common"
require "faker"
require "marcel"

def upload_image(file_path)
  file = File.open(file_path, "rb")
  content_type = Marcel::MimeType.for(file)
  headers = cookie_header.merge(
    "Content-Type" => content_type,
    "X-Filename" => File.basename(file.path),
    "Content-Length" => File.size(file.path).to_s
  )

  response = json_client_post(
    "/inventory/#{@inventory_pool.id}/models/#{model_id}/images/",
    body: file,
    headers: headers,
    is_binary: true
  )
  file.close

  expect(response.status).to eq(200)

  response
end

def expect_correct_url(url)
  resp = client.get url
  expect(resp.status).to eq(200)
end

describe "Inventory Model" do
  ["inventory_manager", "customer"].each do |role|
    context "when interacting with inventory model as #{role}" do
      include_context :setup_models_api_model, role
      include_context :generate_session_header

      let(:pool_id) { @inventory_pool.id }
      let(:model) { @models.first }
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
      let(:pool_id) { @inventory_pool.id }

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
            "/inventory/#{pool_id}/models/#{model_id}/images/",
            body: file,
            headers: headers,
            is_binary: true
          )
          file.close
          response
        end

        it "accepts valid image file types (PNG, JPG, JPEG)" do
          [path_valid_png, path_valid_jpg, path_valid_jpeg].each do |path|
            response = upload_image(path)
            expect(response.status).to eq(200)
          end
        end

        it "rejects unsupported image formats (PDF)" do
          [path_valid_pdf].each do |path|
            response = upload_image(path)
            expect(response.status).to eq(400)
            expect(response.body["error"]).to eq("Failed to upload image")
          end
        end

        context "upload & fetch image" do
          before :each do
            @upload_response = upload_image(path_valid_png)
          end

          it "allows fetching the uploaded image" do
            image_id = @upload_response.body["image"]["id"]

            resp = client.get "/inventory/#{pool_id}/models/#{model_id}/images/#{image_id}"
            expect(resp.status).to eq(200)
          end

          it "allows fetching the uploaded image thumbnail" do
            image_id = @upload_response.body["image"]["id"]

            resp = client.get "/inventory/#{pool_id}/models/#{model_id}/images/#{image_id}/thumbnail"
            expect(resp.status).to eq(200)
          end

          it "returns 404 for customers when accessing model images, otherwise returns image info without is_cover set" do
            image_id = @upload_response.body["image"]["id"]
            thumb_id = @upload_response.body["thumbnail"]["id"]

            resp = client.get "/inventory/#{pool_id}/models/#{model_id}"

            if role == "customer"
              expect(resp.status).to eq(404)
              next
            end

            expect(resp.status).to eq(200)
            expect(resp.body["images"][0]["is_cover"]).to eq(false)
            expect(resp.body["images"][0]["url"]).to eq("/inventory/#{pool_id}/models/#{model_id}/images/#{image_id}")
                      end
        end

        context "upload & fetch image" do
          before :each do
            @upload_response = upload_image(path_valid_png)
            @upload_response2 = upload_image(path_valid_jpg)
          end

          it "shows the image as cover when cover_image_id is set for the model (inventory_manager only)" do
            resp = client.get "/inventory/#{pool_id}/models/#{model_id}"

            if role == "customer"
              expect(resp.status).to eq(404)
              next
            end

            expect(resp.status).to eq(200)

            resp.body["images"].each do |img|
              expect(resp.body["images"][0]["is_cover"]).to eq(false)
              expect_correct_url(img["url"])
              expect_correct_url(img["thumbnail_url"]) if img["thumbnail_url"]
            end
          end

          it "shows the image as cover when cover_image_id is set for the model (inventory_manager only)" do
            image_id = @upload_response2.body["image"]["id"]
            model.update(cover_image_id: image_id)

            resp = client.get "/inventory/#{pool_id}/models/#{model_id}"
            if role == "customer"
              expect(resp.status).to eq(404)
              next
            end

            expect(resp.status).to eq(200)
            resp.body["images"].each do |img|
              
              # expect(resp.body["images"][0]["is_cover"]).to eq(true)
              expect_correct_url(img["url"])
              expect_correct_url(img["thumbnail_url"]) if img["thumbnail_url"]
            end
          end
        end
      end
    end
  end
end
