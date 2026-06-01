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

def expect_group_manager_timeline_on_model_row(pool:, model:, product_label: nil)
  label = product_label || model.name
  within find('[data-row="model"]', text: label, wait: 10) do
    timeline_link = find('[data-test-id="timeline-button"]')
    expect(timeline_link[:href]).to end_with(
      "/manage/#{pool.id}/models/#{model.id}/timeline"
    )
    expect(timeline_link[:target]).to eq("_blank")
    expect(page).not_to have_css('[data-test-id="edit-dropdown"]')
  end
end

def expect_no_timeline_on_model_row(product_label:)
  expect(page).to have_css('[data-row="model"]', text: product_label, wait: 10)
  within find('[data-row="model"]', text: product_label) do
    expect(page).not_to have_css('[data-test-id="timeline-button"]')
  end
end

def expect_manager_option_row_without_timeline(product_label:)
  dismiss_open_menus
  expect(page).to have_css('[data-row="model"]', text: product_label, wait: 10)
  within find('[data-row="model"]', text: product_label) do
    find('[data-test-id="edit-dropdown"]').click
  end
  expect(page).not_to have_css('[data-test-id="timeline-menu-item"]')
  dismiss_open_menus
end

def expect_manager_timeline_in_edit_dropdown(pool:, model:, product_label: nil)
  dismiss_open_menus
  label = product_label || model.name
  expect(page).to have_css('[data-row="model"]', text: label, wait: 10)
  within find('[data-row="model"]', text: label) do
    expect(page).to have_css('[data-test-id="edit-dropdown"]')
    find('[data-test-id="edit-dropdown"]').click
  end
  timeline_link = find('[data-test-id="timeline-menu-item"]', visible: true, wait: 10)
  expect(timeline_link[:href]).to end_with(
    "/manage/#{pool.id}/models/#{model.id}/timeline"
  )
  expect(timeline_link[:target]).to eq("_blank")
  dismiss_open_menus
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
  find("button[name='#{name}']").click
  option = find("[role='listbox'] [data-test-id='#{value}']", visible: true, wait: 10)
  option.click
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
