require "spec_helper"
require "pry"
require_relative "../../_shared"

describe "Fetching Fields" do
  context "when searching for fields in a specific inventory pool" do
    include_context :setup_models_api_license

    before :each do
      @user_cookies, @user_cookies_str, @cookie_token = create_and_login_by(@user)
    end

    let(:client) { session_auth_plain_faraday_json_client(cookies: @user_cookies) }
    let(:pool_id) { @inventory_pool.id }
    let(:direct_access_right_of_user) { @direct_access_right }

    {
      "inventory_manager" => 3,
      "lending_manager" => 3,
      "group_manager" => 3,
      "customer" => 3
    }.each do |role, expected_count|
      context "1) GET /inventory/:pool_id/fields for role #{role}" do
        before do
          direct_access_right_of_user.update(role: role)
        end

        it "returns #{expected_count} manufacturers for role #{role} and status 200" do
          resp = client.get "/inventory/manufacturers"
          expect(resp.body.size).to eq(expected_count)
          expect(resp.body[0].is_a?(String)).to eq(true)
          expect(resp.status).to eq(200)
        end

        it "returns #{expected_count} manufacturers for role #{role} and status 200" do
          resp = client.get "/inventory/manufacturers?in-detail=true"

          expect(resp.body.size).to eq(expected_count)
          expect(resp.body[0].is_a?(Hash)).to eq(true)
          expect(resp.status).to eq(200)
        end

        it "returns #{expected_count} manufacturers for role #{role} and status 200" do
          resp = client.get "/inventory/manufacturers?in-detail=false"

          expect(resp.body.size).to eq(expected_count)
          expect(resp.body[0].is_a?(String)).to eq(true)
          expect(resp.status).to eq(200)
        end
      end
    end

    {
      "inventory_manager" => 1,
      "lending_manager" => 1,
      "group_manager" => 1,
      "customer" => 1
    }.each do |role, expected_count|
      context "2) GET /inventory/:pool_id/fields for role #{role}" do
        before do
          direct_access_right_of_user.update(role: role)
        end

        it "returns #{expected_count} manufacturers for role #{role} and status 200" do
          resp = client.get "/inventory/manufacturers?type=Software"

          expect(resp.body.size).to eq(expected_count)
          expect(resp.body[0].is_a?(String)).to eq(true)
          expect(resp.status).to eq(200)
        end

        it "returns #{expected_count} manufacturers for role #{role} and status 200" do
          resp = client.get "/inventory/manufacturers?in-detail=true&type=Software"

          expect(resp.body.size).to eq(expected_count)
          expect(resp.body[0].is_a?(Hash)).to eq(true)
          expect(resp.status).to eq(200)
        end

        it "returns #{expected_count} manufacturers for role #{role} and status 200" do
          resp = client.get "/inventory/manufacturers?in-detail=false&type=Software"

          expect(resp.body.size).to eq(expected_count)
          expect(resp.body[0].is_a?(String)).to eq(true)
          expect(resp.status).to eq(200)
        end
      end
    end

    {
      "inventory_manager" => 2,
      "lending_manager" => 2,
      "group_manager" => 2,
      "customer" => 2
    }.each do |role, expected_count|
      context "3) GET /inventory/:pool_id/fields for role #{role}" do
        before do
          direct_access_right_of_user.update(role: role)
        end

        it "returns #{expected_count} manufacturers for role #{role} and status 200" do
          resp = client.get "/inventory/manufacturers?type=Model"
          expect(resp.body.size).to eq(expected_count)
          expect(resp.body[0].is_a?(String)).to eq(true)
          expect(resp.status).to eq(200)
        end

        it "returns #{expected_count} manufacturers for role #{role} and status 200" do
          resp = client.get "/inventory/manufacturers?in-detail=true&type=Model"

          expect(resp.body.size).to eq(expected_count)
          expect(resp.body[0].is_a?(Hash)).to eq(true)
          expect(resp.status).to eq(200)
        end

        it "returns #{expected_count} manufacturers for role #{role} and status 200" do
          resp = client.get "/inventory/manufacturers?in-detail=false&type=Model"

          expect(resp.body.size).to eq(expected_count)
          expect(resp.body[0].is_a?(String)).to eq(true)
          expect(resp.status).to eq(200)
        end
      end
    end
  end
end
