require 'spec_helper'
require 'pry'
require "#{File.dirname(__FILE__)}/_shared"

def ui_update_user_ok user, extra_props
  properties = BASIC_USER_PROPERTIES.merge(extra_props)
  click_on 'Users'
  click_on_first_user user
  click_on 'Edit'
  fill_in_user_properties properties
  sleep(0.5) if (properties.include? :img256_url) or (properties.include? :img32_url)
  click_on 'Save'
  wait_until { current_path.match(%r"/admin/users/([^/]+)") }
  sleep(0.5) if (properties.include? :img256_url) or (properties.include? :img32_url)
  assert_user_properties user[:id] , properties
end

def ui_can_not_edit user
  click_on 'Users'
  click_on_first_user user
  wait_until { current_path.match(%r"/admin/users/([^/]+)") }
  within('.basic-properties') do
    expect(all("button, a", text: 'Edit')).to be_empty
  end
end

def ui_update_attribute_disabled user, attr_name
  click_on 'Users'
  click_on_first_user user
  click_on 'Edit'
  fill_in_user_properties ::BASIC_USER_PROPERTIES
  expect(find_field(attr_name, disabled: :all)).to be_disabled
end

def api_update_ok user, extra_props
  properties = BASIC_USER_PROPERTIES.merge(extra_props)
  resp = @http_client.patch "/admin/users/#{user[:id]}", properties.to_json
  expect(resp.status).to be== 200
  data = resp.body.with_indifferent_access
  properties.each do |k,v|
    expect(data[k]).to be== properties[k]
  end
end

def api_update_forbidden user, extra_props = {}
  properties = BASIC_USER_PROPERTIES.merge(extra_props)
  resp = @http_client.patch "/admin/users/#{user[:id]}", properties.to_json
  expect(resp.status).to be== 403
end

feature 'Editing and updating a user', type: :feature do
  context 'all types of useres exist,' do
    include_context :all_types_of_users

    context 'as a pool manager' do
      before(:each){ @current_user = @manager }

      context 'via the UI' do
        include_context :sign_in_to_admin
        include_context :basic_user_properties
        scenario "I can update a normal user with basic properties"  do
          ui_update_user_ok @user, {}
        end
        scenario "I can not change the organization attribute" do
          ui_update_attribute_disabled @user, :organization
        end
        scenario "I can not change the org_id attribure" do
          ui_update_attribute_disabled @user, :org_id
        end
        scenario "I can not change the is_admin attribure" do
          ui_update_attribute_disabled @user, :is_admin
        end
        scenario "I can not change the admin_protected attribure" do
          ui_update_attribute_disabled @user, :admin_protected
        end
        scenario "I can not change the is_system_admin attribure" do
          ui_update_attribute_disabled @user, :is_system_admin
        end
        scenario "I can not change the system_admin_protected attribure" do
          ui_update_attribute_disabled @user, :system_admin_protected
        end
        scenario "I can not update an admin" do
          ui_can_not_edit @admin
        end
        scenario "I can not update a system_admin" do
          ui_can_not_edit @system_admin
        end
        scenario "I can not update an admin_protected user" do
          database[:users].where(id: @user[:id]).update(admin_protected: true)
          ui_can_not_edit @user
        end
        scenario "I can not update an system_admin_protected user" do
          database[:users].where(id: @user[:id]) \
            .update(admin_protected: true, system_admin_protected: true)
          ui_can_not_edit @user
        end
      end

      context 'via the API' do
        include_context :setup_api
        include_context :basic_user_properties
        scenario "I can update user via the API with basic_user_properties" do
          api_update_ok @user, {}
        end
        scenario "It is forbidden for me to update the a user via the API changing the organization" do
          api_update_forbidden @user, organization: Faker::Internet.domain_name
        end
        scenario "It is forbidden for me to update the a user via the API changing the org_id" do
          api_update_forbidden @user, org_id: Faker::Internet.uuid
        end
        scenario "It is forbidden for me to update the a user via the API changing admin_protected to true" do
          api_update_forbidden @user, admin_protected: true
        end
        scenario "It is forbidden for me to update the a user via the API changing is_admin to true" do
          api_update_forbidden @user, is_admin: true
        end
        scenario "It is forbidden for me to update the a user via the API changing admin_protected to true" do
          api_update_forbidden @user, admin_protected: true
        end
        scenario "It is forbidden for me to update the a user via the API changing is_system_admin to true" do
          api_update_forbidden @user, is_system_admin: true
        end
        scenario "It is forbidden for me to update the a user via the API changing system_admin_protected to true" do
          api_update_forbidden @user, system_admin_protected: true
        end
        scenario "It is forbidden for me to update an admin with basic properties" do
          api_update_forbidden @admin
        end
        scenario "It is forbidden for me to update an system_admin with basic properties" do
          api_update_forbidden @system_admin
        end
        scenario "It is forbidden for me to update an admin_protect normal user with basic properties" do
          database[:users].where(id: @user[:id]).update(admin_protected: true)
          api_update_forbidden @user
        end
        scenario "It is forbidden for me to update an system_admin_protected normal user with basic properties" do
          database[:users].where(id: @user[:id]).update(admin_protected: true, system_admin_protected: true)
          api_update_forbidden @user
        end
      end
    end


    context 'as an (leihs) admin' do
      before(:each){ @current_user = @admin }

      context 'via the UI' do
        include_context :sign_in_to_admin
        include_context :basic_user_properties
        scenario "I can update a normal user with basic properties"  do
          ui_update_user_ok @user, {}
        end
        scenario "I can update an other admin with basic properties"  do
          database[:users].where(id: @user[:id]).update(is_admin: true, admin_protected: true)
          ui_update_user_ok @user, {}
        end
        scenario "I can update an admin_protected user with basic properties"  do
          database[:users].where(id: @user[:id]).update(admin_protected: true)
          ui_update_user_ok @user, {}
        end
        scenario "I can change the organization attribute" do
          ui_update_user_ok @user, {organization: Faker::Internet.domain_name}
        end
        scenario "I can change the org_id attribure" do
          ui_update_user_ok @user, {org_id: Faker::Internet.uuid}
        end
        scenario "I can escalate a user to admin " do
          ui_update_user_ok @user, {is_admin: true, admin_protected: true}
        end
        scenario "I can set a user to admin_protected " do
          ui_update_user_ok @user, {admin_protected: true}
        end
        scenario "I can not change the is_system_admin attribure" do
          ui_update_attribute_disabled @user, :is_system_admin
        end
        scenario "I can not change the system_admin_protected attribure" do
          ui_update_attribute_disabled @user, :system_admin_protected
        end
        scenario "I can not update an system_admin_protected user" do
          database[:users].where(id: @user[:id]) \
            .update(admin_protected: true, system_admin_protected: true)
          ui_can_not_edit @user
        end
      end

      context 'via the API' do
        include_context :setup_api
        include_context :basic_user_properties
        scenario "I can update user via the API with basic_user_properties" do
          api_update_ok @user, {}
        end
        scenario "It is forbidden for me to update the a user via the API changing is_system_admin to true" do
          api_update_forbidden @user, is_system_admin: true
        end
        scenario "It is forbidden for me to update the a user via the API changing system_admin_protected to true" do
          api_update_forbidden @user, system_admin_protected: true
        end
        scenario "It is forbidden for me to update an system_admin with basic properties" do
          api_update_forbidden @system_admin
        end
        scenario "It is forbidden for me to update an system_admin_protected normal user with basic properties" do
          database[:users].where(id: @user[:id]).update(admin_protected: true, system_admin_protected: true)
          api_update_forbidden @user
        end
      end
    end

    context 'as a system admin' do
      before(:each){ @current_user = @system_admin}

      context 'via the UI' do
        include_context :sign_in_to_admin
        include_context :basic_user_properties

        scenario "I can update an other system-admin with basic properties"  do
          database[:users].where(id: @user[:id])\
            .update(is_admin: true, admin_protected: true, is_system_admin: true, system_admin_protected: true)
          ui_update_user_ok @user, {}
        end
        scenario "I can update an sytem_admin_protected user with basic properties"  do
          database[:users].where(id: @user[:id])\
            .update(admin_protected: true, system_admin_protected: true)
          ui_update_user_ok @user, {}
        end
        scenario "I can escalate a user to system_admin " do
          ui_update_user_ok @user,
            {is_admin: true, admin_protected: true,
             is_system_admin: true, system_admin_protected: true}
        end
        scenario "I can set a user to system_admin_protected " do
          ui_update_user_ok @user, {admin_protected: true, system_admin_protected: true}
        end
      end

    end
  end
end



