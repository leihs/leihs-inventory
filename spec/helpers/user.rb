module Helpers
  module User
    extend self

    def sign_in_as user
      visit "/"
      fill_in "user", with: user.email
      click_on "Login"
      fill_in "password", with: user.password
      click_on "Continue"
      find(".fa-user-circle").click
      expect(page).to have_content user.lastname
      visit "/admin/"
    end
  end
end
