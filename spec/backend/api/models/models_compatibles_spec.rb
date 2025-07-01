require "spec_helper"
require "pry"
require_relative "../_shared"

describe "Inventory API Endpoints - Compatible Models" do
  let(:client) { session_auth_plain_faraday_json_client(cookies: @user_cookies) }

  before :each do
    @user, @user_cookies, @user_cookies_str, @cookie_token = create_and_login(:user)
  end

  let(:pool_id) { Faker::Internet.uuid }

  context "when fetching specific compatible models" do
    let(:compatible1) { FactoryBot.create(:leihs_model, id: SecureRandom.uuid) }
    let(:compatible2) { FactoryBot.create(:leihs_model, id: SecureRandom.uuid) }

    let(:model) {
      model = FactoryBot.create(:leihs_model, manufacturer: Faker::Company.name, type: "Software")
      model.add_compatible_model(compatible1)
      model.add_compatible_model(compatible2)
      model
    }

    it "returns status 200 and the compatible models for a given model" do
      resp = client.get "/inventory/#{pool_id}/models/compatibles/#{model.id}"
      expect(resp.status).to eq(200)
      expect(resp.body.count).to eq(2)
    end

    it "returns status 200 and no compatible models for a given compatible model" do
      resp = client.get "/inventory/#{pool_id}/models/compatibles/#{compatible1.id}"
      expect(resp.status).to eq(200)
      expect(resp.body.count).to eq(0)
    end

    it "returns status 200 and no compatible models for another given compatible model" do
      resp = client.get "/inventory/#{pool_id}/models/compatibles/#{compatible2.id}"
      expect(resp.status).to eq(200)
      expect(resp.body.count).to eq(0)
    end
  end
end
