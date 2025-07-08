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
  # ['inventory_manager', 'customer'].each do |role|
  ['inventory_manager'].each do |role|
    context "when interacting with inventory model as #{role}" do
      include_context :setup_models_api_model_compatible, role
      include_context :generate_session_header

      let(:pool_id) { @inventory_pool.id }
      let(:model) { @model }
      let(:model_id) { @model.id }
      let(:client) { plain_faraday_json_client(cookie_header) }
      let(:cookie_header) { @cookie_header }
      let(:pool_id) { @inventory_pool.id }

      context "upload attachments" do
        it "fetches compatibles without images" do
          resp = client.get "/inventory/#{pool_id}/models/#{model_id}"

          if role == "customer"
            expect(resp.status).to eq(404)
            next
          end
          expect(resp.status).to eq(200)

          expect(resp.body["compatibles"][0]["cover_image_url"]).to be_nil
          expect(resp.body["compatibles"][0]["cover_image_id"]).to be_nil
        end

        it "fetches compatibles with images without thumb (default image)" do

          @image = FactoryBot.create(:image, :for_leihs_model,
                                     target: @compatible_model,
                                     real_filename: "anon.jpg")

          resp = client.get "/inventory/#{pool_id}/models/#{model_id}"
          if role == "customer"
            expect(resp.status).to eq(404)
            next
          end

          expect(resp.status).to eq(200)

          expect(resp.body["compatibles"][0]["cover_image_url"]).to be_nil
          expect(resp.body["compatibles"][0]["cover_image_id"]).to be_nil
        end

        context "fetches compatibles with images with thumb" do
          before do
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
          end

          it "fetches compatibles with two images (cover_image)" do
            @compatible_model.update(cover_image_id: @image2.id)

            resp = client.get "/inventory/#{pool_id}/models/#{model_id}"
            if role == "customer"
              expect(resp.status).to eq(404)
              next
            end

            expect(resp.status).to eq(200)

            expect(resp.body["compatibles"][0]["cover_image_url"]).to eq("/inventory/#{pool_id}/models/#{@compatible_model.id}/images/#{@image2.id}")
            expect(resp.body["compatibles"][0]["cover_image_id"]).to eq(@image2.id)

            expect_correct_url(resp.body["compatibles"][0]["cover_image_url"])

          end

          it "fetches compatibles with two images (cover_image)" do
            @compatible_model.update(cover_image_id: @image.id)

            resp = client.get "/inventory/#{pool_id}/models/#{model_id}"
            if role == "customer"
              expect(resp.status).to eq(404)
              next
            end

            expect(resp.status).to eq(200)

            expect(resp.body["compatibles"][0]["cover_image_url"]).to eq("/inventory/#{pool_id}/models/#{@compatible_model.id}/images/#{@image.id}")
            expect(resp.body["compatibles"][0]["cover_image_id"]).to eq(@image.id)

            expect_correct_url(resp.body["compatibles"][0]["cover_image_url"])

          end
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
          if role == "customer"
            expect(resp.status).to eq(404)
            next
          end

          expect(resp.status).to eq(200)

          # 
          expect(resp.body["compatibles"][0]["cover_image_thumb"]).to eq("/inventory/#{pool_id}/models/#{@compatible_model.id}/images/#{@thumbnail.id}/thumbnail")
          expect(resp.body["compatibles"][0]["cover_image_id"]).to be_nil

          expect_correct_url(resp.body["compatibles"][0]["cover_image_url"])

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
          if role == "customer"
            expect(resp.status).to eq(404)
            next
          end

          expect(resp.status).to eq(200)

          
          # expect(resp.body["compatibles"][0]["cover_image_url"]).to eq("/inventory/#{pool_id}/models/#{@compatible_model.id}/images/#{@thumbnail.id}")
          expect(resp.body["compatibles"][0]["cover_image_thumb"]).to eq("/inventory/#{pool_id}/models/#{@compatible_model.id}/images/#{@thumbnail.id}/thumbnail")
          expect(resp.body["compatibles"][0]["cover_image_id"]).to be_nil

          expect_correct_url(resp.body["compatibles"][0]["cover_image_url"])
        end

      end
    end
  end
end

