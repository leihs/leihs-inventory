require 'spec_helper'
require 'pry'
require "#{File.dirname(__FILE__)}/_shared"


shared_examples :create_password_reset_link_via_ui do
  include_context :sign_in_to_admin
  scenario "Creating a password reset link for target_user via UI works" do
    click_on 'Users'
    click_on_first_user target_user
    click_on 'Reset Password'
    # select '3 days', from: 'Link valid for'
    click_on 'Create Reset Link - 3 days'
    # click_on 'Create'
    # there is an "send pass word reset link via e-mail" button
    expect(page).to have_content  'Send per Mail'
    within('.action.modal .modal-body'){ expect(find('svg'))}
    token = find_field('reset-token', disabled: true).value
    user_password_reset = database[:user_password_resets].where(token: token).first
    expect( user_password_reset ).to be
    # it belongs to the user
    expect( user_password_reset[:user_id] ).to be== target_user[:id]
    # it is valid for about 3 days
    expect( user_password_reset[:valid_until] ).to be>= Time.now + 3.days - 1.hour
    expect( user_password_reset[:valid_until] ).to be<= Time.now + 3.days + 1.hour
  end
end

shared_examples :password_reset_page_shows_warning do
  include_context :sign_in_to_admin
  scenario 'The password reset link page shows an warning' do
    click_on 'Users'
    click_on_first_user target_user
    click_on 'Reset Password'
    click_on 'Create Reset Link - 1 hour'
    expect(page).to have_content "Creating a Password Reset Link Is Not Possible"
  end
end


shared_examples :create_password_reset_link_via_api do
  include_context :setup_api
  scenario "Creating a password reset link for target_user via API works" do
    url = "/admin/users/#{target_user[:id]}/password-reset"
    payload = {valid_for_hours: 48}
    resp = @http_client.post url, payload.to_json
    expect(resp.status).to be== 201
    expect(resp.body['token']).to be
    expect(resp.body['user_id']).to be== target_user[:id]
    expect( DateTime.parse(resp.body['valid_until']) ).to be>= Time.now + 2.days - 1.hour
    expect( DateTime.parse(resp.body['valid_until']) ).to be<= Time.now + 2.days + 1.hour
  end
end


shared_examples :create_password_reset_link_via_api_is_forbidden do
  include_context :setup_api
  scenario "Creating a password reset link for target_user via API is forbidden" do
    url = "/admin/users/#{target_user[:id]}/password-reset"
    payload = {valid_for_hours: 48}
    resp = @http_client.post url, payload.to_json
    expect(resp.status).to be== 403
  end
end


feature "Password Reset Link" do

  before :each do
    database[:system_and_security_settings].where(id: 0).update(external_base_url: @http_base_url)
  end

  context 'all types of useres exist,' do
    include_context :all_types_of_users

    context 'as an admin' do
      before(:each){ @current_user = @admin }
      context 'set target user as regular user' do
        let (:target_user) {@user}

        context 'the target users password sign in is disabled' do
          before :each do
            database[:users].where(id: @user.id).update(password_sign_in_enabled: false)
          end
          include_examples :password_reset_page_shows_warning
        end
        context 'the targer user has no email-address' do
          before :each do
            database[:users].where(id: @user.id).update(email: nil)
          end
          include_examples :password_reset_page_shows_warning
        end

        context do
          include_examples :create_password_reset_link_via_api
          include_examples :create_password_reset_link_via_ui
        end

      end

      context 'set target_user as system_admin' do
        let (:target_user) {@system_admin}
        include_examples :create_password_reset_link_via_api_is_forbidden
      end

    end

    context 'as an system admin' do
      before(:each){ @current_user = @system_admin }
      context 'set target user as admin' do
        let (:target_user) {@admin}
        include_examples :create_password_reset_link_via_api
        include_examples :create_password_reset_link_via_ui
      end
    end

    context 'as and pool manager' do
      before(:each){ @current_user = @manager}
      context 'set target user as regular user' do
        let (:target_user) {@user}
        include_examples :create_password_reset_link_via_api_is_forbidden
      end
    end
  end
end
