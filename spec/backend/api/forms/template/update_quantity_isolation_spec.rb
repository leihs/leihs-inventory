require "spec_helper"
require_relative "../../_shared"
require "faker"
require_relative "../_common"

describe "Template quantity isolation" do
  context "as inventory_manager" do
    include_context :setup_models_api_model, "inventory_manager"
    include_context :generate_session_header

    let(:cookie_header) { @cookie_header }
    let(:pool_id) { @inventory_pool.id }
    let(:shared_model_id) { @models.first.id }

    def create_template(name:, quantity:)
      json_client_post(
        "/inventory/#{pool_id}/templates/",
        body: {
          name: name,
          models: [{id: shared_model_id, quantity: quantity}]
        },
        headers: cookie_header
      )
    end

    it "removing a model from one template does not affect the same model in another template" do
      resp1 = create_template(name: "Template A", quantity: 5)
      expect(resp1.status).to eq(200)
      template1_id = resp1.body["id"]

      # add a second model so template A remains valid after removal
      second_model_id = @models.second.id
      update_resp = json_client_put(
        "/inventory/#{pool_id}/templates/#{template1_id}",
        body: {
          name: "Template A",
          models: [
            {id: shared_model_id, quantity: 5},
            {id: second_model_id, quantity: 2}
          ]
        },
        headers: cookie_header
      )
      expect(update_resp.status).to eq(200)

      resp2 = create_template(name: "Template B", quantity: 10)
      expect(resp2.status).to eq(200)
      template2_id = resp2.body["id"]

      # remove shared model from template A
      update_resp = json_client_put(
        "/inventory/#{pool_id}/templates/#{template1_id}",
        body: {
          name: "Template A",
          models: [{id: second_model_id, quantity: 2}]
        },
        headers: cookie_header
      )
      expect(update_resp.status).to eq(200)
      expect(update_resp.body["models"].map { |m| m["id"] }).not_to include(shared_model_id.to_s)

      # template B must still contain the shared model
      resp_b = plain_faraday_json_client(cookie_header).get("/inventory/#{pool_id}/templates/#{template2_id}")
      expect(resp_b.status).to eq(200)
      expect(resp_b.body["models"].first["id"]).to eq(shared_model_id.to_s)
      expect(resp_b.body["models"].first["quantity"]).to eq(10)
    end

    it "updating quantity in one template does not affect the same model in another template" do
      resp1 = create_template(name: "Template A", quantity: 5)
      expect(resp1.status).to eq(200)
      template1_id = resp1.body["id"]

      resp2 = create_template(name: "Template B", quantity: 10)
      expect(resp2.status).to eq(200)
      template2_id = resp2.body["id"]

      # update quantity in template A
      update_resp = json_client_put(
        "/inventory/#{pool_id}/templates/#{template1_id}",
        body: {
          name: "Template A",
          models: [{id: shared_model_id, quantity: 99}]
        },
        headers: cookie_header
      )
      expect(update_resp.status).to eq(200)
      expect(update_resp.body["models"].first["quantity"]).to eq(99)

      # template B quantity must remain unchanged
      resp_b = plain_faraday_json_client(cookie_header).get("/inventory/#{pool_id}/templates/#{template2_id}")
      expect(resp_b.status).to eq(200)
      expect(resp_b.body["models"].first["quantity"]).to eq(10)
    end
  end
end
