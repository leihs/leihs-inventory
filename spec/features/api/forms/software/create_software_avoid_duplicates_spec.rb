require "spec_helper"
require "pry"
require_relative "../../_shared"
require_relative "../_common"
require "faker"

feature "Inventory Software" do
  ["inventory_manager", "lending_manager"].each do |role|
    context "when interacting with inventory software with role=#{role}", driver: :selenium_headless do
      include_context :setup_models_api_model, role
      include_context :generate_session_header

      let(:pool_id) { @inventory_pool.id }
      let(:cookie_header) { @cookie_header }
      let(:client) { plain_faraday_json_client(cookie_header) }

      let(:path_test_pdf) { File.expand_path("spec/files/test.pdf", Dir.pwd) }
      let(:path_test2_pdf) { File.expand_path("spec/files/test2.pdf", Dir.pwd) }
      let(:path_test_txt) { File.expand_path("spec/files/text-file.txt", Dir.pwd) }

      before do
        [path_test_pdf, path_test_txt].each do |path|
          raise "File not found: #{path}" unless File.exist?(path)
        end

        # Fetch shared data and set global instance variables
        result = client.get "/inventory/manufacturers?type=Software"
        @form_manufacturers = result.body
        raise "Failed to fetch manufacturers" unless result.status == 200
      end

      context "create software (avoid duplicate name)" do
        it "blocks creation of software when already exists (product_name)" do
          # create software request
          product = Faker::Commerce.product_name
          form_data = {
            "product" => product
          }

          result = http_multipart_client(
            "/inventory/#{pool_id}/software",
            form_data,
            headers: cookie_header
          )
          expect(result.status).to eq(200)

          # fetch created software
          model_id = result.body["data"]["id"]
          result = client.get "/inventory/#{pool_id}/software/#{model_id}"

          expect(result.body[0]["attachments"].count).to eq(0)
          expect(result.status).to eq(200)

          # send same create-software-request
          form_data = {
            "product" => product
          }

          result = http_multipart_client(
            "/inventory/#{pool_id}/software",
            form_data,
            headers: cookie_header
          )
          expect(result.status).to eq(409)
          expect(result.body["message"]).to eq("Model already exists")

          # create software with different version request
          form_data = {
            "product" => product,
            "version" => "2.0"
          }

          result = http_multipart_client(
            "/inventory/#{pool_id}/software",
            form_data,
            headers: cookie_header
          )
          expect(result.status).to eq(200)
        end

        it "blocks creation of software when already exists (product_name & version)" do
          # create software request
          product = Faker::Commerce.product_name
          form_data = {
            "product" => product,
            "version" => "1.0"
          }

          result = http_multipart_client(
            "/inventory/#{pool_id}/software",
            form_data,
            headers: cookie_header
          )
          expect(result.status).to eq(200)

          # fetch created software
          model_id = result.body["data"]["id"]
          result = client.get "/inventory/#{pool_id}/software/#{model_id}"

          expect(result.body[0]["attachments"].count).to eq(0)
          expect(result.status).to eq(200)

          # send same create-software-request
          form_data = {
            "product" => product,
            "version" => "1.0"
          }

          result = http_multipart_client(
            "/inventory/#{pool_id}/software",
            form_data,
            headers: cookie_header
          )
          expect(result.status).to eq(409)
          expect(result.body["message"]).to eq("Model already exists")

          # create software with different version request
          form_data = {
            "product" => product,
            "version" => "2.0"
          }

          result = http_multipart_client(
            "/inventory/#{pool_id}/software",
            form_data,
            headers: cookie_header
          )
          expect(result.status).to eq(200)
        end
      end
    end
  end
end
