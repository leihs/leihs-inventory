def login(user)
  username = user.login || user.email

  visit "/inventory"
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

def assert_field(label, value)
  expect(find_field(label).value).to eq value
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
