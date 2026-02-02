require "spec_helper"
require "config/browser"

RSpec.configure do |config|
  config.before(type: :feature) do
    page.driver.browser.manage.window.resize_to(1280, 1200)
  end
end
