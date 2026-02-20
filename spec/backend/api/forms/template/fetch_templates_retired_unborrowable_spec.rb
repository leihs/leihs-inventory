require "spec_helper"
require_relative "../../_shared"
require "faker"
require_relative "../_common"

["inventory_manager", "lending_manager"].each do |role|
  context "when interacting with inventory templates as inventory_manager" do
    include_context :setup_api, role
    include_context :generate_session_header

    let(:cookie_header) { @cookie_header }
    let(:client) { plain_faraday_json_client(cookie_header) }
    let(:pool_id) { @inventory_pool.id }
    let(:model_id) { @models.first.id }

    def create_template(quantity:)
      json_client_post(
        "/inventory/#{pool_id}/templates/",
        body: {
          name: Faker::Commerce.product_name,
          models: [{quantity: quantity, id: @models.second.id}]
        },
        headers: cookie_header
      )
    end

    describe "fetching templates, none existing" do
      it "returns all templates when no pagination is provided" do
        resp = client.get "/inventory/#{pool_id}/templates/"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(0)
      end

      it "returns all models used by model-selection" do
        resp = client.get "/inventory/#{pool_id}/list/?borrowable=true&type=model&retired=false"
        expect(resp.status).to eq(200)
        expect(resp.body.count).to eq(0)
      end
    end

    describe "fetching template linked with model without item" do
      let :model do
        FactoryBot.create(:leihs_model,
          id: SecureRandom.uuid,
          product: Faker::Commerce.product_name)
      end

      let! :template do
        FactoryBot.create(:template, inventory_pool: @inventory_pool, direct_models: [model])
      end

      describe "fetching templates" do
        let :response do
          client.get "/inventory/#{pool_id}/templates/"
        end

        it "returns all templates when no pagination is provided" do
          expect(response.status).to eq(200)

          expect(response.body.count).to eq(1)
          expect(response.body.first["models_count"]).to eq(1)
        end

        it "returns all templates when no pagination is provided" do
          resp = client.get "/inventory/#{pool_id}/templates/#{response.body.first["id"]}"

          expect(resp.status).to eq(200)

          expect(resp.body["models"].count).to eq(1)
          expect(resp.body["models"].first["quantity"]).to eq(1)
          expect(resp.body["models"].first["available_quantity"]).to eq(0)
          expect(resp.body["models"].first["is_quantity_ok"]).to eq(false)
        end
      end
    end

    describe "fetching template linked with model with" do
      let :model do
        FactoryBot.create(:leihs_model,
          id: SecureRandom.uuid,
          product: Faker::Commerce.product_name)
      end

      let! :item do
        FactoryBot.create(:item, leihs_model: model, inventory_pool_id: @inventory_pool.id,
          responsible: @inventory_pool, is_borrowable: true)
      end

      let! :template do
        FactoryBot.create(:template, inventory_pool: @inventory_pool, direct_models: [model])
      end

      describe "item is not borrowable" do
        let :response do
          client.get "/inventory/#{pool_id}/templates/"
        end

        before :each do
          item.is_borrowable = false
          item.save_changes
        end

        it "returns all templates when no pagination is provided" do
          expect(response.status).to eq(200)

          expect(response.body.count).to eq(1)
          expect(response.body.first["models_count"]).to eq(1)
        end

        it "returns all templates when no pagination is provided" do
          resp = client.get "/inventory/#{pool_id}/templates/#{response.body.first["id"]}"

          expect(resp.status).to eq(200)

          expect(resp.body["models"].count).to eq(1)
          expect(resp.body["models"].first["quantity"]).to eq(1)
          expect(resp.body["models"].first["available_quantity"]).to eq(0)
          expect(resp.body["models"].first["is_quantity_ok"]).to eq(false)
        end

        it "returns all templates when no pagination is provided" do
          resp = client.get "/inventory/#{pool_id}/templates/#{response.body.first["id"]}"

          expect(resp.status).to eq(200)

          expect(resp.body["models"].count).to eq(1)
          expect(resp.body["models"].first["quantity"]).to eq(1)
          expect(resp.body["models"].first["available_quantity"]).to eq(0)
          expect(resp.body["models"].first["is_quantity_ok"]).to eq(false)
        end
      end

      describe "item is retired" do
        let :response do
          client.get "/inventory/#{pool_id}/templates/"
        end

        before :each do
          item.retired = Date.yesterday
          item.save_changes
        end

        it "returns all templates when no pagination is provided" do
          expect(response.status).to eq(200)
          expect(response.body.count).to eq(1)
          expect(response.body.first["models_count"]).to eq(1)
          expect(response.body.first["is_quantity_ok"]).to eq(false)
        end

        it "returns all templates when no pagination is provided" do
          resp = client.get "/inventory/#{pool_id}/templates/#{response.body.first["id"]}"

          expect(resp.status).to eq(200)

          expect(resp.body["models"].count).to eq(1)
          expect(resp.body["models"].first["quantity"]).to eq(1)
          expect(resp.body["models"].first["available_quantity"]).to eq(0)
          expect(resp.body["models"].first["is_quantity_ok"]).to eq(false)
        end

        it "returns all templates when no pagination is provided" do
          resp = client.get "/inventory/#{pool_id}/templates/#{response.body.first["id"]}"

          expect(resp.status).to eq(200)

          expect(resp.body["models"].count).to eq(1)
          expect(resp.body["models"].first["quantity"]).to eq(1)
          expect(resp.body["models"].first["available_quantity"]).to eq(0)
          expect(resp.body["models"].first["is_quantity_ok"]).to eq(false)
        end
      end
    end
  end
end
