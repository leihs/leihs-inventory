require "spec_helper"
require "pry"

def create_model(client, inventory_pool_id, product, category_ids)
  client.post "/inventory/#{inventory_pool_id}/models" do |req|
    req.body = {
      product: product,
      category_ids: category_ids,
      version: "1",
      type: "Model",
      is_package: false
    }.to_json
    req.headers["Content-Type"] = "application/json"
    req.headers["Accept"] = "application/json"
  end
end

feature "Swagger Inventory Endpoints" do
  context "when fetching models for a pool", driver: :selenium_headless do
    before :each do
      @user = FactoryBot.create(:user, login: "test", password: "password")

      @inventory_pool = FactoryBot.create(:inventory_pool)

      FactoryBot.create(:direct_access_right, inventory_pool_id: @inventory_pool.id, user_id: @user.id, role: "group_manager")

      @models = 3.times.map do |i|
        FactoryBot.create(:leihs_model, id: SecureRandom.uuid)
      end

      LeihsModel.all.each do |model|
        FactoryBot.create(:item, leihs_model: model, inventory_pool_id: @inventory_pool.id, responsible: @inventory_pool, is_borrowable: true)
      end

      # binding.pry

    end

    let(:client) { plain_faraday_json_client }

    context "GET /inventory/models-compatibles" do
      it "retrieves all models and returns 200" do

        # binding.pry

        resp = client.get "/inventory/models-compatibles"
        expect(resp.status).to eq(200)
        # expect(resp.body["data"].count).to eq(0)
        expect(resp.body["pagination"]["total_records"]).to eq(0)
      end

      it "supports pagination and returns 200 with limited results" do
        resp = client.get "/inventory/models-compatibles?page=1&size=1"
        binding.pry
        expect(resp.status).to eq(200)
        # expect(resp.body["data"].count).to eq(1)
        expect(resp.body["pagination"]["total_records"]).to eq(0)
      end

      context "GET /inventory/models-compatibles" do

        before :each do
          compatible_model = FactoryBot.create(:leihs_model, id: SecureRandom.uuid)
          first_model = @models.first
          binding.pry
          first_model.add_recommend(compatible_model)
        end

        it "supports pagination and returns 200 with limited results" do
          resp = client.get "/inventory/models-compatibles?page=3&size=1"

          binding.pry
          expect(resp.status).to eq(200)
          expect(resp.body["data"].count).to eq(1)
        expect(resp.body["pagination"]["total_records"]).to eq(1)
        end

        it "supports pagination and returns 200 with limited results" do
          resp = client.get "/inventory/models-compatibles?page=1&size=1"
          expect(resp.status).to eq(200)
          expect(resp.body["data"].count).to eq(1)
        expect(resp.body["pagination"]["total_records"]).to eq(1)
        end

        it "supports pagination and returns 200 with limited results" do
          resp = client.get "/inventory/models-compatibles/#{first_model.id}"
          expect(resp.status).to eq(200)
          expect(resp.body["data"].count).to eq(1)
        expect(resp.body["pagination"]["total_records"]).to eq(1)
        end
      end

    end
  end
  end
