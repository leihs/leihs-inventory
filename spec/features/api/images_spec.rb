require "spec_helper"
require "pry"
require "#{File.dirname(__FILE__)}/_shared"

feature "Inventory API Endpoints - Image Handling" do
  context "when fetching images for a specific inventory pool", driver: :selenium_headless do
    include_context :setup_access_rights

    let(:url) { "/inventory/images/" }
    let(:client) { plain_faraday_json_client }
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

    # FIXME
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

    # FIXME
    context "Fetch image data as an image" do
      it "returns error when fetching image by ID as a raw image format" do
        resp = plain_faraday_image_client.get "#{url}#{image_id}"
        expect(resp.status).to eq(400)
      end

      it "returns error when fetching image thumbnail as a raw image format" do
        resp = plain_faraday_image_client.get "#{url}#{image_id}/thumbnail"
        expect(resp.status).to eq(400)
      end
    end
  end
end
