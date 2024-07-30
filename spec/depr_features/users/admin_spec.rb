require 'spec_helper'
require 'pry'
require "#{File.dirname(__FILE__)}/_shared"


feature 'Managing admin users:' do

  context 'all types of useres exist,' do
    include_context :all_types_of_users

    context 'as an pool manager' do
      before(:each){ @current_user = @manager}

      context 'via the UI' do
        include_context :sign_in_to_admin

        scenario 'I can not upgrade myself to either admin or system-admin' do
          click_on 'Users'
          click_on_first_user @current_user
          click_on  'Edit'
          expect(find_field("is_admin", disabled: :all)).to be_disabled
          expect(find_field("is_system_admin", disabled: :all)).to be_disabled
        end

        scenario 'I can not degrade an admin' do
          click_on 'Users'
          click_on_first_user @admin
          sleep 1
          expect(page).not_to have_content 'Edit'
        end

        scenario 'I can not delete an admin' do
          click_on 'Users'
          click_on_first_user @admin
          sleep 1
          expect(page).not_to have_content 'Delete'
        end

        scenario 'I can not create an admin' do
          click_on 'Users'
          click_on_first 'Add User'
          expect(find_field("is_admin", disabled: :all)).to be_disabled
        end

      end

      context 'via the API' do
        include_context :setup_api

        scenario 'it is forbidden to upgrade myself to a leihs-admin' do
          resp = @http_client.patch "/admin/users/#{@current_user[:id]}",
            {is_admin: true}.to_json
            expect(resp.status).to be== 403
        end

        scenario 'it is forbidden to upgrade myself to a system-admin' do
          resp = @http_client.patch "/admin/users/#{@current_user[:id]}",
            {is_admin: true, is_system_admin: true}.to_json
            expect(resp.status).to be== 403
        end

        scenario 'it is forbidden to delete an admin' do
          resp = @http_client.delete "/admin/users/#{@admin[:id]}"
          expect(resp.status).to be== 403
        end

        scenario 'it is forbidden to create and admin' do
          resp = @http_client.post "/admin/users/", {is_admin: true}.to_json
          expect(resp.status).to be== 403
        end

      end

    end

    context 'as an admin' do
      before(:each){ @current_user = @admin}

      context 'via the UI' do
        include_context :sign_in_to_admin

        scenario 'I can upgrade upgrade some user to admin' do
          click_on 'Users'
          click_on_first_user @manager
          click_on  'Edit'
          check 'is_admin'
          check 'admin_protected'
          click_on 'Save'
          wait_until { database[:users].where(id: @manager.id).first[:is_admin] == true }
        end

        scenario 'I can not upgrade myself to system-admin' do
          click_on 'Users'
          click_on_first_user @current_user
          click_on  'Edit'
          expect(find_field("is_system_admin", disabled: :all)).to be_disabled
        end

        scenario 'I can not delete an system-admin' do
          click_on 'Users'
          click_on_first_user @system_admin
          sleep 1
          expect(page).not_to have_content 'Delete'
        end

        scenario 'I can not create an system_admin' do
          click_on 'Users'
          click_on_first  'Add User'
          expect(find_field("is_system_admin", disabled: :all)).to be_disabled
        end

      end

      context 'via the API' do
        include_context :setup_api

        scenario 'it is forbidden to upgrade myself to a system-admin' do
          resp = @http_client.patch "/admin/users/#{@current_user[:id]}",
            {is_admin: true, is_system_admin: true}.to_json
            expect(resp.status).to be== 403
        end

        scenario 'it is forbidden to delete an system-admin' do
          resp = @http_client.delete "/admin/users/#{@system_admin[:id]}"
          expect(resp.status).to be== 403
        end

        scenario 'it is forbidden to create and admin' do
          resp = @http_client.post "/admin/users/",
            {is_admin: true, is_system_admin: true}.to_json
          expect(resp.status).to be== 403
        end

      end


      context 'as an system-admin' do
        before(:each){ @current_user = @system_admin}

        context 'via the UI' do
          include_context :sign_in_to_admin

          scenario 'I can upgrade upgrade some user to system_admin' do
            click_on 'Users'
            click_on_first_user @manager
            click_on  'Edit'
            check 'is_admin'
            check 'admin_protected'
            check 'is_system_admin'
            check 'system_admin_protected'
            click_on 'Save'
            wait_until { database[:users].where(id: @manager.id) \
              .first[:is_system_admin] == true }
          end

        end
      end
    end
  end
end
