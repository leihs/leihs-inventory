require "spec_helper"
require_relative "../../_shared"
require "faker"
require_relative "../_common"

RSpec.describe "Inventory templates API (access control)" do
  %w[group_manager customer].each do |role|
    context "as #{role}" do
      include_context :setup_models_api_model, role
      include_context :generate_session_header

      let(:cookie_header) { @cookie_header }
      let(:client) { plain_faraday_json_client(cookie_header) }
      let(:pool_id) { @inventory_pool.id }
      let(:model_id) { @models.first.id }
      let!(:template) { FactoryBot.create(:template, inventory_pool: @inventory_pool) }
      let(:template_id) { template.id }

      let(:expected_status) { 404 }

      def call(method, path, body: nil)
        case method
        when :get then client.get(path)
        when :post then json_client_post(path, body: body, headers: cookie_header)
        when :put then json_client_put(path, body: body, headers: cookie_header)
        when :delete then json_client_delete(path, headers: cookie_header)
        else raise ArgumentError, "Unknown method: #{method}"
        end
      end

      it "denies index, pagination, create, update, and delete" do
        endpoints = [
          [:get, "/inventory/#{pool_id}/templates/"],
          [:get, "/inventory/#{pool_id}/templates/?size=5&page=2"],
          [:post, "/inventory/#{pool_id}/templates/", {name: Faker::Commerce.product_name, models: [{id: model_id, quantity: 15}]}],
          [:post, "/inventory/#{pool_id}/templates/", {name: Faker::Commerce.product_name, models: [{id: model_id, quantity: 0}]}],
          [:put, "/inventory/#{pool_id}/templates/#{template_id}", {name: "updated-name", models: [{id: model_id, quantity: 0}]}],
          [:put, "/inventory/#{pool_id}/templates/#{template_id}", {name: "updated-name", models: [{id: model_id, quantity: 15}]}],
          [:delete, "/inventory/#{pool_id}/templates/#{template_id}", nil]
        ]

        endpoints.each do |method, path, body|
          resp = call(method, path, body: body)
          expect(resp.status).to eq(expected_status),
            "Expected #{method.upcase} #{path} => #{expected_status}, got #{resp.status} (body: #{resp.body.inspect})"
        end
      end

      it "denies create, update caused by invalid coercion" do
        endpoints = [
          [:post, "/inventory/#{pool_id}/templates/", {name: "", models: [{id: model_id, quantity: 0}]}],
          [:post, "/inventory/#{pool_id}/templates/", {name: Faker::Commerce.product_name, models: [{id: model_id, quantity: -1}]}],
          [:put, "/inventory/#{pool_id}/templates/#{template_id}", {name: "", models: [{id: model_id, quantity: 0}]}],
          [:put, "/inventory/#{pool_id}/templates/#{template_id}", {name: "updated-name", models: [{id: model_id, quantity: -1}]}]
        ]

        endpoints.each do |method, path, body|
          resp = call(method, path, body: body)
          expect(resp.status).to eq(422),
            "Expected #{method.upcase} #{path} => #{expected_status}, got #{resp.status} (body: #{resp.body.inspect})"
        end
      end
    end
  end
end
