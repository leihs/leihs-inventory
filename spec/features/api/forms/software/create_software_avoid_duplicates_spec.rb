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

      before do
        # Fetch shared data and set global instance variables
        resp = client.get "/inventory/manufacturers?type=Software"
        @form_manufacturers = resp.body
        raise "Failed to fetch manufacturers" unless resp.status == 200
      end

      context "create software (avoid duplicate name)" do
        it "blocks creation of software when already exists (product_name)" do
          # create software request
          product = Faker::Commerce.product_name
          form_data = {
            "product" => product
          }

          resp = http_multipart_client(
            "/inventory/#{pool_id}/software",
            form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(200)

          # fetch created software
          model_id = resp.body["data"]["id"]
          resp = client.get "/inventory/#{pool_id}/software/#{model_id}"

          expect(resp.body[0]["attachments"].count).to eq(0)
          expect(resp.status).to eq(200)

          # send same create-software-request
          form_data = {
            "product" => product
          }

          resp = http_multipart_client(
            "/inventory/#{pool_id}/software",
            form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(409)
          expect(resp.body["message"]).to eq("Model already exists")

          # create software with different version request
          form_data = {
            "product" => product,
            "version" => "2.0"
          }

          resp = http_multipart_client(
            "/inventory/#{pool_id}/software",
            form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(200)
        end

        it "blocks creation of software when already exists (product_name & version)" do
          # create software request
          product = Faker::Commerce.product_name
          form_data = {
            "product" => product,
            "version" => "1.0"
          }

          resp = http_multipart_client(
            "/inventory/#{pool_id}/software",
            form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(200)

          # fetch created software
          model_id = resp.body["data"]["id"]
          resp = client.get "/inventory/#{pool_id}/software/#{model_id}"

          expect(resp.body[0]["attachments"].count).to eq(0)
          expect(resp.status).to eq(200)

          # send same create-software-request
          form_data = {
            "product" => product,
            "version" => "1.0"
          }

          resp = http_multipart_client(
            "/inventory/#{pool_id}/software",
            form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(409)
          expect(resp.body["message"]).to eq("Model already exists")

          # create software with different version request
          form_data = {
            "product" => product,
            "version" => "2.0"
          }

          resp = http_multipart_client(
            "/inventory/#{pool_id}/software",
            form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(200)
        end
      end
    end
  end
end
