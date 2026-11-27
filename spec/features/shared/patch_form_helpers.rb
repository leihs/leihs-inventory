def patch_value_name(index)
  "update.#{index}.value"
end

def add_patch_field(index, label)
  dismiss_open_menus
  within find('[id="patch-item-form"]') do
    find(:button, text: /Add field/i, match: :first).click if index > 0
    click_on "field-select-#{index}"
  end
  within find('[data-test-id="field-options"]', wait: 10) do
    find("span", text: label, match: :first).click
  end
  expect(page).not_to have_css('[data-test-id="field-options"]', wait: 5)
  expect(find("[data-test-id='field-select-#{index}']", wait: 10)).to have_text(label)
end

def fill_patch_text(index, value)
  name = patch_value_name(index)
  within find('[id="patch-item-form"]') do
    field = find("textarea[name='#{name}'], input[name='#{name}']", visible: :all, wait: 10)
    field.set("")
    field.set(value)
  end
end

def fill_patch_price(index, value)
  name = patch_value_name(index)
  within find('[id="patch-item-form"]') do
    field = find("input[name='#{name}']", wait: 5)
    field.set(value)
    field.send_keys(:tab)
    sleep 0.1
  end
end

def click_patch_radio(index, value)
  find(%([data-test-id="#{patch_value_name(index)}-#{value}"]), visible: :all, wait: 5).click
end

def fill_patch_select(index, option_label)
  name = patch_value_name(index)
  dismiss_open_menus
  find("button[name='#{name}']", visible: :all, wait: 5).click
  within find("[data-test-id='#{name}-options']", wait: 10) do
    find(:button, text: option_label, match: :first).click
  end
  expect(page).not_to have_css("[data-test-id='#{name}-options']", wait: 5)
end

def fill_patch_autocomplete(index, search_term, option_text)
  name = patch_value_name(index)
  dismiss_open_menus
  find("button[data-test-id='#{name}']", wait: 10).click
  input = find("input[data-test-id='#{name}-input'], input[placeholder='Enter search term']", visible: :all, wait: 10)
  input.set(search_term)
  await_debounce
  find(:button, text: option_text, match: :first, wait: 10).click
  dismiss_open_menus
  expect(find("button[data-test-id='#{name}']", wait: 10)).to have_text(option_text)
end

def fill_patch_calendar(index, date)
  name = patch_value_name(index)
  dismiss_open_menus
  find("button[name='#{name}']", visible: :all, wait: 10).click
  expect(page).to have_css("button[data-day]", wait: 10)
  click_calendar_day(date, dismiss: true)
  expect(find("button[name='#{name}']", wait: 10)).to have_text(date.strftime("%d/%m/%Y"))
end
