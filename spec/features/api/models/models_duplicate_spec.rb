require "spec_helper"
require "pry"
require_relative "../_shared"

feature "Inventory API Endpoints - Items" do
  context "when fetching items for a specific model in an inventory pool", driver: :selenium_headless do
    include_context :setup_models_for_duplicates_api, "inventory_manager"

    let(:model_with_items) { @models.first }
    let(:model_with_two_items) { @models.second }
    let(:model_without_items) { @models.third }

    before :each do
      @user, @user_cookies, @user_cookies_str, @cookie_token = create_and_login(:user)
    end

    let(:fake_package) { FactoryBot.create(:package_model_with_items, inventory_pool: @inventory_pool) }
    let(:fake_package2) { FactoryBot.create(:package_model_with_parent_and_items, inventory_pool: @inventory_pool) }

    let(:client) { session_auth_plain_faraday_json_client(cookies: @user_cookies) }

    ["/", "/#{@inventory_pool_id}"].each do |path|
      let(:url) { "/inventory#{path}models" }

      context "GET /inventory/models for a model with items" do
        it "retrieves all items for the model and returns status 200" do
          resp = client.get url
          expect(resp.status).to eq(200)
          expect(resp.body.count).to eq(1)

          products = resp.body.map { |item| item["product"] }.compact
          unique_products = products.uniq
          expect(products.count).to eq(unique_products.count)
          expect(products.count).to eq(1)
        end
      end
    end

    ["/", "/#{@inventory_pool_id}"].each do |path|
      let(:url) { "/inventory#{path}models/#{model_with_two_items.id}/items" }

      context "GET /inventory/models/*/items for a model with items" do
        it "retrieves all items for the model and returns status 200" do
          resp = client.get url
          expect(resp.status).to eq(200)
          expect(resp.body.count).to eq(1)
        end
      end
    end

    ["/", "/#{@inventory_pool_id}"].each do |path|
      let(:url) { "/inventory#{path}models/#{fake_package.id}/items" }

      context "GET /inventory/models/*/items for a model with items" do
        it "retrieves all items for the model and returns status 200" do
          resp = client.get url
          expect(resp.status).to eq(200)
          expect(resp.body.count).to eq(1)
        end
      end
    end

    ["/", "/#{@inventory_pool_id}"].each do |path|
      let(:url) { "/inventory#{path}models/#{fake_package2.id}/items" }

      context "GET /inventory/models/*/items for a model with items" do
        it "retrieves all items for the model and returns status 200" do
          resp = client.get url
          expect(resp.status).to eq(200)
          expect(resp.body.count).to eq(1)
        end
      end
    end

    ["/", "/#{@inventory_pool_id}"].each do |path|
      let(:url) { "/inventory#{path}models/#{fake_package2.items.first.model_id}/items" }

      context "GET /inventory/models/*/items for a model with items" do
        it "retrieves all items for the model and returns status 200" do
          resp = client.get url
          expect(resp.status).to eq(200)
          expect(resp.body.count).to eq(1)
        end
      end
    end
  end
end
