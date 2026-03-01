require "spec_helper"
require_relative "_shared"

describe "Swagger Inventory Endpoints - Fields" do
  context "when retrieving fields for an inventory pool" do
    include_context :setup_models_min_api

    before :each do
      @user, @user_cookies, @user_cookies_str, @cookie_token = create_and_login(:user)
      FactoryBot.create(:access_right,
        inventory_pool_id: @inventory_pool.id,
        user_id: @user.id,
        role: "inventory_manager")

      @other_pool = FactoryBot.create(:inventory_pool)

      owner_suffix = SecureRandom.hex(4)
      @owner_field = FactoryBot.create(:field,
        id: "properties_owner_only_#{owner_suffix}",
        data: Sequel.pg_jsonb({
          label: "Owner Only Field",
          type: "text",
          group: "General Information",
          attribute: ["properties", "owner_only_#{owner_suffix}"],
          target_type: "item",
          permissions: {
            role: "inventory_manager",
            owner: true
          }
        }))

      regular_suffix = SecureRandom.hex(4)
      @regular_field = FactoryBot.create(:field,
        id: "properties_regular_#{regular_suffix}",
        data: Sequel.pg_jsonb({
          label: "Regular Field",
          type: "text",
          group: "General Information",
          attribute: ["properties", "regular_#{regular_suffix}"],
          target_type: "item",
          permissions: {
            role: "inventory_manager",
            owner: false
          }
        }))

      @model = FactoryBot.create(:leihs_model, product: "Test Product")
      @item_owned_by_pool = FactoryBot.create(:item,
        leihs_model: @model,
        inventory_pool_id: @inventory_pool.id,
        owner_id: @inventory_pool.id)
      @item_owned_by_other = FactoryBot.create(:item,
        leihs_model: @model,
        inventory_pool_id: @inventory_pool.id,
        owner_id: @other_pool.id)
    end

    let(:client) { session_auth_plain_faraday_json_client(cookies: @user_cookies) }
    let(:inventory_pool_id) { @inventory_pool.id }

    context "GET /inventory/:pool-id/fields/" do
      it "does not add protected attribute when no resource_id is provided (creating as owner)" do
        resp = client.get "/inventory/#{inventory_pool_id}/fields/" do |req|
          req.params["target_type"] = "item"
          req.headers["Accept"] = "application/json"
        end

        expect(resp.status).to eq(200)

        owner_field = resp.body["fields"].find { |f| f["id"] == @owner_field.id }
        regular_field = resp.body["fields"].find { |f| f["id"] == @regular_field.id }

        expect(owner_field).not_to be_nil
        expect(owner_field.key?("protected")).to be false

        expect(regular_field).not_to be_nil
        expect(regular_field.key?("protected")).to be false
      end
    end

    context "GET /inventory/:pool-id/items/:item-id" do
      it "marks owner-only fields as protected when item is owned by different pool" do
        resp = client.get "/inventory/#{inventory_pool_id}/items/#{@item_owned_by_other.id}" do |req|
          req.headers["Accept"] = "application/json"
        end

        expect(resp.status).to eq(200)
        expect(resp.body["fields"]).to be_an(Array)

        owner_field = resp.body["fields"].find { |f| f["id"] == @owner_field.id }
        regular_field = resp.body["fields"].find { |f| f["id"] == @regular_field.id }

        expect(owner_field).not_to be_nil
        expect(owner_field["protected"]).to be true
        expect(owner_field["protected_reason"]).to eq("editable for owner only")

        expect(regular_field).not_to be_nil
        expect(regular_field["protected"]).to be false
        expect(regular_field.key?("protected_reason")).to be false
      end

      it "does not mark owner-only fields as protected when item is owned by same pool" do
        resp = client.get "/inventory/#{inventory_pool_id}/items/#{@item_owned_by_pool.id}" do |req|
          req.headers["Accept"] = "application/json"
        end

        expect(resp.status).to eq(200)

        owner_field = resp.body["fields"].find { |f| f["id"] == @owner_field.id }
        expect(owner_field).not_to be_nil
        expect(owner_field["protected"]).to be false
        expect(owner_field.key?("protected_reason")).to be false
      end
    end
  end
end
