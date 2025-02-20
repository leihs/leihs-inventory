require "spec_helper"
require "pry"
require_relative "../_shared"
require_relative "_common"
require "faker"

feature "Admin protection for dev-endpoints" do
  ["customer", "group_manager", "inventory_manager", "lending_manager"].each do |role|
    context "when interacting with inventory software as #{role}", driver: :selenium_headless do
      include_context :setup_user_with_direct_access_right, role
      include_context :generate_session_header

      let(:pool_id) { @inventory_pool.id }
      let(:cookie_header) { @cookie_header }
      let(:client) { plain_faraday_json_client(cookie_header) }

      ["model", "software", "license", "item", "option", "package"].each do |type|
        it "returns 404 for non-existent #{type}" do
          result = client.get "/inventory/#{pool_id}/dev/#{type}"
          expect(result.status).to eq(404)
        end
      end
    end
  end

  ["customer", "group_manager", "inventory_manager", "lending_manager"].each do |role|
    context "when interacting with inventory software as #{role} with admin rights/cookie-value", driver: :selenium_headless do
      include_context :setup_admin_with_direct_access_right, role
      include_context :generate_session_header, "*/*", [
        {"name" => "leihs-dev-modes", "value" => "[\"dev-forms-v0\"]"}
      ]

      let(:pool_id) { @inventory_pool.id }
      let(:cookie_header) { @cookie_header }
      let(:client) { plain_faraday_resource_client(cookie_header) }

      ["model", "software", "license", "item", "option", "package"].each do |type|
        it "returns 200 for existing #{type}" do
          result = client.get "/inventory/#{pool_id}/dev/#{type}"
          expect(result.status).to eq(200)
        end
      end
    end
  end

  ["customer", "group_manager", "inventory_manager", "lending_manager"].each do |role|
    context "when interacting with inventory software as #{role} with admin rights only", driver: :selenium_headless do
      include_context :setup_admin_with_direct_access_right, role
      include_context :generate_session_header, "*/*"

      let(:pool_id) { @inventory_pool.id }
      let(:cookie_header) { @cookie_header }
      let(:client) { plain_faraday_resource_client(cookie_header) }

      ["model", "software", "license", "item", "option", "package"].each do |type|
        it "returns 200 for existing #{type}" do
          result = client.get "/inventory/#{pool_id}/dev/#{type}"
          expect(result.status).to eq(200)
        end
      end
    end
  end
end
