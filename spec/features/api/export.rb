require "spec_helper"
require "pry"
require "#{File.dirname(__FILE__)}/_shared"

feature "Inventory API Endpoints - Image Handling" do
  context "when fetching images for a specific inventory pool", driver: :selenium_headless do
    let :url do
      "/inventory/export"
    end

    context "Fetch image data as an image" do
      it "returns error when fetching image by ID as a raw image format" do
        resp = plain_faraday_resource_client({accept: ACCEPT_CSV}).get "#{url}/csv"
        expect(resp.status).to eq(200)
      end

      it "returns error when fetching image thumbnail as a raw image format" do
        resp = plain_faraday_resource_client({accept: ACCEPT_XLSX}).get "#{url}/excel"
        expect(resp.status).to eq(200)
      end
    end

    context "Fetch image data as an image" do
      it "returns error when fetching image by ID as a raw image format" do
        resp = plain_faraday_resource_client({accept: ACCEPT_HTML}).get "#{url}/csv"
        expect(resp.status).to eq(302)

        expect(resp.headers["location"]).to eq("/sign-in?return-to=%2Finventory")
      end

      it "returns error when fetching image thumbnail as a raw image format" do
        resp = plain_faraday_resource_client({accept: ACCEPT_HTML}).get "#{url}/excel"
        expect(resp.status).to eq(302)
        expect(resp.headers["location"]).to eq("/sign-in?return-to=%2Finventory")
      end
    end
  end
end
