require "spec_helper"
require "pry"
require_relative "../_shared"

feature "Inventory API Endpoints" do
  context "when fetching models for a specific inventory pool", driver: :selenium_headless do
    before :each do
      @user = FactoryBot.create(:user, login: "test", password: "password")
      @inventory_pool = FactoryBot.create(:inventory_pool)

      @direct_access_right = FactoryBot.create(:direct_access_right, inventory_pool_id: @inventory_pool.id, user_id: @user.id, role: "group_manager")

      @models = 3.times.map do
        FactoryBot.create(:leihs_model, id: SecureRandom.uuid)
      end

      LeihsModel.all.each do |model|
        FactoryBot.create(:item, leihs_model: model, inventory_pool_id: @inventory_pool.id, responsible: @inventory_pool, is_borrowable: true)
      end
    end

    before do
      @admin, @admin_cookies, @user_cookies_str, @cookie_token = create_and_login(:admin)
    end

    let(:client) { session_auth_plain_faraday_json_client(cookies: @admin_cookies) }

    context "GET /inventory/models-compatibles" do
      it "retrieves all compatible models and returns status 200 with no results" do
        resp = client.get "/inventory/models-compatibles"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(0)
      end

      it "retrieves paginated results with status 200 and no models" do
        resp = client.get "/inventory/models-compatibles?page=1&size=1"
        expect(resp.status).to eq(200)
        expect(resp.body["pagination"]["total_rows"]).to eq(0)
      end

      context "when compatible models are linked" do
        let(:model_with_props) { @models.first }

        before :each do
          compatible_model = FactoryBot.create(:leihs_model, id: SecureRandom.uuid)
          model_with_props.add_recommend(compatible_model)
        end

        it "returns paginated compatible models with status 200" do
          resp = client.get "/inventory/models-compatibles?page=1&size=1"
          expect(resp.status).to eq(200)
          expect(resp.body["data"].count).to eq(1)
          expect(resp.body["pagination"]["total_rows"]).to eq(1)
        end

        it "retrieves a specific compatible model by ID and returns status 200" do
          resp = client.get "/inventory/models-compatibles/#{model_with_props.id}"
          expect(resp.status).to eq(200)
          expect(resp.body.count).to eq(1)
        end
      end
    end
  end
end
