require "spec_helper"
require "pry"
require_relative "../backend/api/_shared"

feature "Sign-in / Sign-out" do
  context "works" do
    before :each do
      visit "/sign-in"
    end

    context "sign-in with invalid credentials" do
      scenario "user can fill out and submit the login form successfully" do
        visit "/sign-in"
        expect(page).to have_selector("form.ui-form-signin")

        fill_in "user", with: "testuser"
        fill_in "password", with: "password123"
        click_button "Continue"

        expect(page).to have_content("error: sign_in_wrong_password_flash_message")
        expect(page).to have_selector("form.ui-form-signin")
      end
    end

    context "sign-in/out with valid credentials" do
      include_context :setup_models_api_base

      scenario "user can fill out and submit the login form successfully" do
        visit "/sign-in"
        expect(page).to have_content("Leihs Simple Login")
        expect(page).to have_selector("form.ui-form-signin")

        fill_in "user", with: @user.login
        fill_in "password", with: @user.password
        click_button "Continue"
      end
    end
  end
end
