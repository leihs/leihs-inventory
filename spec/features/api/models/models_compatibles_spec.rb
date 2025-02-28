require "spec_helper"
require "pry"
require_relative "../_shared"

feature "Inventory API Endpoints - Compatible Models" do
    let(:client) { plain_faraday_json_client }
  context "when fetching compatible models for an inventory pool", driver: :selenium_headless do
    include_context :setup_access_rights


    context "GET /inventory/models-compatibles" do
      it "retrieves no compatible models and returns status 200" do
        resp = client.get "/inventory/models-compatibles?page=1&size=100"
        expect(resp.status).to eq(200)
        expect(resp.body["pagination"]["total_records"]).to eq(0)
      end

      it "retrieves no compatible models and returns status 200" do
        resp = client.get "/inventory/models-compatibles"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(0)
      end

      it "retrieves paginated results with no compatible models and returns status 200" do
        resp = client.get "/inventory/models-compatibles?page=1&size=1"
        expect(resp.status).to eq(200)
        expect(resp.body["pagination"]["total_records"]).to eq(0)
      end
    end

  end



  context "when correct compatibles" do

    let(:model) {
      model = FactoryBot.create(:leihs_model, manufacturer: Faker::Company.name, type: "Software")

      compatible = FactoryBot.create(:leihs_model, id: SecureRandom.uuid)
      model.add_recommend(compatible)

      compatible = FactoryBot.create(:leihs_model, id: SecureRandom.uuid)
      model.add_recommend(compatible)
      model
    }


    it "retrieves no compatible models and returns status 200" do
      resp = client.get "/inventory/models-compatibles/#{model.id}"
      expect(resp.status).to eq(200)
      expect(resp.body.count).to eq(2)
    end

  end
end
