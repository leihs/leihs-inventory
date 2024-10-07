require "spec_helper"
require "pry"
require "securerandom"
require "../../leihs/database/lib/leihs/constants"

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

feature "Inventory Model Management via API" do
  context "Retrieving models from an inventory pool", driver: :selenium_headless do
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

    context "User with group manager access rights" do
      before :each do
        group_user = GroupUser.find(user_id: @user.id,
          group_id: Leihs::Constants::ALL_USERS_GROUP_UUID)
        FactoryBot.create(:group_access_right, inventory_pool_id: @inventory_pool.id, group_id: group_user.group_id, role: "group_manager")
      end

      it "successfully retrieves available models with a 200 status" do
        resp = client.get "/inventory/pools?login=#{@login}"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to be 1
      end
    end

    context "User without any access rights" do
      it "returns no models with a 200 status" do
        resp = client.get "/inventory/pools?login=#{@login}"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to be 0
      end
    end

    context "Creating and retrieving a new model" do
      before :each do
        category = FactoryBot.create(:category, name: "Test-ModelGroup")
        resp = create_model(client, @inventory_pool.id, "Example Model", [category.id])
        expect(resp.status).to eq(200)
      end

      it "does not retrieve the new model without access rights, returns 200 status" do
        resp = client.get "/inventory/pools?login=#{@login}"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to be 0
      end

      it "retrieves the new model with group manager access rights, returns 200 status" do
        group_user = GroupUser.find(user_id: @user.id,
          group_id: Leihs::Constants::ALL_USERS_GROUP_UUID)
        FactoryBot.create(:group_access_right, inventory_pool_id: @inventory_pool.id, group_id: group_user.group_id, role: "group_manager")

        resp = client.get "/inventory/pools?login=#{@login}"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to be 1
      end
    end
  end
end
