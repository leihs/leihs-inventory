def login(user)
  username = user.login || user.email

  visit "/inventory"
  fill_in("user", with: username)
  fill_in("password", with: user.password)
  click_on("Continue")
end
