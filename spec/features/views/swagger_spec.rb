require "spec_helper"
require "pry"

feature "Call swagger-endpoints" do
  context " with accept=text/html" do
    before :each do
      visit "/inventory/api-docs/"
    end

    scenario "Contains expected elements" do
      expect(page).to have_content "inventory-api"
      expect(page).to have_content "Models"
    end
  end

  context "swagger.json exists" do
    let :http_client do
      plain_faraday_json_client
    end

    scenario "json response is correct" do
      resp = http_client.get "/inventory/api-docs/swagger.json"
      expect(resp.status).to be == 200
      expect(resp.body["swagger"]).to eq "2.0"
    end
  end
end
