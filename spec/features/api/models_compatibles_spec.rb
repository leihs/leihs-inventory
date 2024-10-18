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

feature "Inventory API Endpoints" do
  context "when fetching models for a specific inventory pool", driver: :selenium_headless do
    before :each do
      @user = FactoryBot.create(:user, login: "test", password: "password")
      @inventory_pool = FactoryBot.create(:inventory_pool)

      FactoryBot.create(:direct_access_right, inventory_pool_id: @inventory_pool.id, user_id: @user.id, role: "group_manager")

      @models = 3.times.map do
        FactoryBot.create(:leihs_model, id: SecureRandom.uuid)
      end

      LeihsModel.all.each do |model|
        FactoryBot.create(:item, leihs_model: model, inventory_pool_id: @inventory_pool.id, responsible: @inventory_pool, is_borrowable: true)
      end
    end

    let(:client) { plain_faraday_json_client }

    context "GET /inventory/models-compatibles" do
      it "retrieves all compatible models and returns 200" do
        resp = client.get "/inventory/models-compatibles"
        expect(resp.status).to eq(200)
        expect(resp.body["pagination"]["total_records"]).to eq(0)
      end

      it "returns paginated results with status 200" do
        resp = client.get "/inventory/models-compatibles?page=1&size=1"
        expect(resp.status).to eq(200)
        expect(resp.body["pagination"]["total_records"]).to eq(0)
      end

      context "when models are linked as compatible" do
        let(:first_model) { @models.first }

        before :each do
          compatible_model = FactoryBot.create(:leihs_model, id: SecureRandom.uuid)
          first_model.add_recommend(compatible_model)

          binding.pry
        end

        it "returns paginated compatible models with status 200" do
          resp = client.get "/inventory/models-compatibles?page=1&size=1"
          expect(resp.status).to eq(200)
          expect(resp.body["data"].count).to eq(1)
          expect(resp.body["pagination"]["total_records"]).to eq(1)
        end

        it "retrieves a specific compatible model by ID and returns 200" do
          resp = client.get "/inventory/models-compatibles/#{first_model.id}"
          expect(resp.status).to eq(200)
          expect(resp.body.count).to eq(1)
        end
      end
    end
  end
end
