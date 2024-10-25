require "spec_helper"
require "pry"

shared_context :setup_models_api_min do
  before :each do
    @user = FactoryBot.create(:user, login: "test", password: "password")
    @inventory_pool = FactoryBot.create(:inventory_pool)
  end
end
