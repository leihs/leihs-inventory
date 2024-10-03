require "spec_helper"
require "pry"

feature "Call /inventory" do
  context "with accept=application/json" do
    let :http_client do
      plain_faraday_json_client
    end

    let :prepare_http_client do
      http_client.headers["Accept"] = "application/json"
    end

    before :each do
      prepare_http_client
    end
  end
end
