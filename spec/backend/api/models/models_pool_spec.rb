require "spec_helper"
require "pry"
require_relative "../_shared"

describe "Swagger Inventory Endpoints - Models" do
  context "when fetching models for an inventory pool" do
    include_context :setup_models_min_api

    before :each do
      @user, @user_cookies, @user_cookies_str, @cookie_token = create_and_login(:user)
    end

    let(:client) { session_auth_plain_faraday_json_client(cookies: @user_cookies) }
    let(:inventory_pool_id) { @inventory_pool.id }
    let(:url) { "/inventory/#{inventory_pool_id}/models/" }

    context "GET /inventory/:pool-id/models" do
      before :each do
        @models = create_models
      end

      it "retrieves all models and returns status 200" do
        resp = client.get url
        expect(resp.status).to eq(200)
        expect(resp.body["data"].count).to eq(4) # all empty models
      end

      it "supports pagination and returns status 200 with limited results" do
        resp = client.get "#{url}?page=3&size=1"
        expect(resp.status).to eq(200)
        expect(resp.body["data"].count).to eq(1)  # FIXME
      end
    end

    context "GET /inventory/:pool_id/models for a specific pool" do
      it "returns an empty list for a new pool and returns status 200" do
        resp = client.get url
        expect(resp.status).to eq(200)
        expect(resp.body["data"].count).to eq(0)
      end

      it "returns paginated empty results for a new pool and returns status 200" do
        resp = client.get "#{url}?page=3&size=1"
        expect(resp.status).to eq(200)
        expect(resp.body["data"].count).to eq(0)
      end
    end

    context "POST and GET /inventory/:pool_id/models when creating new models" do
      before :each do
        category = FactoryBot.create(:category)
        resp = create_model_post(client, inventory_pool_id, Faker::Lorem.word, [category.id])
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(1)
        @model_id = resp.body[0]["id"]
      end

      it "returns one model after creation and returns status 200" do
        resp = client.get url
        expect(resp.status).to eq(200)
        expect(resp.body["data"].count).to eq(1)
      end

      context "when adding another model" do
        before :each do
          category = FactoryBot.create(:category)
          resp = create_model_post(client, inventory_pool_id, Faker::Lorem.word, [category.id])

          expect(resp.status).to eq(200)
          expect(resp.body.count).to eq(1)
        end

        it "returns both models and returns status 200" do
          resp = client.get url
          expect(resp.status).to eq(200)
          expect(resp.body["data"].count).to eq(2)
        end
      end
    end
  end
end
