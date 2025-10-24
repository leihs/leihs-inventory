require "spec_helper"
require_relative "../../_shared"
require "faker"
require_relative "../_common"

def seed_models_items_templates(inventory_pool:, models: 1, items_per_model: 1, templates: 1)
  models_arr = Array.new(models) do
    FactoryBot.create(:leihs_model,
      id: SecureRandom.uuid,
      product: Faker::Commerce.product_name)
  end

  items_arr = models_arr.flat_map do |m|
    Array.new(items_per_model) do
      FactoryBot.create(:item,
        leihs_model: m,
        inventory_pool_id: inventory_pool.id,
        responsible: inventory_pool,
        is_borrowable: true)
    end
  end

  templates_arr = Array.new(templates) do
    FactoryBot.create(:template, inventory_pool: inventory_pool)
  end

  db = defined?(Sequel::Model) ? Sequel::Model.db : database

  model_links_ids =
    templates_arr.product(models_arr).each_with_index.map do |(t, m), i|
      db[:model_links].insert(
        model_group_id: t.id,
        model_id: m.id,
        quantity: i * 5
      )
    end

  OpenStruct.new(
    models: models_arr,
    items: items_arr,
    templates: templates_arr,
    model_links_ids: model_links_ids
  )
end

def expect_qty_availability(rows, expected, ignore_order: true)
  actual = rows.map do |r|
    [
      r[:quantity] || r["quantity"],
      r[:available] || r["available"],
      r[:is_quantity_ok] || r["is_quantity_ok"]
    ]
  end

  if ignore_order
    expect(actual).to match_array(expected)
  else
    expect(actual).to eq(expected)
  end
end

# ["inventory_manager", "lending_manager"].each do |role|
["inventory_manager"].each do |role|
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

    describe "fetching template with" do
      describe "invalid quantiy" do
        let!(:data) do
          seed_models_items_templates(
            inventory_pool: @inventory_pool,
            models: 2,
            items_per_model: 3,
            templates: 1
          )
        end
        let :response do
          client.get "/inventory/#{pool_id}/templates/"
        end

        it "returns all templates when no pagination is provided" do
          expect(response.status).to eq(200)
          expect(response.body.count).to eq(1)
          expect(response.body.first["models_count"]).to eq(2)
          expect(response.body.first["is_quantity_ok"]).to eq(false)
        end

        it "returns all templates when no pagination is provided" do
          resp = client.get "/inventory/#{pool_id}/templates/#{response.body.first["id"]}"

          expect(resp.status).to eq(200)
          expect_qty_availability(resp.body["models"], [[0, 3, true], [5, 3, false]])
        end
      end

      describe "valid quantiy" do
        let!(:data) do
          seed_models_items_templates(
            inventory_pool: @inventory_pool,
            models: 2,
            items_per_model: 10,
            templates: 1
          )
        end
        let :response do
          client.get "/inventory/#{pool_id}/templates/"
        end

        it "returns all templates when no pagination is provided" do
          expect(response.status).to eq(200)
          expect(response.body.count).to eq(1)
          expect(response.body.first["models_count"]).to eq(2)
          expect(response.body.first["is_quantity_ok"]).to eq(true)
        end

        it "returns all templates when no pagination is provided" do
          resp = client.get "/inventory/#{pool_id}/templates/#{response.body.first["id"]}"

          expect(resp.status).to eq(200)
          expect_qty_availability(resp.body["models"], [[0, 10, true], [5, 10, true]])
        end
      end

      describe "valid quantiy with no items" do
        let!(:data) do
          seed_models_items_templates(
            inventory_pool: @inventory_pool,
            models: 2,
            items_per_model: 0,
            templates: 1
          )
        end
        let :response do
          client.get "/inventory/#{pool_id}/templates/"
        end

        it "returns all templates when no pagination is provided" do
          expect(response.status).to eq(200)
          expect(response.body.count).to eq(1)
          expect(response.body.first["models_count"]).to eq(2)
          expect(response.body.first["is_quantity_ok"]).to eq(false)
        end

        it "returns all templates when no pagination is provided" do
          resp = client.get "/inventory/#{pool_id}/templates/#{response.body.first["id"]}"

          expect(resp.status).to eq(200)
          expect_qty_availability(resp.body["models"], [[0, 0, true], [5, 0, false]])
        end
      end

      describe "valid quantiy with no items" do
        let!(:data) do
          seed_models_items_templates(
            inventory_pool: @inventory_pool,
            models: 1,
            items_per_model: 0,
            templates: 1
          )
        end
        let :response do
          client.get "/inventory/#{pool_id}/templates/"
        end

        it "returns all templates when no pagination is provided" do
          expect(response.status).to eq(200)
          expect(response.body.count).to eq(1)
          expect(response.body.first["models_count"]).to eq(1)
          expect(response.body.first["is_quantity_ok"]).to eq(true)
        end

        it "returns all templates when no pagination is provided" do
          resp = client.get "/inventory/#{pool_id}/templates/#{response.body.first["id"]}"

          expect(resp.status).to eq(200)
          expect_qty_availability(resp.body["models"], [[0, 0, true]])
        end
      end
    end
  end
end
