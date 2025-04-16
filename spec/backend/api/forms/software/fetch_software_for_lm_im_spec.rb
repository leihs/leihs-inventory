require "spec_helper"
require "pry"
require_relative "../../_shared"
require_relative "../_common"
require "faker"

post_response = {
  "description" => [NilClass, String],
  "is_package" => [TrueClass, FalseClass],
  "maintenance_period" => [NilClass, Integer],
  "type" => String,
  "rental_price" => [NilClass, Numeric],
  "cover_image_id" => [NilClass, String],
  "hand_over_note" => [NilClass, String],
  "updated_at" => String,
  "internal_description" => [NilClass, String],
  "product" => String,
  "info_url" => [NilClass, String],
  "id" => String,
  "manufacturer" => [NilClass, String],
  "version" => [NilClass, String],
  "created_at" => String,
  "technical_detail" => [NilClass, String]
}

get_response = {
  "description" => [NilClass, String],
  "is_package" => [TrueClass, FalseClass],
  "attachments" => Array,
  "type" => String,
  "hand_over_note" => [NilClass, String],
  "internal_description" => [NilClass, String],
  "product" => String,
  "id" => String,
  "manufacturer" => [NilClass, String],
  "version" => [NilClass, String],
  "technical_detail" => [NilClass, String]
}

put_response = {
  "description" => [NilClass, String],
  "is_package" => [TrueClass, FalseClass],
  "maintenance_period" => [NilClass, Integer],
  "type" => String,
  "rental_price" => [NilClass, Numeric],
  "cover_image_id" => [NilClass, String],
  "hand_over_note" => [NilClass, String],
  "updated_at" => String,
  "internal_description" => [NilClass, String],
  "product" => String,
  "info_url" => [NilClass, String],
  "id" => String,
  "manufacturer" => [NilClass, String],
  "version" => [NilClass, String],
  "created_at" => String,
  "technical_detail" => [NilClass, String]
}

describe "Inventory Software" do
  ["inventory_manager", "lending_manager"].each do |role|
    context "when interacting with inventory software with role=#{role}" do
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
        resp = client.get "/inventory/manufacturers?type=Software"
        @form_manufacturers = resp.body
        raise "Failed to fetch manufacturers" unless resp.status == 200
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
          "technical_detail" => "Specs go here"
        }

        resp = http_multipart_client(
          "/inventory/#{pool_id}/software",
          form_data,
          headers: cookie_header
        )
        expect(resp.status).to eq(200)
        validate_map_structure(resp.body["data"], post_response)
        expect(compare_values(resp.body["data"], form_data, ["product", "version", "technical_detail"])).to eq(true)

        # fetch created software
        model_id = resp.body["data"]["id"]
        resp = client.get "/inventory/#{pool_id}/software/#{model_id}"

        validate_map_structure(resp.body.first, get_response)
        attachments = resp.body[0]["attachments"]
        expect(attachments.count).to eq(2)
        expect(resp.status).to eq(200)
        expect(Attachment.where(model_id: model_id).count).to eq(2)

        # update software request
        form_data = {
          "product" => "updated product",
          "attachments" => [],
          "attachments_to_delete" => [attachments[0]["id"]].to_json,
          "version" => "updated v2.0",
          "manufacturer" => "updated manufacturer",
          "technical_detail" => "updated techDetail"
        }

        resp = http_multipart_client(
          "/inventory/#{pool_id}/software/#{model_id}",
          form_data,
          method: :put,
          headers: cookie_header
        )
        validate_map_structure(resp.body.first, put_response)
        expect(resp.status).to eq(200)
        expect(resp.body[0]["id"]).to eq(model_id)
        expect(compare_values(resp.body[0], form_data, ["product", "version", "manufacturer", "technical_detail"])).to eq(true)

        # fetch updated model
        resp = client.get "/inventory/#{pool_id}/software/#{model_id}"
        validate_map_structure(resp.body.first, get_response)
        expect(resp.body[0]["attachments"].count).to eq(1)
        expect(resp.status).to eq(200)
      end

      context "create software (min)" do
        it "creates software with all available attributes" do
          # create software request
          form_data = {
            "product" => Faker::Commerce.product_name
          }

          resp = http_multipart_client(
            "/inventory/#{pool_id}/software",
            form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(200)
          validate_map_structure(resp.body["data"], post_response)

          # fetch created software
          model_id = resp.body["data"]["id"]
          resp = client.get "/inventory/#{pool_id}/software/#{model_id}"

          validate_map_structure(resp.body.first, get_response)
          expect(resp.body[0]["attachments"].count).to eq(0)
          expect(resp.status).to eq(200)

          # update software request
          form_data = {
            "product" => "updated product"
          }

          resp = http_multipart_client(
            "/inventory/#{pool_id}/software/#{model_id}",
            form_data,
            method: :put,
            headers: cookie_header
          )

          validate_map_structure(resp.body.first, put_response)
          expect(resp.status).to eq(200)
          expect(resp.body[0]["id"]).to eq(model_id)

          # fetch updated model
          resp = client.get "/inventory/#{pool_id}/software/#{model_id}"

          validate_map_structure(resp.body.first, get_response)
          expect(resp.body[0]["product"]).to eq("updated product")
          expect(resp.body[0]["attachments"].count).to eq(0)
          expect(resp.status).to eq(200)
        end
      end
    end
  end
end
