require "spec_helper"
require "pry"
require "#{File.dirname(__FILE__)}/_shared"

describe "Inventory API Endpoints - Image Handling" do
  context "when accessing image export endpoints for an inventory pool" do
    let(:url) { "/inventory/export" }

    context "Exporting images in CSV and Excel formats" do
      it "returns a 200 status for CSV format request" do
        resp = plain_faraday_resource_client({accept: ACCEPT_CSV}).get "#{url}/csv"
        expect(resp.status).to eq(200)
      end

      it "returns a 200 status for Excel format request" do
        resp = plain_faraday_resource_client({accept: ACCEPT_XLSX}).get "#{url}/excel"
        expect(resp.status).to eq(200)
      end
    end

    context "Redirect to sign-in for HTML requests" do
      it "redirects to sign-in for CSV format with HTML accept header" do
        resp = plain_faraday_resource_client({accept: ACCEPT_HTML}).get "#{url}/csv"
        expect(resp.status).to eq(302)
        expect(resp.headers["location"]).to eq("/sign-in?return-to=%2Finventory")
      end

      it "redirects to sign-in for Excel format with HTML accept header" do
        resp = plain_faraday_resource_client({accept: ACCEPT_HTML}).get "#{url}/excel"
        expect(resp.status).to eq(302)
        expect(resp.headers["location"]).to eq("/sign-in?return-to=%2Finventory")
      end
    end
  end
end
