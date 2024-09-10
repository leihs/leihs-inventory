require "capybara/rspec"
require "selenium-webdriver"

BROWSER_DOWNLOAD_DIR = File.absolute_path(File.expand_path(__FILE__) + "/../../../tmp")

def http_port
  @port ||= Integer(ENV["LEIHS_INVENTORY_HTTP_PORT"].presence || 3260)
end

def http_host
  @host ||= ENV["LEIHS_INVENTORY_HTTP_HOST"].presence || "localhost"
end

def http_base_url
  @http_base_url ||= "http://#{http_host}:#{http_port}"
end

def set_capybara_values
  Capybara.app_host = http_base_url
  Capybara.server_port = http_port
  Capybara.test_id = "data-test-id"
end

firefox_bin_path = Pathname.new(`asdf where firefox`.strip).join("bin/firefox").expand_path.to_s
Selenium::WebDriver::Firefox.path = firefox_bin_path

Capybara.register_driver :firefox do |app|
  options = Selenium::WebDriver::Firefox::Options.new(
    binary: firefox_bin_path,
    log_level: :trace
  )

  # Accept insecure certificates
  options.accept_insecure_certs = true

  # Create a new profile with preferences
  profile = Selenium::WebDriver::Firefox::Profile.new
  profile["browser.helperApps.neverAsk.saveToDisk"] = "image/jpeg,application/pdf,application/json"
  profile["browser.download.folderList"] = 2 # Custom location
  profile["browser.download.dir"] = BROWSER_DOWNLOAD_DIR.to_s

  options.profile = profile

  # NOTE: good for local dev
  options.args << "--headless" if ENV["LEIHS_TEST_HEADLESS"].present?
  # Uncomment the line below for debugging with devtools
  # options.args << '--devtools'

  Capybara::Selenium::Driver.new(
    app,
    browser: :firefox,
    options: options
  )
end

RSpec.configure do |config|
  set_capybara_values

  # Capybara.run_server = false
  Capybara.default_driver = :firefox
  Capybara.current_driver = :firefox

  config.before :all do
    set_capybara_values
  end

  config.before :each do |example|
    set_capybara_values
  end

  config.after(:each) do |example|
    unless example.exception.nil?
      take_screenshot screenshot_dir
    end
  end

  config.before :all do
    FileUtils.remove_dir(screenshot_dir, force: true)
    FileUtils.mkdir_p(screenshot_dir)
  end

  def screenshot_dir
    Pathname(BROWSER_DOWNLOAD_DIR).join("screenshots")
  end

  def take_screenshot(screenshot_dir = nil, name = nil)
    name ||= "#{Time.now.iso8601.tr(":", "-")}.png"
    path = screenshot_dir.join(name)
    case Capybara.current_driver
    when :firefox
      begin
        page.driver.browser.save_screenshot(path)
      rescue
        nil
      end
    else
      Logger.warn "Taking screenshots is not implemented for \
              #{Capybara.current_driver}."
    end
  end
end
