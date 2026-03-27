require "spec_helper"
require "pry"
require_relative "../_shared"

describe "Swagger Inventory Endpoints - Models" do
  context "when fetching models for an inventory pool" do
    include_context :setup_models_min_api

    before :each do
      @user, @user_cookies, @user_cookies_str, @cookie_token = create_and_login(:user)
      FactoryBot.create(:access_right,
        inventory_pool_id: @inventory_pool.id,
        user_id: @user.id,
        role: "inventory_manager")
    end

    let(:client) { session_auth_plain_faraday_json_client(cookies: @user_cookies) }
    let(:inventory_pool_id) { @inventory_pool.id }
    let(:url) { "/inventory/#{inventory_pool_id}/list/" }

    context "GET /inventory/:pool-id/list/" do
      before :each do
        @models = create_models
      end

      it "retrieves all models and returns status 200" do
        resp = client.get url
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(4) # all empty models
      end

      it "supports pagination and returns status 200 with limited results" do
        resp = client.get "#{url}?page=3&size=1"
        expect(resp.status).to eq(200)
        expect(resp.body["data"].count).to eq(1)  # FIXME
      end

      it "excludes package children from borrowable_quantity in list availability" do
        package_model = FactoryBot.create(:leihs_model,
          product: "Package for List Rentable",
          type: "Model",
          is_package: true)
        child_model = FactoryBot.create(:leihs_model,
          product: "Child Model List Rentable",
          type: "Model",
          is_package: false)

        package_item = FactoryBot.create(:item,
          leihs_model: package_model,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          is_borrowable: true)

        FactoryBot.create(:item,
          leihs_model: child_model,
          parent_id: package_item.id,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          is_borrowable: true)

        resp = client.get "#{url}?page=1&size=50&with_items=true&type=model&search=Child%20Model%20List%20Rentable"
        expect(resp.status).to eq(200)

        row = resp.body["data"].find { |m| m["id"] == child_model.id }
        expect(row).not_to be_nil
        expect(row["in_stock_quantity"]).to eq(0)
        expect(row["borrowable_quantity"]).to eq(0)
      end

      it "treats absent retired like false for list quantity aggregates" do
        model = FactoryBot.create(:leihs_model,
          product: "List Quantities Retired Filter",
          type: "Model",
          is_package: false)

        FactoryBot.create(:item,
          leihs_model: model,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          is_borrowable: true)

        FactoryBot.create(:item,
          leihs_model: model,
          inventory_pool_id: @inventory_pool.id,
          owner_id: @inventory_pool.id,
          is_borrowable: true,
          retired: Date.today)

        q = "#{url}?page=1&size=50&with_items=true&type=model&search=List%20Quantities%20Retired%20Filter"

        resp_all = client.get q
        expect(resp_all.status).to eq(200)
        row_all = resp_all.body["data"].find { |m| m["id"] == model.id }
        expect(row_all["borrowable_quantity"]).to eq(1)
        expect(row_all["in_stock_quantity"]).to eq(1)

        resp_active = client.get "#{q}&retired=false"
        expect(resp_active.status).to eq(200)
        row_active = resp_active.body["data"].find { |m| m["id"] == model.id }
        expect(row_active["borrowable_quantity"]).to eq(1)
        expect(row_active["in_stock_quantity"]).to eq(1)

        resp_retired = client.get "#{q}&retired=true"
        expect(resp_retired.status).to eq(200)
        row_retired = resp_retired.body["data"].find { |m| m["id"] == model.id }
        expect(row_retired["borrowable_quantity"]).to eq(1)
        expect(row_retired["in_stock_quantity"]).to eq(1)
      end
    end

    context "GET /inventory/:pool_id/models for a specific pool" do
      it "returns an empty list for a new pool and returns status 200" do
        resp = client.get url
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(0)
      end

      it "returns paginated empty results for a new pool and returns status 200" do
        resp = client.get "#{url}?page=3&size=1"
        expect(resp.status).to eq(200)
        expect(resp.body["data"].count).to eq(0)
      end
    end

    context "POST and GET /inventory/:pool_id/models when creating new models" do
      before :each do
        category = FactoryBot.create(:category, name: Faker::Company.name)
        model = FactoryBot.create(:leihs_model, manufacturer: Faker::Company.name, type: "Model", is_package: false,
          version: "1")

        category.add_direct_model(model)
        @model_id = model.id
      end

      it "returns one model after creation and returns status 200" do
        resp = client.get url
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(1)
      end

      context "when adding another model" do
        before :each do
          category = FactoryBot.create(:category, name: Faker::Company.name)
          model = FactoryBot.create(:leihs_model, manufacturer: Faker::Company.name, type: "Model", is_package: false,
            version: "1")

          category.add_direct_model(model)
        end

        it "returns both models and returns status 200" do
          resp = client.get url
          expect(resp.status).to eq(200)
          expect(resp.body.count).to eq(2)
        end
      end
    end
  end
end
