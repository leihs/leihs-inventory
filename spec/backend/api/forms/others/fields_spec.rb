# require "spec_helper"
# require "pry"
# require_relative "../../_shared"

# describe "Fetching Fields" do
#   context "when searching for fields in a specific inventory pool" do
#     include_context :setup_models_api, "inventory_manager"

#     before :each do
#       @user_cookies, @user_cookies_str, @cookie_token = create_and_login_by(@user)
#     end

#     let(:client) { session_auth_plain_faraday_json_client(cookies: @user_cookies) }
#     let(:pool_id) { @inventory_pool.id }

#     {
#       "customer" => 11,
#       "lending_manager" => 25,
#       "group_manager" => 11,
#       "inventory_manager" => 32
#     }.each do |role, expected_count|
#       context "GET /inventory/:pool_id/fields for role #{role}" do
#         let(:url) { "/inventory/fields?page=1&size=10&role=#{role}" }

#         it "returns #{expected_count} fields for role #{role} and status 200" do
#           resp = client.get url

#           expect(resp.status).to eq(200)
#           expect(resp.body["pagination"]["total_rows"]).to eq(expected_count)
#           expect(resp.body["data"].count).to eq(10)
#         end
#       end
#     end

#     context "GET /inventory/:pool_id/fields without specifying a role" do
#       let(:url) { "/inventory/fields?page=1&size=10" }

#       it "returns 46 total records and status 200" do
#         resp = client.get url

#         expect(resp.status).to eq(200)
#         expect(resp.body["pagination"]["total_rows"]).to eq(46)
#         expect(resp.body["pagination"]["size"]).to eq(10)
#         expect(resp.body["data"].count).to eq(10)
#       end
#     end

#     context "GET /inventory/:pool_id/fields without specifying a role" do
#       let(:url) { "/inventory/fields" }

#       it "returns 46 total records and status 200" do
#         resp = client.get url

#         expect(resp.status).to eq(200)
#         expect(resp.body.count).to eq(46)
#       end
#     end

#     context "GET /inventory/:pool_id/fields and retrieve specific field details" do
#       let(:url) { "/inventory/fields" }

#       it "returns the details of a specific field with status 200" do
#         resp = client.get "#{url}?page=1&size=10"
#         id = resp.body["data"][0]["id"]

#         resp = client.get "#{url}/#{id}"
#         expect(resp.status).to eq(200)
#         expect(resp.body["data"][0]["id"]).to eq(id)
#       end

#       it "compare counts of keys concerning filter with status 200" do
#         resp = client.get url
#         expect(resp.body.size).to eq(46)
#         expect(resp.status).to eq(200)

#         # TODO: filter should be removed ? test should use different endpoint
#         resp = client.get "#{url}?type=license"
#         expect(resp.body.size).to eq(16)
#         expect(resp.status).to eq(200)
#       end
#     end
#   end
# end
