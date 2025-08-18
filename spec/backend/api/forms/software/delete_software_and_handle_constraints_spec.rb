require "spec_helper"
require "pry"
require_relative "../../_shared"
require_relative "../_common"
require "faker"
require "marcel"

describe "Inventory Software" do
  ["inventory_manager"].each do |role|
    context "when interacting with inventory software with role=#{role}" do
      include_context :setup_models_api_model, role
      include_context :generate_session_header

      let(:pool_id) { @inventory_pool.id }
      let(:cookie_header) { @cookie_header }
      let(:client) { plain_faraday_json_client(cookie_header) }

      let(:manufacturer) {
        resp = client.get "/inventory/#{pool_id}/manufacturers/?type=Software"
        raise "Failed to fetch manufacturers" unless resp.status == 200
        resp.body.first
      }

      let(:form_data) {
        {
          "product" => Faker::Commerce.product_name,
          "version" => "v1.0",
          "manufacturer" => manufacturer,
          "technical_details" => "Specs go here"
        }
      }

      it "delete software without reservation" do
        resp = json_client_post(
          "/inventory/#{pool_id}/software/",
          body: form_data,
          headers: cookie_header
        )
        expect(resp.status).to eq(200)

        # fetch created software
        model_id = resp.body["id"]
        resp = client.get "/inventory/#{pool_id}/software/#{model_id}"
        expect(resp.status).to eq(200)
        expect(resp.body["is_deletable"]).to eq(true)

        # delete software request
        resp = json_client_delete(
          "/inventory/#{pool_id}/software/#{model_id}",
          headers: cookie_header
        )
        expect(resp.status).to eq(200)
        expect(resp.body["deleted_model"].count).to eq(1)
      end

      describe "with reservation & no order in status 'draft'" do
        before do
          resp = json_client_post(
            "/inventory/#{pool_id}/software/",
            body: form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(200)

          @model_id = resp.body["id"]
          m = LeihsModel.where(type: "Software", id: @model_id).first
          @order = FactoryBot.create(:reservation,
            user_id: @user.id,
            inventory_pool_id: @inventory_pool.id,
            leihs_model: m,
            status: :draft)
        end

        it "delete software without reservation" do
          resp = client.get "/inventory/#{pool_id}/software/#{@model_id}"
          expect(resp.status).to eq(200)
          expect(resp.body["is_deletable"]).to eq(false)

          resp = json_client_delete(
            "/inventory/#{pool_id}/software/#{@model_id}",
            headers: cookie_header
          )
          expect(resp.status).to eq(409)
        end
      end

      describe "with reservation & order in status 'submitted'" do
        before do
          resp = json_client_post(
            "/inventory/#{pool_id}/software/",
            body: form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(200)

          co = FactoryBot.create(:customer_order, user: @user)
          @order = FactoryBot.create(:order, inventory_pool: @inventory_pool, user: @user, customer_order: co, state: :submitted)

          @model_id = resp.body["id"]
          m = LeihsModel.where(type: "Software", id: @model_id).first
          @reservation = FactoryBot.create(:reservation,
            order: @order,
            user_id: @user.id,
            type: "ItemLine",
            inventory_pool_id: @inventory_pool.id,
            leihs_model: m,
            status: :submitted)
        end

        it "delete software without reservation" do
          resp = client.get "/inventory/#{pool_id}/software/#{@model_id}"
          expect(resp.status).to eq(200)
          expect(resp.body["is_deletable"]).to eq(false)

          resp = json_client_delete(
            "/inventory/#{pool_id}/software/#{@model_id}",
            headers: cookie_header
          )
          expect(resp.status).to eq(409)
        end
      end

      describe "with reservation & order in status 'approved'" do
        before do
          resp = json_client_post(
            "/inventory/#{pool_id}/software/",
            body: form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(200)

          co = FactoryBot.create(:customer_order, user: @user)
          @order = FactoryBot.create(:order, inventory_pool: @inventory_pool, user: @user, customer_order: co, state: :approved)

          @model_id = resp.body["id"]
          m = LeihsModel.where(type: "Software", id: @model_id).first
          @reservation = FactoryBot.create(:reservation,
            order: @order,
            user_id: @user.id,
            type: "ItemLine",
            inventory_pool_id: @inventory_pool.id,
            leihs_model: m,
            status: :approved)
        end

        it "delete software without reservation" do
          resp = client.get "/inventory/#{pool_id}/software/#{@model_id}"
          expect(resp.status).to eq(200)
          expect(resp.body["is_deletable"]).to eq(false)

          resp = json_client_delete(
            "/inventory/#{pool_id}/software/#{@model_id}",
            headers: cookie_header
          )
          expect(resp.status).to eq(409)
        end
      end

      describe "with reservation & order in status 'approved'" do
        before do
          resp = json_client_post(
            "/inventory/#{pool_id}/software/",
            body: form_data,
            headers: cookie_header
          )
          expect(resp.status).to eq(200)

          co = FactoryBot.create(:customer_order, user: @user)
          @order = FactoryBot.create(:order, inventory_pool: @inventory_pool, user: @user, customer_order: co,
            state: :approved)

          @model_id = resp.body["id"]
          m = LeihsModel.where(type: "Software", id: @model_id).first
          @reservation = FactoryBot.create(:reservation,
            order: @order,
            user_id: @user.id,
            type: "ItemLine",
            inventory_pool_id: @inventory_pool.id,
            leihs_model: m,
            status: :approved)
        end

        it "delete software without reservation" do
          resp = client.get "/inventory/#{pool_id}/software/#{@model_id}"
          expect(resp.status).to eq(200)
          expect(resp.body["is_deletable"]).to eq(false)

          resp = json_client_delete(
            "/inventory/#{pool_id}/software/#{@model_id}",
            headers: cookie_header
          )
          expect(resp.status).to eq(409)
        end
      end
    end
  end
end
