require "spec_helper"
require "pry"
require_relative "../_shared"

describe "Swagger Inventory Endpoints - Model Rentable" do
  context "when fetching a model with rentable count" do
    include_context :setup_models_min_api

    before :each do
      @user, @user_cookies, @user_cookies_str, @cookie_token = create_and_login(:user)
      FactoryBot.create(:access_right,
        inventory_pool_id: @inventory_pool.id,
        user_id: @user.id,
        role: "inventory_manager")

      @model = FactoryBot.create(:leihs_model,
        product: "Rentable Test Model",
        is_package: false)
    end

    let(:client) { session_auth_plain_faraday_json_client(cookies: @user_cookies) }
    let(:inventory_pool_id) { @inventory_pool.id }
    let(:url) { "/inventory/#{inventory_pool_id}/models/#{@model.id}" }

    context "GET /inventory/:pool-id/models/:model-id" do
      it "returns rentable 0 when model has no items" do
        resp = client.get url
        expect(resp.status).to eq(200)
        expect(resp.body["rentable"]).to eq(0)
      end

      it "counts only borrowable items as rentable" do
        3.times do
          FactoryBot.create(:item,
            leihs_model: @model,
            inventory_pool_id: @inventory_pool.id,
            responsible: @inventory_pool,
            is_borrowable: true)
        end

        FactoryBot.create(:item,
          leihs_model: @model,
          inventory_pool_id: @inventory_pool.id,
          responsible: @inventory_pool,
          is_borrowable: false)

        resp = client.get url
        expect(resp.status).to eq(200)
        expect(resp.body["rentable"]).to eq(3)
      end

      it "excludes retired items from rentable count" do
        2.times do
          FactoryBot.create(:item,
            leihs_model: @model,
            inventory_pool_id: @inventory_pool.id,
            responsible: @inventory_pool,
            is_borrowable: true)
        end

        FactoryBot.create(:item,
          leihs_model: @model,
          inventory_pool_id: @inventory_pool.id,
          responsible: @inventory_pool,
          is_borrowable: true,
          retired: Date.today,
          retired_reason: "test")

        resp = client.get url
        expect(resp.status).to eq(200)
        expect(resp.body["rentable"]).to eq(2)
      end

      it "excludes package children from rentable count" do
        parent_item = FactoryBot.create(:item,
          leihs_model: @model,
          inventory_pool_id: @inventory_pool.id,
          responsible: @inventory_pool,
          is_borrowable: true)

        FactoryBot.create(:item,
          leihs_model: @model,
          inventory_pool_id: @inventory_pool.id,
          responsible: @inventory_pool,
          is_borrowable: true,
          parent_id: parent_item.id)

        resp = client.get url
        expect(resp.status).to eq(200)
        expect(resp.body["rentable"]).to eq(1)
      end

      it "excludes items from other inventory pools" do
        other_pool = FactoryBot.create(:inventory_pool)

        2.times do
          FactoryBot.create(:item,
            leihs_model: @model,
            inventory_pool_id: @inventory_pool.id,
            responsible: @inventory_pool,
            is_borrowable: true)
        end

        FactoryBot.create(:item,
          leihs_model: @model,
          inventory_pool_id: other_pool.id,
          responsible: other_pool,
          is_borrowable: true)

        resp = client.get url
        expect(resp.status).to eq(200)
        expect(resp.body["rentable"]).to eq(2)
      end

      it "combines all exclusion conditions correctly" do
        3.times do
          FactoryBot.create(:item,
            leihs_model: @model,
            inventory_pool_id: @inventory_pool.id,
            responsible: @inventory_pool,
            is_borrowable: true)
        end

        FactoryBot.create(:item,
          leihs_model: @model,
          inventory_pool_id: @inventory_pool.id,
          responsible: @inventory_pool,
          is_borrowable: false)

        FactoryBot.create(:item,
          leihs_model: @model,
          inventory_pool_id: @inventory_pool.id,
          responsible: @inventory_pool,
          is_borrowable: true,
          retired: Date.today,
          retired_reason: "test")

        parent_item = FactoryBot.create(:item,
          leihs_model: @model,
          inventory_pool_id: @inventory_pool.id,
          responsible: @inventory_pool,
          is_borrowable: true)

        FactoryBot.create(:item,
          leihs_model: @model,
          inventory_pool_id: @inventory_pool.id,
          responsible: @inventory_pool,
          is_borrowable: true,
          parent_id: parent_item.id)

        other_pool = FactoryBot.create(:inventory_pool)
        FactoryBot.create(:item,
          leihs_model: @model,
          inventory_pool_id: other_pool.id,
          responsible: other_pool,
          is_borrowable: true)

        resp = client.get url
        expect(resp.status).to eq(200)
        # 3 valid + 1 parent_item (top-level, borrowable, not retired, correct pool) = 4
        expect(resp.body["rentable"]).to eq(4)
      end
    end
  end
end
