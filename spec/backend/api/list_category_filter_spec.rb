require "spec_helper"
require_relative "_shared"

describe "GET /inventory/:pool_id/list/ filtered by category_id" do
  include_context :setup_models_min_api

  before :each do
    @user, @user_cookies, = create_and_login(:user)
    FactoryBot.create(:access_right,
      inventory_pool_id: @inventory_pool.id,
      user_id: @user.id,
      role: "inventory_manager")
  end

  let(:client) { session_auth_plain_faraday_json_client(cookies: @user_cookies) }

  def model
    FactoryBot.create(:leihs_model, type: "Model", is_package: false)
  end

  def model_ids(category_id)
    resp = client.get "/inventory/#{@inventory_pool.id}/list/?category_id=#{category_id}"
    expect(resp.status).to eq(200)
    resp.body.map { |m| m["id"] }.sort
  end

  it "includes the category and its descendants, excludes ancestors and unrelated" do
    root_model = model
    child_model = model
    grandchild_model = model
    unrelated_model = model

    grandchild = FactoryBot.create(:category, direct_models: [grandchild_model])
    child = FactoryBot.create(:category,
      direct_models: [child_model],
      children: [grandchild])
    FactoryBot.create(:category, direct_models: [root_model], children: [child])
    FactoryBot.create(:category, direct_models: [unrelated_model])

    expect(model_ids(child.id))
      .to eq([child_model.id, grandchild_model.id].sort)
  end

  it "returns a model linked to the category and a descendant once" do
    m = model
    child = FactoryBot.create(:category, direct_models: [m])
    parent = FactoryBot.create(:category, direct_models: [m], children: [child])

    expect(model_ids(parent.id)).to eq([m.id])
  end

  it "terminates on cyclic category links" do
    a_model = model
    b_model = model
    a = FactoryBot.create(:category, direct_models: [a_model])
    b = FactoryBot.create(:category, direct_models: [b_model], parents: [a])
    a.add_parent(b)

    expect(model_ids(a.id)).to eq([a_model.id, b_model.id].sort)
  end
end
