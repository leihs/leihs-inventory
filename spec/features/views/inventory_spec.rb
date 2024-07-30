require "spec_helper"
require "pry"

feature "Call /inventory" do
  context " with accept=text/html" do
    before :each do
      visit "/inventory"
    end

    scenario "Contains expected elements" do
      expect(page).to have_content "Leihs Inventory with OpenApi"
      expect(page).to have_content "home-page"
    end

    scenario "Contains expected elements in Model" do
      click_on "Models"
      expect(page).to have_content "Add Model"
      expect(page).to have_content "Refresh"
    end

    scenario "Contains expected elements in Home" do
      click_on "Home"
      expect(page).to have_content "home-page"
    end

    scenario "Contains expected elements in Debug" do
      click_on "Debug"
      expect(page).to have_content "Some routing tests"
      expect(page).to have_content "JS integration tests"
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
      resp = http_client.get "/inventory"
      expect(resp.status).to be == 200
      expect(resp.body).to include('<script src="/inventory/js/main.js"></script>')
    end
  end
end
