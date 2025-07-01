require "spec_helper"
require "pry"
require "#{File.dirname(__FILE__)}/_shared"

describe "Inventory API Endpoints - Image Handling" do
  context "when fetching images for a specific inventory pool" do
    include_context :setup_access_rights

    before :each do
      @user_cookies, @user_cookies_str, @cookie_token = create_and_login_by(@user)
    end

    let(:any_uuid) { Faker::Internet.uuid }

    let(:url) { "/inventory/#{any_uuid}/images/" }
    let(:client) { session_auth_plain_faraday_json_csrf_client(cookies: @user_cookies) }
    let(:resp) { client.get url }
    let(:image_id) { resp.body["data"][0]["id"] }

    context "GET /inventory/images" do
      it "retrieves all images and returns status 200" do
        expect(resp.status).to eq(200)
        expect(resp.body["data"].count).to eq(2)
      end

      it "retrieves paginated image results and returns status 200" do
        resp = client.get "#{url}?page=3&size=1"
        expect(resp.status).to eq(200)
        expect(resp.body["data"].count).to eq(2)
      end
    end

    context "Fetch image data as JSON" do
      it "retrieves image details by ID and returns status 200" do
        resp = client.get "#{url}#{image_id}"
        expect(resp.status).to eq(200)
      end

      it "retrieves image thumbnail by ID and returns status 200" do
        resp = client.get "#{url}#{image_id}/thumbnail"
        expect(resp.status).to eq(200)
      end
    end

    context "Fetch image data as an image" do
      it "returns error when fetching image by ID as a raw image format" do
        resp = plain_faraday_resource_client.get "#{url}#{image_id}"
        expect(resp.status).to eq(200)
      end

      # FIXME: thumb not created by include_context :setup_access_rights
      it "returns error when fetching image thumbnail as a raw image format" do
        resp = plain_faraday_resource_client.get "#{url}#{image_id}/thumbnail"
        expect(resp.status).to eq(400)
      end
    end
  end
end
