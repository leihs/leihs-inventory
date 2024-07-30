require "spec_helper"
require "pry"

feature "Call /" do
  context " with accept=text/html" do
    before :each do
      visit "/"
    end

    scenario "Contains expected elements" do
      expect(page).to have_content "Overview"
      expect(page).to have_content "Resource Links"
    end
  end

  context "with accept=application/json" do
    let :http_client do
      plain_faraday_client
    end

    let :prepare_http_client do
      http_client.headers["Accept"] = "application/json"
    end

    before :each do
      prepare_http_client
    end

    scenario "json response is correct" do
      resp = http_client.get "/"
      expect(resp.status).to be == 200
      expect(resp.body["message"]).to be == "Welcome to Inventory-API"
    end
  end
end
