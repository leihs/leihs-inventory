require "spec_helper"
require "pry"
require_relative "../../_shared"
require_relative "../_common"
require "faker"
require "marcel"

describe "Inventory option" do
  ["inventory_manager"].each do |role|
    context "when interacting with inventory option with role=#{role}" do
      include_context :setup_models_api_model, role
      include_context :generate_session_header

      let(:pool_id) { @inventory_pool.id }
      let(:cookie_header) { @cookie_header }
      let(:client) { plain_faraday_json_client(cookie_header) }

      let(:form_data) {
        {
          "product" => Faker::Commerce.product_name,
          "version" => "v1.0",
          "inventory_code" => Faker::Commerce.promotion_code,
          "price" => 12.33
        }
      }

      it "delete option without reservation" do
        resp = json_client_post(
          "/inventory/#{pool_id}/options/",
          body: form_data,
          headers: cookie_header
        )
        expect(resp.status).to eq(200)

        # fetch created option
        model_id = resp.body["id"]
        resp = client.get "/inventory/#{pool_id}/options/#{model_id}"
        expect(resp.status).to eq(200)
        expect(resp.body["is_deletable"]).to eq(true)

        # delete option request
        resp = json_client_delete(
          "/inventory/#{pool_id}/options/#{model_id}",
          headers: cookie_header
        )
        expect(resp.status).to eq(200)
        expect(resp.body.is_a?(Hash)).to eq(true)
      end

      describe "with reservation & no order in status 'draft'" do
        before do
          resp = json_client_post(
            "/inventory/#{pool_id}/options/",
            body: form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(200)

          @model_id = resp.body["id"]
          m = Option.where(id: @model_id).first
          @order = FactoryBot.create(:reservation,
            user_id: @user.id,
            inventory_pool_id: @inventory_pool.id,
            option: m,
            status: :draft)
        end

        it "delete option without reservation" do
          resp = client.get "/inventory/#{pool_id}/options/#{@model_id}"
          expect(resp.status).to eq(200)
          expect(resp.body["is_deletable"]).to eq(false)

          resp = json_client_delete(
            "/inventory/#{pool_id}/options/#{@model_id}",
            headers: cookie_header
          )
          expect(resp.status).to eq(409)
        end
      end

      describe "with reservation & order in status 'submitted'" do
        before do
          resp = json_client_post(
            "/inventory/#{pool_id}/options/",
            body: form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(200)

          co = FactoryBot.create(:customer_order, user: @user)
          @order = FactoryBot.create(:order, inventory_pool: @inventory_pool, user: @user, customer_order: co, state: :submitted)

          @model_id = resp.body["id"]
          m = Option.where(id: @model_id).first
          @reservation = FactoryBot.create(:reservation,
            order: @order,
            user_id: @user.id,
            type: "ItemLine",
            inventory_pool_id: @inventory_pool.id,
            option: m,
            status: :submitted)
        end

        it "delete option without reservation" do
          resp = client.get "/inventory/#{pool_id}/options/#{@model_id}"
          expect(resp.status).to eq(200)
          expect(resp.body["is_deletable"]).to eq(false)

          resp = json_client_delete(
            "/inventory/#{pool_id}/options/#{@model_id}",
            headers: cookie_header
          )
          expect(resp.status).to eq(409)
        end
      end

      describe "with reservation & order in status 'approved'" do
        before do
          resp = json_client_post(
            "/inventory/#{pool_id}/options/",
            body: form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(200)

          co = FactoryBot.create(:customer_order, user: @user)
          @order = FactoryBot.create(:order, inventory_pool: @inventory_pool, user: @user, customer_order: co, state: :approved)

          @model_id = resp.body["id"]
          m = Option.where(id: @model_id).first
          @reservation = FactoryBot.create(:reservation,
            order: @order,
            user_id: @user.id,
            type: "ItemLine",
            inventory_pool_id: @inventory_pool.id,
            option: m,
            status: :approved)
        end

        it "delete option without reservation" do
          resp = client.get "/inventory/#{pool_id}/options/#{@model_id}"
          expect(resp.status).to eq(200)
          expect(resp.body["is_deletable"]).to eq(false)

          resp = json_client_delete(
            "/inventory/#{pool_id}/options/#{@model_id}",
            headers: cookie_header
          )
          expect(resp.status).to eq(409)
        end
      end

      describe "with reservation & order in status 'approved'" do
        before do
          resp = json_client_post(
            "/inventory/#{pool_id}/options/",
            body: form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(200)

          co = FactoryBot.create(:customer_order, user: @user)
          @order = FactoryBot.create(:order, inventory_pool: @inventory_pool, user: @user, customer_order: co,
            state: :approved)

          @model_id = resp.body["id"]
          m = Option.where(id: @model_id).first
          @reservation = FactoryBot.create(:reservation,
            order: @order,
            user_id: @user.id,
            type: "ItemLine",
            inventory_pool_id: @inventory_pool.id,
            option: m,
            status: :approved)
        end

        it "delete option without reservation" do
          resp = client.get "/inventory/#{pool_id}/options/#{@model_id}"
          expect(resp.status).to eq(200)
          expect(resp.body["is_deletable"]).to eq(false)

          resp = json_client_delete(
            "/inventory/#{pool_id}/options/#{@model_id}",
            headers: cookie_header
          )
          expect(resp.status).to eq(409)
        end
      end
    end
  end
end
