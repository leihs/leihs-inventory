require "spec_helper"
require "pry"

describe "Call swagger-endpoints" do
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
