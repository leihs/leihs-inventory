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

def attach_file_by_label(label_text, file_path)
  within find("label", text: label_text).find(:xpath, "..") do
    file_input = first("input[type='file']", minimum: 0, visible: :all)
    if file_input
      file_input.attach_file file_path
    else
      raise "No file input found for label '#{label_text}'"
    end
  end
end
