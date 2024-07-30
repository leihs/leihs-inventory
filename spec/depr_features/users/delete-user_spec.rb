require 'spec_helper'
require 'pry'
require "#{File.dirname(__FILE__)}/_shared"

def ui_delete_user user
  click_on 'Users'
  click_on_first_user user
  click_on 'Delete'
  click_on 'Delete'
  expect(database[:users].where(id: @user[:id]).first).not_to be
end

def ui_transfer_and_delete user
  FactoryBot.create :reservation, user_id: user[:id]
  target_user = FactoryBot.create :user
  click_on 'Users'
  click_on_first_user user
  click_on 'Delete'
  click_on 'Choose user'
  fill_in 'Search', with: target_user.email
  click_on 'Choose user'
  click_on 'Transfer and delete'
  expect(database[:users].where(id: @user[:id]).first).not_to be
end


def ui_no_delete_button user
  click_on 'Users'
  click_on_first_user user
  wait_until { current_path.match(%r"/admin/users/([^/]+)") }
  within('.basic-properties') do
    expect(all("button, a", text: 'Delete')).to be_empty
  end
end

def api_delete_forbidden user
  resp = @http_client.delete "/admin/users/#{user[:id]}"
  expect(resp.status).to be== 403
end

def api_transfer_delete_forbidden user
  FactoryBot.create :reservation, user_id: user[:id]
  target_user = FactoryBot.create :user
  resp = @http_client.delete \
    "/admin/users/#{user[:id]}/transfer/#{target_user[:id]}"
  expect(resp.status).to be== 403
end

feature 'Deleting users', type: :feature do
  context 'all types of useres exist,' do
    include_context :all_types_of_users

    context 'as a pool manager' do
      before(:each){ @current_user = @manager }

      context 'via the UI' do
        include_context :sign_in_to_admin
        scenario "I can delete a regular user" do
          ui_delete_user @user
        end
        scenario "I can transfer data and delete a regular user" do
          ui_transfer_and_delete @user
        end
        scenario "There is no delete button for an admin" do
          ui_no_delete_button @admin
        end
        scenario "There is no delete button for an admin_protected user" do
          database[:users].where(id: @user[:id]).update(admin_protected: true)
          ui_no_delete_button @user
        end
      end

      context 'via the API' do
        include_context :setup_api
        include_context :basic_user_properties
        scenario "It is forbidden for me to delete an admin" do
          api_delete_forbidden @admin
        end
        scenario "It is forbidden for me to delete an admin_protected user" do
          database[:users].where(id: @user[:id]).update(admin_protected: true)
          api_delete_forbidden @user
        end
        scenario "It is forbidden for me to tranfer and delete an admin" do
          api_transfer_delete_forbidden @admin
        end
        scenario "It is forbidden for me to transfer and delete an admin_protected user" do
          database[:users].where(id: @user[:id]).update(admin_protected: true)
          api_transfer_delete_forbidden @user
        end
      end
    end

    context 'as an admin' do
      before(:each){ @current_user = @admin}

      context 'via the UI' do
        include_context :sign_in_to_admin
        scenario "I can delete an other admin" do
          database[:users].where(id: @user[:id])\
            .update(is_admin: true, admin_protected: true)
          ui_delete_user @user
        end
        scenario "There is no delete button for a system_admin" do
          ui_no_delete_button @system_admin
        end
        scenario "There is no delete button for an system_admin_protected user" do
          database[:users].where(id: @user[:id])\
            .update(admin_protected: true, system_admin_protected: true)
          ui_no_delete_button @user
        end
      end

      context 'via the API' do
        include_context :setup_api
        include_context :basic_user_properties
        scenario "It is forbidden for me to delete a sytem_admin" do
          api_delete_forbidden @system_admin
        end
        scenario "It is forbidden for me to delete an admin_protected user" do
          database[:users].where(id: @user[:id])\
            .update(admin_protected: true, system_admin_protected: true)
          api_delete_forbidden @user
        end
        scenario "It is forbidden for me to transfer and delete an system_admin" do
          api_transfer_delete_forbidden @system_admin
        end
        scenario "It is forbidden for me to transfer and delete an admin_protected user" do
          database[:users].where(id: @user[:id])\
            .update(admin_protected: true, system_admin_protected: true)
          api_transfer_delete_forbidden @user
        end
      end
    end

    context 'as an system_admin' do
      before(:each){ @current_user = @system_admin}

      context 'via the UI' do
        include_context :sign_in_to_admin
        scenario "I can delete an other system_admin" do
          database[:users].where(id: @user[:id])\
            .update(is_admin: true, is_system_admin: true,
                    admin_protected: true, system_admin_protected: true)
          ui_delete_user @user
        end
      end
    end

  end
end
