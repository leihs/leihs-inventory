def login(user)
  username = user.login || user.email

  visit "/sign-in"
  fill_in("user", with: username)
  fill_in("password", with: user.password)
  click_on("Continue")
end

def assert_checked(el)
  expect(el["data-state"]).to eq "checked"
end

def assert_unchecked(el)
  expect(el["data-state"]).to eq "unchecked"
end

def await_debounce
  sleep 0.3
end

def assert_field(label, value)
  expect(find_field(label, wait: 10).value).to eq value
end

def assert_button(name, value)
  button = find("button[name='#{name}']", wait: 10)
  expect(button).to have_text(value)
end

def fill_in_command_field(placeholder, value)
  find("input[placeholder='#{placeholder}']").set value
end

def select_value(name, value)
  filter = find("button[name='#{name}']")
  filter.click
  expect(page).to have_css("[data-test-id='#{value}']", wait: 10)
  find("div[data-test-id='#{value}']", match: :first).click
end

def click_calendar_day(date)
  find('[class*="rdp-button_previous"]').click unless date.month == Date.today.month && date.year == Date.today.year
  find("[data-day='#{date.strftime("%-m/%-d/%Y")}']").click
end

def attach_file_by_label(label_text, file_path)
  within find("label", text: label_text).find(:xpath, "..") do
    file_input = first("input[type='file']", minimum: 0, visible: :all)
    raise "No file input found for label '#{label_text}'" unless file_input

    file_input.attach_file file_path
  end
end

# Status filter uses Radix submenus in a portal; use the menu + button[name=…] so we do not
# click the wrong control (e.g. another "Yes" or a stale match).
STATUS_SUBMENU_BUTTON_NAME = {
  "Owned" => "owned",
  "In stock" => "in_stock",
  "Broken" => "broken",
  "Incomplete" => "incomplete"
}.freeze

def wait_for_download(filename, timeout: 15)
  path = File.join(BROWSER_DOWNLOAD_DIR, filename)
  Timeout.timeout(timeout) do
    sleep 0.2 until File.exist?(path) && File.size(path) > 0
  end
  path
rescue Timeout::Error
  nil
end

def cleanup_download(filename)
  path = File.join(BROWSER_DOWNLOAD_DIR, filename)
  FileUtils.rm_f(path)
end

def select_status_filter_submenu(submenu_label, yes_or_no)
  click_on "Status"
  # Radix mounts several [role=menu] portals; pick the Status menu (only it has an "Owned" row).
  menu = find(:xpath, "//*[@role='menu'][.//button[normalize-space()='Owned']]", wait: 10)
  menu.find(:button, submenu_label, match: :first).click
  param = STATUS_SUBMENU_BUTTON_NAME.fetch(submenu_label)
  find("button[name='#{param}']", text: yes_or_no, wait: 10).click
end
