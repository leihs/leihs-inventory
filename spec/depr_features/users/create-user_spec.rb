require 'spec_helper'
require 'pry'
require "#{File.dirname(__FILE__)}/_shared"

shared_examples :create_with_extra_props do |extra_props = {}|
  scenario "I can create a user with all basic properties and #{extra_props}" do
    properties = BASIC_USER_PROPERTIES.merge(extra_props)
    click_on 'Users'
    click_on_first 'Add User'
    wait_until { page.has_css?('.modal') }
    binding.pry
    within '.modal' do
      fill_in_user_properties properties
    end
    sleep 0.1
    click_on 'Add'
    wait_until { page.has_no_css?('.modal') }
    wait_until { current_path.match(%r"/admin/users/([^/]+)") }
    assert_user_properties current_path.match(%r"/admin/users/([^/]+)")[1], properties
  end
end

shared_examples :can_not_set_the_attribure do |attr_name|
  scenario "I can not set the attribure #{attr_name}" do
    click_on 'Users'
    click_on_first 'Add User'
    fill_in_user_properties ::BASIC_USER_PROPERTIES
    expect(find_field(attr_name, disabled: :all)).to be_disabled
  end
end

shared_examples :create_via_api_ok do |extra_props|
  scenario "I can create the a user via the API with extra_props #{extra_props}" do
    properties = BASIC_USER_PROPERTIES.merge(extra_props)
    resp = @http_client.post "/admin/users/", properties.to_json
    expect(resp.status).to be== 201
    data = resp.body.with_indifferent_access
    properties.each do |k,v|
      expect(data[k]).to be== properties[k]
    end
  end
end

shared_examples :create_via_api_forbidden do |extra_props|
  scenario "It is forbidden for me to create the a user via the API with extra_props #{extra_props}" do
    properties = BASIC_USER_PROPERTIES.merge(extra_props)
    resp = @http_client.post "/admin/users/", properties.to_json
    expect(resp.status).to be== 403
  end
end

feature 'Creating a user', type: :feature do

  context 'all types of useres exist,' do
    include_context :all_types_of_users

    context 'as a pool manager' do

      before(:each){ @current_user = @manager}
      context 'via the UI' do
        include_context :sign_in_to_admin
        include_context :basic_user_properties
        include_examples :create_with_extra_props, {}
        include_examples :can_not_set_the_attribure, :is_admin
        include_examples :can_not_set_the_attribure, :admin_protected
        include_examples :can_not_set_the_attribure, :is_system_admin
        include_examples :can_not_set_the_attribure, :system_admin_protected
      end

      context 'via the API' do
        include_context :setup_api
        include_context :basic_user_properties
        include_examples :create_via_api_ok, {}
        include_examples :create_via_api_forbidden, {is_admin: true}
        include_examples :create_via_api_forbidden, {admin_protected: true}
        include_examples :create_via_api_forbidden, {is_system_admin: true}
        include_examples :create_via_api_forbidden, {system_admin_protected: true}
      end
    end

    context 'as an admin' do

      before(:each){ @current_user = @admin}

      context 'via the UI' do
        include_context :sign_in_to_admin
        include_context :basic_user_properties
        include_examples :create_with_extra_props, {}
      end

      context 'via the API' do
        include_context :setup_api
        include_context :basic_user_properties
        include_examples :create_via_api_ok, {is_admin: true, admin_protected: true}
        include_examples :create_via_api_forbidden, {is_system_admin: true}
        include_examples :create_via_api_forbidden, {system_admin_protected: true}
      end

    end

    context 'as a system-admin' do
      before(:each){ @current_user = @system_admin}

      context 'via the UI' do
        include_context :sign_in_to_admin
        include_context :basic_user_properties
        include_examples :create_with_extra_props,
          {is_admin: true, admin_protected: true,
           is_system_admin: true, system_admin_protected: true,
           organization: 'example-com', org_id: '123'}
      end

      context 'via the UI' do
        context "disabling default true values in table definition" do
          include_context :sign_in_to_admin
          include_context :basic_user_properties
          include_examples :create_with_extra_props,
            {account_enabled: false,
             password_sign_in_enabled: false}
        end
      end
    end
  end
end
