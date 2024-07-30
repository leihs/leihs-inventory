require 'spec_helper'
require 'pry'

feature 'Editing groups', type: :feature do
  context 'some admins, a bunch of users and a bunch of groups exist' do
    before :each do
      @admins = 3.times.map { FactoryBot.create :admin }
      @system_admins = 3.times.map { FactoryBot.create :system_admin}
      @users = 100.times.map { FactoryBot.create :user }
      @groups = 100.times.map { FactoryBot.create :group }
    end


    context 'an system-admin via the UI' do
      before :each do
        @admin = @system_admins.sample
        sign_in_as @admin
      end

      scenario 'edits a system-admin protected group ' do
        visit '/admin/'
        click_on 'Groups'
        group = @groups.filter{|g| g[:system_admin_protected] == true }.sample # pick a random group
        fill_in 'term', with: group.name
        click_on group.name
        click_on 'Edit'

        name = Faker::Name.name
        description = Faker::Lorem.sentences.join(' ')

        fill_in 'name', with: name
        fill_in 'description', with: description
        click_on 'Save'
        wait_until { page.has_content? "#{name}" }

        within("tr.name") { expect(page).to have_text name } 
        within("tr.description") { expect(page).to have_text description } 
      end

    end


    context 'an admin via the UI' do
      before :each do
        @admin = @admins.sample
        sign_in_as @admin
      end

      scenario 'edits an admin protected group ' do
      visit '/admin/'
      click_on 'Groups'

      # Select a random group
      group = @groups.find { |g| g[:admin_protected] && !g[:system_admin_protected] }

      fill_in 'term', with: group.name
      click_on group.name

      # Retry until the modal is displayed
      click_on 'Edit'; page.has_content?("Edit Group")

      # Generate random name and description
      name = Faker::Name.name
      description = Faker::Lorem.sentences.join(' ')

      fill_in 'name', with: name
      fill_in 'description', with: description
      click_on 'Save'

      # Wait until the page has the new name
      wait_until { page.has_content? name }

      # Check if the page has the correct content
      expect(page).to have_selector("tr.admin_protected", text: "yes")
      expect(page).to have_selector("tr.name", text: name)
      expect(page).to have_selector("tr.description", text: description)
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

        scenario 'changing some field of a system_admin_protected group is forbidden ' do
          group = @groups.filter{|g| g[:system_admin_protected] == true }.sample # pick a random group
          resp = http_client.patch "/admin/groups/#{group[:id]}", {name: "New Name"}.to_json
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

      context 'via the UI' do
        before(:each){ sign_in_as @lending_manager }

        scenario 'edits an unprotected group' do
          visit '/admin/'
          click_on 'Groups'
          group = @groups.filter{|g| g[:admin_protected] == false }.sample # pick a random group
          expect(group).to be
          fill_in 'term', with: group.name
          click_on group.name
          click_on 'Edit'
          expect(find(:checkbox, id: 'admin_protected', disabled: true)).not_to be_checked
          name = Faker::Name.name
          description = Faker::Lorem.sentences.join(' ')
          fill_in 'name', with: name
          fill_in 'description', with: description
          click_on 'Save'

          expect(page).to have_content(name, wait: 10)

          within("tr.admin_protected") { expect(page).to have_text "no" } 
          within("tr.name") { expect(page).to have_text name }
          within("tr.description") { expect(page).to have_text description } 
        end

        scenario 'can not edit a protected group' do
          visit '/admin/'
          click_on 'Groups'
          group = @groups.filter{|g| g[:admin_protected] == true }.sample # pick a random group
          expect(group).to be
          fill_in 'term', with: group.name
          click_on group.name
          expect(all("a, button", text: "Edit").count).to be== 0
        end

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

        scenario 'changing the protected field of an unprotected group is forbidden ' do
          group = @groups.filter{|g| g[:admin_protected] == false }.sample # pick a random group
          resp = http_client.patch "/admin/groups/#{group[:id]}", {admin_protected: true }.to_json
          expect(resp.status).to be== 403
        end


        scenario 'changing some field of a protected group is forbidden ' do
          group = @groups.filter{|g| g[:admin_protected] == true }.sample # pick a random group
          resp = http_client.patch "/admin/groups/#{group[:id]}", {name: "New Name"}.to_json
          expect(resp.status).to be== 403
        end
      end
    end
  end
end
