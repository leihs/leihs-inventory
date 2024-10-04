require "spec_helper"

feature "Call swagger-endpoints" do
  context "revoking the token ", driver: :selenium_headless do
    {
      "/inventory/locales/de/translation.json" => 200,
      "/inventory/locales/fr/translation.json" => 200,
      "/inventory/locales/en/translation.json" => 200,
      "/inventory/locales/es/translation.json" => 200,
      "/inventory/locales/nd/translation.json" => 404,
      "/inventory/css/additional.css" => 200,
      "/inventory/css/nd.css" => 404,

      "/inventory/zhdk-logo.svg" => 200,
      "/inventory/nd.svg" => 404,
      "/inventory/static/zhdk-logo.svg" => 404
    }.each do |url, code|
      it "accessing #{url}    results in expected status-code" do
        response = plain_faraday_client.get(url)
        expect(response.status).to eq(code)
      end
    end
  end
end
