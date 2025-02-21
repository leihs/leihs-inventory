require "spec_helper"
require "pry"
require_relative "../../_shared"
require_relative "../_common"
require "faker"

feature "Inventory Software" do
  ["groups_manager", "customer"].each do |role|
    context "when interacting with inventory software with role=#{role}", driver: :selenium_headless do
      include_context :setup_models_api_model, role
      include_context :generate_session_header

      let(:pool_id) { @inventory_pool.id }
      let(:cookie_header) { @cookie_header }
      let(:client) { plain_faraday_json_client(cookie_header) }

      let(:path_test_pdf) { File.expand_path("spec/files/test.pdf", Dir.pwd) }
      let(:path_test2_pdf) { File.expand_path("spec/files/test2.pdf", Dir.pwd) }
      let(:path_test_txt) { File.expand_path("spec/files/text-file.txt", Dir.pwd) }

      let(:fake_software) {
        LeihsModel.where(type: "Software").first
      }

      before do
        [path_test_pdf, path_test_txt].each do |path|
          raise "File not found: #{path}" unless File.exist?(path)
        end

        # Fetch shared data and set global instance variables
        result = client.get "/inventory/manufacturers?type=Software"
        @form_manufacturers = result.body
        raise "Failed to fetch manufacturers" unless result.status == 200
      end

      context "fetch form data" do
        it "ensures form manufacturer data is fetched" do
          expect(@form_manufacturers).not_to be_nil
          expect(@form_manufacturers.count).to eq(1)
        end
      end

      it "creates software with all available attributes" do
        # create software request
        form_data = {
          "product" => Faker::Commerce.product_name,
          "attachments" => [File.open(path_test_pdf, "rb"), File.open(path_test2_pdf, "rb")],
          "version" => "v1.0",
          "manufacturer" => @form_manufacturers.first, # Use fetched manufacturer name
          "technicalDetails" => "Specs go here"
        }

        result = http_multipart_client(
          "/inventory/#{pool_id}/software",
          form_data,
          headers: cookie_header
        )
        expect(result.status).to eq(401)

        # fetch created software
        model_id = fake_software.id
        result = client.get "/inventory/#{pool_id}/software/#{model_id}"
        expect(result.status).to eq(401)

        # update software request
        form_data = {
          "product" => "updated product",
          "attachments" => [],
          "attachments-to-delete" => [],
          "version" => "updated v2.0",
          "manufacturer" => "updated manufacturer",
          "technicalDetails" => "updated techDetail"
        }

        result = http_multipart_client(
          "/inventory/#{pool_id}/software/#{model_id}",
          form_data,
          method: :put,
          headers: cookie_header
        )
        expect(result.status).to eq(401)

        # fetch updated model
        result = client.get "/inventory/#{pool_id}/software/#{model_id}"
        expect(result.status).to eq(401)
      end

      context "create software (min)" do
        it "creates software with all available attributes" do
          # create software request
          form_data = {
            "product" => Faker::Commerce.product_name
          }

          result = http_multipart_client(
            "/inventory/#{pool_id}/software",
            form_data,
            headers: cookie_header
          )
          expect(result.status).to eq(401)

          # fetch created software
          model_id = fake_software.id
          result = client.get "/inventory/#{pool_id}/software/#{model_id}"
          expect(result.status).to eq(401)

          # update software request
          form_data = {
            "product" => "updated product"
          }

          result = http_multipart_client(
            "/inventory/#{pool_id}/software/#{model_id}",
            form_data,
            method: :put,
            headers: cookie_header
          )
          expect(result.status).to eq(401)

          # fetch updated model
          result = client.get "/inventory/#{pool_id}/software/#{model_id}"
          expect(result.status).to eq(401)
        end
      end
    end
  end
end
