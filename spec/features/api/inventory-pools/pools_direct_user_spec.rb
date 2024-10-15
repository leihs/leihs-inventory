require "spec_helper"
require "pry"
require "securerandom"

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

feature "Inventory Management via Swagger Endpoints" do
  context "when retrieving models from an inventory pool", driver: :selenium_headless do
    before :each do
      @login = "test"
      @user = FactoryBot.create(:user, id: "ce3665a0-2711-44b8-aa47-11fb881c3f91", login: @login, password: "password")
      @inventory_pool = FactoryBot.create(:inventory_pool, id: "8f613f14-3b6d-4d5c-9804-913e2da1109e")

      @models = 3.times.map do
        FactoryBot.create(:leihs_model, id: SecureRandom.uuid)
      end

      LeihsModel.all.each do |model|
        FactoryBot.create(:item, leihs_model: model, inventory_pool_id: @inventory_pool.id, responsible: @inventory_pool, is_borrowable: true)
      end
    end

    let(:client) { plain_faraday_json_client }

    context "with direct access rights as a group manager" do
      before :each do
        FactoryBot.create(:direct_access_right, inventory_pool_id: @inventory_pool.id, user_id: @user.id, role: "group_manager")
      end

      it "returns available models in the pool with a 200 status" do
        resp = client.get "/inventory/pools?login=#{@login}"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to be 1
      end
    end

    context "without any access rights" do
      it "returns no models and a 200 status" do
        resp = client.get "/inventory/pools?login=#{@login}"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to be 0
      end
    end

    context "when creating a new model and retrieving it from the pool" do
      before :each do
        category = FactoryBot.create(:category, name: "Test-ModelGroup")
        resp = create_model(client, @inventory_pool.id, "Example Model", [category.id])
        expect(resp.status).to eq(200)
      end

      it "returns the newly created model with a 200 status" do
        resp = client.get "/inventory/pools?login=#{@login}"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to be 0
      end

      it "returns the created model with access rights and a 200 status" do
        FactoryBot.create(:direct_access_right, inventory_pool_id: @inventory_pool.id, user_id: @user.id, role: "group_manager")
        resp = client.get "/inventory/pools?login=#{@login}"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to be 1
      end
    end
  end
end
