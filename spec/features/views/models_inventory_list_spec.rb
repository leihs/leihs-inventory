require "spec_helper"
require "pry"

feature "Call /inventory/models/inventory-list" do
  context " with accept=text/html" do
    let :http_client do
      plain_faraday_client
    end

    scenario "response is correct" do
      resp = http_client.get "/inventory/models/inventory-list"
      expect(resp.status).to be == 200
    end
  end
end
