require "spec_helper"
require "cgi"
require_relative "_shared"

describe "Swagger Inventory Endpoints - Items list for Software (issue #2128)" do
  context "when listing licenses by software model_id" do
    include_context :setup_models_min_api

    before :each do
      @user, @user_cookies, @user_cookies_str, @cookie_token = create_and_login(:user)
      FactoryBot.create(:access_right,
        inventory_pool_id: @inventory_pool.id,
        user_id: @user.id,
        role: "inventory_manager")

      @software_model = FactoryBot.create(:leihs_model,
        product: "Spec Software",
        type: "Software")

      @building = FactoryBot.create(:building, name: "Spec Building")
      @room = FactoryBot.create(:room,
        name: "Spec Room",
        building_id: @building.id)

      @license = FactoryBot.create(:item,
        inventory_code: "LIC-OS-SPEC",
        leihs_model: @software_model,
        room_id: @room.id,
        inventory_pool_id: @inventory_pool.id,
        owner_id: @inventory_pool.id,
        properties: {operating_system: %w[windows linux], license_type: "single_workplace"})
    end

    let(:client) { session_auth_plain_faraday_json_client(cookies: @user_cookies) }
    let(:pool_id) { @inventory_pool.id }
    let(:base_url) { "/inventory/#{pool_id}/items/" }

    it "returns properties_operating_system when fields requests it" do
      fields = %w[
        id inventory_code inventory_pool_name properties_operating_system
      ].join(",")
      url = "#{base_url}?model_id=#{@software_model.id}&fields=#{CGI.escape(fields)}"
      resp = client.get(url)
      expect(resp.status).to eq(200)
      data = resp.body
      expect(data).to be_a(Array)
      expect(data.length).to eq(1)
      row = data.first
      expect(row["inventory_code"]).to eq("LIC-OS-SPEC")
      expect(row["properties_operating_system"]).to eq(%w[windows linux])
    end

    it "returns properties_license_type when fields requests it" do
      fields = %w[
        id inventory_code inventory_pool_name properties_license_type
      ].join(",")
      url = "#{base_url}?model_id=#{@software_model.id}&fields=#{CGI.escape(fields)}"
      resp = client.get(url)
      expect(resp.status).to eq(200)
      data = resp.body
      expect(data).to be_a(Array)
      row = data.first
      expect(row["properties_license_type"]).to eq("single_workplace")
    end
  end
end
