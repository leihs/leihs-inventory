require "spec_helper"
require "pry"
require_relative "../_shared"

feature "Inventory API Endpoints - Compatible Models" do
  let(:client) { session_auth_plain_faraday_json_client(cookies: @user_cookies) }

  before :each do
    @user, @user_cookies, @user_cookies_str, @cookie_token = create_and_login(:user)
  end

  context "when fetching compatible models for an inventory pool", driver: :selenium_headless do
    include_context :setup_access_rights

    context "GET /inventory/models-compatibles" do
      it "returns status 200 and no compatible models when none exist" do
        resp = client.get "/inventory/models-compatibles?page=1&size=100"
        expect(resp.status).to eq(200)
        expect(resp.body["pagination"]["total_rows"]).to eq(0)
      end

      it "returns status 200 and an empty list when no compatible models exist" do
        resp = client.get "/inventory/models-compatibles"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(0)
      end

      it "returns status 200 and paginated results with no compatible models" do
        resp = client.get "/inventory/models-compatibles?page=1&size=1"
        expect(resp.status).to eq(200)
        expect(resp.body["pagination"]["total_rows"]).to eq(0)
      end
    end
  end

  context "when fetching specific compatible models" do
    let(:compatible1) { FactoryBot.create(:leihs_model, id: SecureRandom.uuid) }
    let(:compatible2) { FactoryBot.create(:leihs_model, id: SecureRandom.uuid) }

    let(:model) {
      model = FactoryBot.create(:leihs_model, manufacturer: Faker::Company.name, type: "Software")
      model.add_recommend(compatible1)
      model.add_recommend(compatible2)
      model
    }

    it "returns status 200 and the compatible models for a given model" do
      resp = client.get "/inventory/models-compatibles/#{model.id}"
      expect(resp.status).to eq(200)
      expect(resp.body.count).to eq(2)
    end

    it "returns status 200 and no compatible models for a given compatible model" do
      resp = client.get "/inventory/models-compatibles/#{compatible1.id}"
      expect(resp.status).to eq(200)
      expect(resp.body.count).to eq(0)
    end

    it "returns status 200 and no compatible models for another given compatible model" do
      resp = client.get "/inventory/models-compatibles/#{compatible2.id}"
      expect(resp.status).to eq(200)
      expect(resp.body.count).to eq(0)
    end
  end
end
