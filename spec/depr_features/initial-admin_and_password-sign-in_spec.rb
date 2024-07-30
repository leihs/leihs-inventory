require 'spec_helper'
require 'pry'

feature 'Initial admin and password sign-in', type: :feature do
  it 'works' do
    visit '/admin/initial-admin'
    # In the integrated app, `my` would redirect here if no admin exists in db.
    # (there is also an integration test which tests this)

    expect(page).to have_content  "Initial Admin"

    # we create the initial admin
    within('#initial-admin-form') do
      fill_in 'email', with: 'admin@example.com'
      fill_in 'password', with: 'password'
      click_on 'Create'
    end

    expect(page).to have_content "Request OK"

    # we sign-in as the admin
    within('.navbar-leihs form.ui-form-signin', match: :first) do
      fill_in 'user', with: 'admin@example.com'
      click_button
    end

    within('form.ui-form-signin') do
      fill_in 'password', with: 'password'
      click_button
    end

    # we are signed-in
    expect(page).to have_content 'Users'
    click_on 'Users'
    expect(page).to have_content 'admin@example.com'

    # we are still signed-in when we reload the page
    visit current_path
    expect(page).to have_content 'admin@example.com'

    # sign-out
    find('.fa-user-circle').click
    click_on 'Logout'
    expect(page).not_to have_content 'admin@example.com'

    # we are still signed-out when we reload the page
    visit current_path
    expect(page).not_to have_content 'admin@example.com'
  end

  it 'is forbidden when an admin exists', type: :feature do
    FactoryBot.create :admin
    visit '/admin/initial-admin'
    expect(page).to have_content  "Initial Admin"
    
    within('#initial-admin-form') do
      fill_in 'email', with: 'admin@example.com'
      fill_in 'password', with: 'password'
      click_on 'Create'
    end

    expect(current_path).to eq "/admin/initial-admin"
    expect(page).to have_content "An admin user already exists!"
    expect(User.count).to eq 1
  end
end
