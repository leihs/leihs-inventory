require "spec_helper"
require "pry"
require_relative "../_shared"

feature "Inventory API Endpoints - Items" do
  context "when fetching items for a specific model in an inventory pool", driver: :selenium_headless do
    include_context :setup_models_for_duplicates_api, "inventory_manager"

    let(:model_with_two_items) { @models.second }

    before :each do
      @user, @user_cookies, @user_cookies_str, @cookie_token = create_and_login(:user)
      @path = "/#{@inventory_pool.id}/"
    end

    let(:package) { FactoryBot.create(:package_model_with_parent_and_items, inventory_pool: @inventory_pool) }

    let(:client) { session_auth_plain_faraday_json_client(cookies: @user_cookies) }

    context "GET /inventory/models/*/items for a model with items" do
      let(:url) { "/inventory#{@path}models/#{model_with_two_items.id}/items" }

      it "retrieves all items for the model and returns status 200" do
        resp = client.get url
        expect(resp.status).to eq(200)
        expect(resp.body["data"].count).to eq(2)
      end
    end

    context "GET /inventory/models/*/items for a model with items" do
      let(:url) { "/inventory#{@path}models/#{package.id}/items" }

      it "retrieves all items for the model and returns status 200" do
        resp = client.get url
        expect(resp.status).to eq(200)
        expect(resp.body["data"].count).to eq(1)
      end
    end

    context "GET /inventory/models/*/items for a model with items" do
      let(:url) { "/inventory#{@path}models/#{package.items.first.model_id}/items" }

      it "retrieves all items for the model and returns status 200" do
        resp = client.get url
        expect(resp.status).to eq(200)
        expect(resp.body["data"].count).to eq(1)
      end
    end
  end
end
