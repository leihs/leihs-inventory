require "spec_helper"
require "pry"
require_relative "../_shared"

describe "Call inventory-pool endpoints" do
  context "when retrieving models from an inventory pool" do
    before :each do
      @admin, @admin_cookies, @admin_cookies_str, @cookie_token = create_and_login(:admin)
    end

    let(:admin_client) { session_auth_plain_faraday_json_csrf_client(cookies: @admin_cookies) }

    context "with both direct and group access rights" do
      before :each do
        ["inventory_manager", "lending_manager", "group_manager", "customer"].each do |role|
          pool = FactoryBot.create(:inventory_pool)
          FactoryBot.create(:direct_access_right, inventory_pool_id: pool.id, user_id: @admin.id, role: role)
        end

        ["inventory_manager", "lending_manager", "group_manager", "customer"].each do |role|
          pool = FactoryBot.create(:inventory_pool)
          group_user = GroupUser.find(user_id: @admin.id, group_id: Leihs::Constants::ALL_USERS_GROUP_UUID)
          FactoryBot.create(:group_access_right, inventory_pool_id: pool.id, group_id: group_user.group_id, role: role)
        end
      end

      it "returns all inventory pools with access via /inventory/pools" do
        resp = admin_client.get "/inventory/pools/"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(8)
      end

      it "returns all inventory pools via /pools-by-access-right" do
        resp = admin_client.get "/inventory/pools-by-access-right/"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(8)
      end

      it "returns all pools with min=false" do
        resp = admin_client.get "/inventory/pools-by-access-right/?min=false"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(8)
      end

      it "returns all pools with min=true" do
        resp = admin_client.get "/inventory/pools-by-access-right/?min=true"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(8)
      end

      it "returns all pools with access_rights=all" do
        resp = admin_client.get "/inventory/pools-by-access-right/?access_rights=all"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(8)
      end

      it "returns only pools with direct access" do
        resp = admin_client.get "/inventory/pools-by-access-right/?access_rights=direct_access_rights"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(4)

        profile = admin_client.get "/inventory/profile/"
        expect(profile.status).to eq(200)
        expect(profile.body["available_inventory_pools"].count).to eq(4)
      end

      it "returns only pools with group access" do
        resp = admin_client.get "/inventory/pools-by-access-right/?access_rights=group_access_rights"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(4)
      end
    end

    context "with only group access rights" do
      before :each do
        ["inventory_manager", "lending_manager", "group_manager", "customer"].each do |role|
          pool = FactoryBot.create(:inventory_pool)
          group_user = GroupUser.find(user_id: @admin.id, group_id: Leihs::Constants::ALL_USERS_GROUP_UUID)
          FactoryBot.create(:group_access_right, inventory_pool_id: pool.id, group_id: group_user.group_id, role: role)
        end
      end

      it "returns group-accessible pools via /inventory/pools/" do
        resp = admin_client.get "/inventory/pools/"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(4)
      end

      it "returns group-accessible pools via /inventory/profile/" do
        resp = admin_client.get "/inventory/profile/"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(4)
      end

      it "returns zero direct access pools" do
        resp = admin_client.get "/inventory/pools-by-access-right/?access_rights=direct_access_rights"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(0)
      end

      it "returns only group-accessible pools" do
        resp = admin_client.get "/inventory/pools-by-access-right/?access_rights=group_access_rights"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(4)
      end

      it "returns all accessible pools (group-only)" do
        resp = admin_client.get "/inventory/pools-by-access-right/"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(4)
      end
    end

    context "with only direct access rights" do
      before :each do
        ["inventory_manager", "lending_manager", "group_manager", "customer"].each do |role|
          pool = FactoryBot.create(:inventory_pool)
          FactoryBot.create(:direct_access_right, inventory_pool_id: pool.id, user_id: @admin.id, role: role)
        end
      end

      it "returns directly accessible pools via /inventory/pools/" do
        resp = admin_client.get "/inventory/pools/"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(4)
      end

      it "returns directly accessible pools via /inventory/profile/" do
        resp = admin_client.get "/inventory/profile/"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(4)
      end

      it "returns only direct access pools via /pools-by-access-right/" do
        resp = admin_client.get "/inventory/pools-by-access-right/?access_rights=direct_access_rights"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(4)
      end

      it "returns zero group access pools" do
        resp = admin_client.get "/inventory/pools-by-access-right/?access_rights=group_access_rights"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(0)
      end

      it "returns all accessible pools (direct-only)" do
        resp = admin_client.get "/inventory/pools-by-access-right/"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(4)
      end
    end
  end
end
