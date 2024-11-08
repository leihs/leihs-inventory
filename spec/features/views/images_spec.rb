require "spec_helper"
require "pry"
require_relative "../../features/api/_shared"

feature "Request" do
  context "with accept=text/html" do

    # # FIXME
    # context "sign-in with invalid credentials" do
    #   scenario "user can fill out and submit the login form successfully" do
    #     visit "/inventory/images/fe8e42a0-1d31-4449-8c91-24c17ddd5d10"
    #
    #     expect(page).to have_content("error: sign_in_wrong_password_flash_message")
    #     expect(page).to have_selector("form.ui-form-signin")
    #   end
    # end
    #
    # # FIXME
    # context "sign-in with invalid credentials" do
    #   scenario "user can fill out and submit the login form successfully" do
    #     visit "/inventory/images/fe8e42a0-1d31-4449-8c91-24c17ddd5d10/thumbnail"
    #
    #     expect(page).to have_content("error: sign_in_wrong_password_flash_message")
    #     expect(page).to have_selector("form.ui-form-signin")
    #   end
    # end

  end
end

