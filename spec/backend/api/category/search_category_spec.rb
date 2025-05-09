require "spec_helper"
require "pry"
require_relative "../_shared"

describe "Inventory API Endpoints - Models" do
  context "when fetching items for a specific model in an inventory pool" do
    include_context :setup_models_api, "inventory_manager"

    before :each do
      @user_cookies, @user_cookies_str, @cookie_token = create_and_login_by(@user)
    end

    let(:model_with_accessories) { @models.first }
    let(:model_without_accessories) { @models.third }
    let(:client) { session_auth_plain_faraday_json_csrf_client(cookies: @user_cookies) }
    let(:pool_id) { @inventory_pool.id }

    context "GET /inventory/:pool_id/search with a model without items" do
      let(:url) { "/inventory/#{pool_id}/search" }

      it "returns no items and status 200 when no model_group_link exists" do
        resp = client.get url

        expect(resp.status).to eq(200)
        expect(resp.body["pagination"]["total_rows"]).to eq(0)
        expect(resp.body["data"].count).to eq(0)
      end

      context "with a linked category model to pool" do
        include_context :setup_category_model_linked_to_pool

        it "returns one item and status 200 when model_group_link exists" do
          resp = client.get url
          expect(resp.status).to eq(200)
          expect(resp.body["pagination"]["total_rows"]).to eq(1)
          expect(resp.body["data"].count).to eq(1)
        end
      end

      context "with all models linked to the pool" do
        include_context :setup_category_model_linked_all_to_pool

        it "returns all items and status 200 when all models are linked to the pool" do
          resp = client.get url

          expect(resp.status).to eq(200)
          expect(resp.body["pagination"]["total_rows"]).to eq(9)
          expect(resp.body["data"].count).to eq(9)
        end

        it "returns no software items and status 200 when filtering by 'Software'" do
          resp = client.get "#{url}?type=Software"

          expect(resp.status).to eq(200)
          expect(resp.body["pagination"]["total_rows"]).to eq(0)
          expect(resp.body["data"].count).to eq(0)
        end

        it "returns all items and status 200 when filtering by 'Model'" do
          resp = client.get "#{url}?type=Model"

          # TODO: improve test assertions if needed
          expect(resp.status).to eq(200)
          expect(resp.body["pagination"]["total_rows"]).to eq(9)
          expect(resp.body["data"].count).to eq(9)
        end
      end
    end
  end
end
