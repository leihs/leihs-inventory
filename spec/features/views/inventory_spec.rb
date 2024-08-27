require "spec_helper"
require "pry"

feature "Call /inventory" do
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
      expect(resp.body).to include('<link rel="icon" type="image/svg+xml" href="/vite.svg" />')
    end
  end
end
