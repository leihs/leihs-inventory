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

      first_model = @models.first

      # -------------------------

      # links = FactoryBot.create(:model_link)
      # ok model-group
      @category = FactoryBot.create(:category, direct_models: [@models.first])

      # ok group_access_right & access_right
      @manager = FactoryBot.create :user
      @group = FactoryBot.create(:group, name: "Group 1")
      FactoryBot.create :access_right, user: @manager, inventory_pool: @inventory_pool, role: "lending_manager"
      FactoryBot.create :group_access_right, group_id: @group.id,
        inventory_pool_id: @inventory_pool.id, role: "customer"

      FactoryBot.create(:supplier)
      building = FactoryBot.create(:building)

      b = Building.find(name: building.name)
      FactoryBot.create(:room, building: b)

      @model = FactoryBot.create(:leihs_model)
      # @image = FactoryBot.create(:image, target: @model)
      # @image = FactoryBot.create(:image)

      @image = FactoryBot.create(:image, :for_category)
      @filename = @image.filename

      @image = FactoryBot.create(:image, :for_leihs_model)
      @filename = @image.filename

      puts " filename: #{@filename}"
      # binding.pry
    end

    let(:client) { plain_faraday_json_client }
    [
      "/inventory/departments",
      "/inventory/owners"
    ].each do |url|
      context "GET /inventory/models-compatibles" do
        it "retrieves all compatible models and returns 200" do
          resp = client.get url
          expect(resp.status).to eq(200)
          expect(resp.body["pagination"]["total_records"]).to eq(4)
        end

        it "returns paginated results with status 200" do
          resp = client.get "#{url}?page=3&size=1"
          expect(resp.status).to eq(200)
          expect(resp.body["pagination"]["total_records"]).to eq(4)

          id = resp.body["data"][0]["id"]
          resp = client.get "#{url}/#{id}"
          expect(resp.status).to eq(200)
          expect(resp.body.count).to eq(1)
        end
      end
    end
  end
end
