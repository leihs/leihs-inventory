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

def expect_spa_content(resp, status)
  expect(resp.body).to include("<title>Inventory</title>")
  binding.pry if resp.status != status
  expect(resp.status).to eq(status)
end

describe "Inventory Model" do
  ["inventory_manager"].each do |role|
    context "when interacting with inventory model as #{role}" do
      include_context :setup_models_api_model, role
      include_context :generate_session_header

      let(:pool_id) { @inventory_pool.id }
      let(:model) { @models.first }
      let(:model_id) { @models.first.id }
      let(:cookie_header) { @cookie_header }
      let(:client) { plain_faraday_json_client(cookie_header) }
      let(:html_client) {        plain_faraday_html_client(cookie_header.merge({ "Accept" => "text/html" })) }

      let(:path_valid_png) { File.expand_path("spec/files/500-kb.png", Dir.pwd) }
      # let(:path_valid_jpg) { File.expand_path("spec/files/600-kb.jpg", Dir.pwd) }
      # let(:path_valid_jpeg) { File.expand_path("spec/files/600-kb.jpeg", Dir.pwd) }
      # let(:path_valid_pdf) { File.expand_path("spec/files/300-kb.pdf", Dir.pwd) }

      let(:pool_id) { @inventory_pool.id }

      # before do
      #   [path_valid_png, path_valid_jpg, path_valid_jpeg, path_valid_pdf].each do |path|
      #     raise "File not found: #{path}" unless File.exist?(path)
      #   end
      # end

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

        context "upload resource," do
          before :each do
            @upload_response = upload_image(path_valid_png)

            @image_id = @upload_response.body["image"]["id"]
            expect(@image_id).not_to be_nil
          end

          context "fetch existing resource, " do
            it "allows fetching the uploaded image" do
              resp = html_client.get "/inventory/#{pool_id}/models/#{model_id}/images/#{@image_id}"
              expect(resp.status).to eq(200)
            end

            it "allows fetching the uploaded thumbnail" do
              resp = html_client.get "/inventory/#{pool_id}/models/#{model_id}/images/#{@image_id}/thumbnail"
              expect(resp.status).to eq(200)
            end
          end

          context "fetch not existing resource, " do
            let (:non_existing_image_id) { "00000000-0000-0000-0000-000000000000" }

            it "allows fetching the uploaded image" do
              resp = html_client.get "/inventory/#{pool_id}/models/#{model_id}/images/#{non_existing_image_id}"
              expect_spa_content(resp, 404)
            end

            it "allows fetching the uploaded thumbnail" do
              resp = html_client.get "/inventory/#{pool_id}/models/#{model_id}/images/#{non_existing_image_id}/thumbnail"
              expect_spa_content(resp, 404)
            end
          end

          context "fetch by invalid uuid, " do
            let (:invalid_uuid_coercion_error) { "00000000-0000-0000-0000-00000000000s" }

            it "blocks fetching the uploaded image" do
              resp = html_client.get "/inventory/#{pool_id}/models/#{model_id}/images/#{invalid_uuid_coercion_error}"
              expect_spa_content(resp, 404)
            end

            it "blocks fetching the uploaded thumbnail" do
              resp = html_client.get "/inventory/#{pool_id}/models/#{model_id}/images/#{invalid_uuid_coercion_error}/thumbnail"
              expect_spa_content(resp, 404)
            end
          end

          context "fetch SPA-response, " do
            let (:in_or_valid_routes) { ["/inventory", "/inventory/",
            "/inventory/#{pool_id}/list?with_items=true&retired=false&page=1&size=50",
            "/inventory/#{pool_id}/list/",
            "/inventory/#{pool_id}/list/invalid-url",
            "/inventory/invalid-url",
            ] }

            it "doesn't matter if in/valid" do
              in_or_valid_routes.each do |route|

              resp = html_client.get route
              expect_spa_content(resp, 200)
              end
            end
          end
        end
      end
    end
  end
end
