require "spec_helper"
require_relative "../_shared"

RSpec.shared_examples "read endpoint access" do |role:, path_proc:, body_assertion:|
  include_context :setup_api, role
  include_context :generate_session_header

  let(:client) { plain_faraday_json_client(@cookie_header) }
  let(:pool_id) { @inventory_pool.id }

  it "allows GET read endpoint" do
    resp = client.get(instance_exec(&path_proc))
    expect(resp.status).to eq(200)
    instance_exec(resp.body, &body_assertion)
  end

  if role == "group_manager"
    it "still denies GET templates" do
      resp = client.get "/inventory/#{pool_id}/templates/"
      expect(resp.status).to eq(403)
    end
  end
end
