require 'spec_helper'
require 'pry'

feature 'Deleting groups', type: :feature do

  context 'some admins, a bunch of users and a bunch of groups exist' do

    before :each do
      @admins = 3.times.map { FactoryBot.create :admin }
      @system_admins = 3.times.map { FactoryBot.create :system_admin}
      @users = 100.times.map { FactoryBot.create :user }
      @groups = 100.times.map { FactoryBot.create :group }
    end

    context 'an system-admin via the UI' do
      before :each do
        sign_in_as @system_admins.sample
      end

      scenario "deletes a system_admin_protected group" do
        @group = @groups.filter{|g| g[:system_admin_protected] == true }.sample
        expect(database[:groups].where(id: @group.id).first).to be
        visit '/admin/'
        click_on 'Groups'
        fill_in 'Search', with: @group.name
        click_on @group.name
        sleep 1
        click_on 'Delete'
        wait_until do
          within ".modal" do
            page.has_content? "Delete Group"
          end
        end
        within ".modal" do
          click_on 'Delete'
        end
        wait_until{ current_path == '/admin/groups/' }
        expect(database[:groups].where(id: @group.id).first).not_to be
      end
    end

    context 'an admin via the UI' do

      before :each do
        @admin = @admins.sample
        sign_in_as @admin
      end

      scenario "deletes an only admin_protected group" do
        @group = @groups.filter{ |g|
          g[:admin_protected] == true && g[:system_admin_protected] == false
        }.sample
        expect(database[:groups].where(id: @group.id).first).to be
        visit '/admin/'
        click_on 'Groups'
        fill_in 'Search', with: @group.name
        click_on @group.name
        sleep 1
        click_on 'Delete'
        wait_until do
          page.has_content? "Delete Group"
        end
        within ".modal" do
          click_on 'Delete'
        end
        wait_until{ current_path == '/admin/groups/' }
        expect(database[:groups].where(id: @group.id).first).not_to be
      end


      scenario "can not delete a system_admin_protected group" do
        @group = @groups.filter{|g| g[:system_admin_protected] == true }.sample
        expect(database[:groups].where(id: @group.id).first).to be
        visit '/admin/'
        click_on 'Groups'
        fill_in 'Search', with: @group.name
        click_on @group.name
        expect(all("a, button", text: "Delete").count).to be== 0
      end


      context 'via the API' do

        let :http_client do
          plain_faraday_client
        end

        let :prepare_http_client do
          @api_token = FactoryBot.create :admin_api_token, user_id: @admin.id
          @token_secret = @api_token.token_secret
          http_client.headers["Authorization"] = "Token #{@token_secret}"
          http_client.headers["Content-Type"] = "application/json"
        end

        before :each do
          prepare_http_client
        end

        scenario 'deleting an system_admin_protected group is forbidden ' do
          @group = @groups.filter{|u| u[:system_admin_protected]==true }.sample
          resp = http_client.delete "/admin/groups/#{@group[:id]}"
          expect(resp.status).to be== 403
        end

      end

    end


    context "some inventory-pool's lending-manager " do

      before :each do
        @pool =  FactoryBot.create :inventory_pool
        @lending_manager = FactoryBot.create :user
        FactoryBot.create :access_right, user: @lending_manager,
          inventory_pool: @pool, role: 'lending_manager'

      end


      context "signs in, and " do

        before(:each){ sign_in_as @lending_manager }

        scenario "deletes a not admin-protected group" do
          @group = @groups.filter{|g| g[:admin_protected] == false }.sample
          expect(database[:groups].where(id: @group.id).first).to be
          visit '/admin/'
          click_on 'Groups'
          fill_in 'Search', with: @group.name
          click_on @group.name
          sleep 1
          click_on 'Delete'
          wait_until do
            within ".modal" do
              page.has_content? "Delete Group"
            end
          end
          within ".modal" do
            click_on 'Delete'
          end
          wait_until{ current_path == '/admin/groups/' }
          expect(database[:groups].where(id: @group.id).first).not_to be
        end

        scenario 'can not delete a admin-protected group' do
          @group = @groups.filter{|g| g[:admin_protected] == true }.sample
          expect(database[:groups].where(id: @group.id).first).to be
          visit '/admin/'
          click_on 'Groups'
          fill_in 'Search', with: @group.name
          click_on @group.name
          expect(all("a, button", text: "Delete").count).to be== 0
        end

        context 'via the API' do

          let :http_client do
            plain_faraday_client
          end

          let :prepare_http_client do
            @api_token = FactoryBot.create :api_token, user_id: @lending_manager.id
            @token_secret = @api_token.token_secret
            http_client.headers["Authorization"] = "Token #{@token_secret}"
            http_client.headers["Content-Type"] = "application/json"
          end

          before :each do
            prepare_http_client
          end

          scenario 'deleting an admin_protected group is forbidden ' do
            @group = @groups.filter{|u| u[:admin_protected]==true }.sample
            resp = http_client.delete "/admin/groups/#{@group[:id]}"
            expect(resp.status).to be== 403
          end

        end
      end
    end
  end
end
