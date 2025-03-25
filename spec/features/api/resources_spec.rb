require "spec_helper"

feature "Call swagger-endpoints" do
  context "revoking the token ", driver: :selenium_headless do
    {
      "/inventory/assets/locales/de/translation.json" => 200,
      "/inventory/assets/locales/en/translation.json" => 200,

      "/inventory/assets/css/additional.css" => 200,

      "/inventory/assets/zhdk-logo.svg" => 200,

      "/inventory/assets/js/main.js" => 200,
      "/inventory/assets/js/libs.js" => 200,
      "/inventory/assets/css/style.css" => 200
    }.each do |url, code|
      it "accessing #{url}    results in expected status-code" do
        response = plain_faraday_client.get(url)
        expect(response.status).to eq(code)
      end
    end

    {
      "/inventory/assets/locales/nd/translation.json" => 404,
      "/assets/locales/nd/translation.json" => 404,

      "/assets/css/additional.css" => 404,
      "/inventory/assets/css/nd.css" => 404,

      "/inventory/nd.svg" => 404,

      "/assets/js/main.js" => 404,
      "/assets/js/libs.js" => 404,
      "/assets/css/style.css" => 404
    }.each do |url, code|
      it "accessing #{url}    fails as expected" do
        response = plain_faraday_client.get(url)
        expect(response.status).to eq(code)
      end
    end
  end
end
