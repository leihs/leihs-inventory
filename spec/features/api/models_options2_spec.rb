require "spec_helper"
require "pry"
require "#{File.dirname(__FILE__)}/_shared"

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

    include_context :setup_models_api

    # before :each do
    #   @user = FactoryBot.create(:user, login: "test", password: "password")
    #   @inventory_pool = FactoryBot.create(:inventory_pool)
    #
    #   FactoryBot.create(:direct_access_right, inventory_pool_id: @inventory_pool.id, user_id: @user.id, role: "group_manager")
    #
    #   @models = 3.times.map do
    #     FactoryBot.create(:leihs_model, id: SecureRandom.uuid)
    #   end
    #
    #   LeihsModel.all.each do |model|
    #     FactoryBot.create(:item, leihs_model: model, inventory_pool_id: @inventory_pool.id, responsible: @inventory_pool, is_borrowable: true)
    #   end
    #
    #   first_model = @models.first
    #
    #   # accessory
    #   cde=database[:accessories_inventory_pools].insert(
    #     accessory_id: FactoryBot.create(:accessory).id,
    #     inventory_pool_id: @inventory_pool.id
    #   )
    #
    #   # FIXME
    #   abc=FactoryBot.create(:accessory, leihs_model: first_model)
    #   puts ">>> Accessory.abc: #{abc.id}"
    #
    #   # binding.pry
    #
    #
    #   # upload_1 = FactoryBot.create(:upload)
    #   # upload_2 = FactoryBot.create(:upload)
    #   # attachment_1 = FactoryBot.create(:attachment, request_id: request.id)
    #   # attachment_2 = FactoryBot.create(:attachment, request_id: request.id)
    #
    #   # attachment = \
    #   #   FactoryBot.build(:attachment,
    #   #                    id: '919fbdd1-111c-49b7-aeb0-2d5d8825ed00')
    #   # first_model.add_attachment(attachment)
    #
    #
    #
    #   # item
    #   i1 = FactoryBot.create(:item,
    #                          leihs_model: first_model,
    #                          inventory_pool: @inventory_pool,
    #                          owner: @inventory_pool)
    #
    #   # a = FactoryBot.create :attachment, item: i1
    #   # @attachment_filenames << a.filename
    #   # puts ">>> attachment: #{a.filename}"
    #
    #   # add model-property
    #   puts ">>>>>>> first_model: #{first_model.id}"
    #   first_model.add_property(FactoryBot.create(:property, key: "test", value: "test"))
    #
    #   # ok / entitlements
    #   @group_1 = FactoryBot.create(:entitlement_group, inventory_pool_id: @inventory_pool.id)
    #   FactoryBot.create(:entitlement, leihs_model: first_model, entitlement_group: @group_1, quantity: 2)
    #
    #
    #   # -------------------------
    #
    #   # links = FactoryBot.create(:model_links)
    #   # links = FactoryBot.create(:model_link, model: first_model, model_group: FactoryBot.create(:category))
    #
    #   # binding.pry
    #
    #
    #
    #
    #
    #
    #
    #
    #
    #
    # end

    let(:fmodel) { @models.first }


    let(:client) { plain_faraday_json_client }

    let(:url) { "/inventory/models/#{fmodel.id}/accessories" }

    context "GET /inventory/models-compatibles" do
      it "retrieves all compatible models and returns 200" do

        # binding.pry
        resp = client.get url
        expect(resp.status).to eq(200)
        expect(resp.body["pagination"]["total_records"]).to eq(1)
      end

      it "returns paginated results with status 200" do
        resp = client.get "#{url}?page=1&size=1"
        expect(resp.status).to eq(200)
        expect(resp.body["pagination"]["total_records"]).to eq(1)
      end

      # context "when models are linked as compatible" do
      #   let(:first_model) { @models.first }
      #
      #   before :each do
      #     compatible_model = FactoryBot.create(:leihs_model, id: SecureRandom.uuid)
      #     first_model.add_recommend(compatible_model)
      #   end
      #
      #   it "returns paginated compatible models with status 200" do
      #     resp = client.get "/inventory/models-compatibles?page=1&size=1"
      #     expect(resp.status).to eq(200)
      #     expect(resp.body["data"].count).to eq(1)
      #     expect(resp.body["pagination"]["total_records"]).to eq(1)
      #   end
      #
      #   it "retrieves a specific compatible model by ID and returns 200" do
      #     resp = client.get "/inventory/models-compatibles/#{first_model.id}"
      #     expect(resp.status).to eq(200)
      #     expect(resp.body.count).to eq(1)
      #   end
      # end
    end
  end
end
