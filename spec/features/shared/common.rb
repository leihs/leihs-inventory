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
  page.has_css?('[aria-busy="true"]', wait: 0.5)
  expect(page).not_to have_css('[aria-busy="true"]', wait: 10)
end

def search_in_list(product)
  unless find("input[name='search']").value == product
    fill_in "search", with: product
    await_debounce
  end
end

def await_list_loaded
  expect(page).to have_css('[aria-busy="true"]')
  expect(page).not_to have_css('[aria-busy="true"]')
end

def dismiss_open_menus
  page.send_keys(:escape)
  page.send_keys(:escape)
  sleep 0.1
end

def create_timeline_list_items(pool:, room:, model:, package_model:, software_model:)
  FactoryBot.create(:item,
    inventory_code: "#{pool.shortname}001",
    owner_id: pool.id,
    inventory_pool_id: pool.id,
    leihs_model: model,
    room: room,
    shelf: "S-01",
    is_borrowable: true,
    retired: nil)

  FactoryBot.create(:item,
    inventory_code: "#{pool.shortname}002",
    owner_id: pool.id,
    inventory_pool_id: pool.id,
    leihs_model: package_model,
    room: room,
    shelf: "S-02",
    is_borrowable: true,
    retired: nil)

  FactoryBot.create(:item,
    inventory_code: "#{pool.shortname}003",
    owner_id: pool.id,
    inventory_pool_id: pool.id,
    leihs_model: software_model,
    room: room,
    is_borrowable: true,
    retired: nil)
end

def assert_field(label, value)
  expect(find_field(label, wait: 10).value).to eq value
end

def format_price_display(price)
  integer_part, decimal_part = price.to_s.split(".")
  formatted_integer = integer_part.reverse.scan(/.{1,3}/).join(",").reverse
  frac = decimal_part ? decimal_part.ljust(2, "0")[0, 2] : "00"
  "#{formatted_integer}.#{frac}"
end

def assert_button(name, value)
  button = find("button[name='#{name}']", wait: 10)
  expect(button).to have_text(value)
end

def fill_in_command_field(placeholder, value)
  find("input[placeholder='#{placeholder}']").set value
end

def select_value(name, value)
  find("button[name='#{name}']").click
  option = find("[role='listbox'] [data-test-id='#{value}']", visible: true, wait: 10)
  option.click
end

def parse_calendar_data_day(value)
  return nil if value.to_s.strip.empty?

  Date.iso8601(value.to_s)
rescue Date::Error
  if (match = value.to_s.match(%r{\A(\d{1,2})/(\d{1,2})/(\d{4})\z}))
    first, second, year = match.captures.map(&:to_i)
    parse_calendar_date_parts(year, first, second) ||
      parse_calendar_date_parts(year, second, first)
  end
rescue ArgumentError
  nil
end

def parse_calendar_date_parts(year, month, day)
  Date.new(year, month, day)
rescue ArgumentError
  nil
end

def visible_calendar_month
  months = all("[data-day]", visible: true, wait: 5).filter_map do |button|
    date = parse_calendar_data_day(button["data-day"])
    Date.new(date.year, date.month, 1) if date
  end
  months.group_by(&:itself).max_by { |_month, dates| dates.size }&.first
end

def calendar_day_selector(date)
  [
    "[data-day='#{date.iso8601}']",
    "[data-day='#{date.strftime("%-m/%-d/%Y")}']",
    "[data-day='#{date.strftime("%m/%d/%Y")}']",
    "[data-day='#{date.strftime("%-d/%-m/%Y")}']",
    "[data-day='#{date.strftime("%d/%m/%Y")}']"
  ].join(", ")
end

def navigate_calendar_to_month(date)
  target_month = Date.new(date.year, date.month, 1)

  12.times do
    month = visible_calendar_month
    break if month.nil? || month == target_month

    direction = (target_month < month) ? "previous" : "next"
    find("[class*='rdp-button_#{direction}']", wait: 5).click
    sleep 0.1
  end
end

def click_calendar_day(date, dismiss: true)
  navigate_calendar_to_month(date)

  buttons = all(calendar_day_selector(date), minimum: 1, wait: 10)
  button = buttons.find { |el| el.text.strip == date.day.to_s } || buttons.first
  button.click
  dismiss_open_menus if dismiss
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
