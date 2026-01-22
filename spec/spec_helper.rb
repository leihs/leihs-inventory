require "active_support/all"
require "pry"

require_relative "../database/spec/config/database"
require "config/factories"

require "config/browser"
require "config/web"

require "helpers/global"
require "helpers/user"

# require "uuidtools"

RSpec.configure do |config|
  config.include Helpers::Global
  config.include Helpers::User

  config.before(type: :feature) do
    page.driver.browser.manage.window.resize_to(1280, 1200)
  end

  config.before(:example) do |example|
    srand 1
    db_clean
    db_restore_data seeds_sql
  end

  config.after(:example) do |example|
    # auto-pry after failures, except in CI!
    if !ENV["CIDER_CI_TRIAL_ID"].present? && ENV["PRY_ON_EXCEPTION"].present?
      unless example.exception.nil?
        binding.pry if example.exception
      end
    end
  end
end
