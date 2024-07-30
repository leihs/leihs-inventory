require 'spec_helper'
require 'pry'

feature 'Creating groups', type: :feature do

  context 'a bunch of groups, users and some admins exist;' do

    before :each do
      @admins = 3.times.map { FactoryBot.create :admin }
      @system_admins = 3.times.map { FactoryBot.create :system_admin}
      @users = 15.times.map { FactoryBot.create :user }
      @groups = 15.times.map { FactoryBot.create :group }
    end


    context "an system admin" do
      before :each do
        @system_admin = @system_admins.sample
      end

      context "via the UI" do
        before :each do
          sign_in_as @system_admin
        end

        scenario 'creates a new group ' do

          description = <<~TEXT
          Describir es explicar, de manera detallada y ordenada, cómo son las personas, animales, lugares, objetos, etc.
          La descripción sirve sobre todo para ambientar la acción y crear una que haga más creíbles los hechos que se narran.
          TEXT
          name = "La Grupa"

          visit '/admin/'
          click_on 'Groups'
          click_on_first 'Add Group'
          within(".modal") do
            fill_in 'name', with: name
            fill_in '(organization)', with: 'example.com'
            fill_in '(org_id)', with: '123'
            fill_in 'description', with: description
          end
          check '(system_admin_protected)'
          check '(admin_protected)'
          click_on 'Add'

          wait_until{ all(".modal").empty? }
          click_on_first 'Groups'
          fill_in 'term', with: name
          wait_until { all(".group").count == 1 }

          # we can open the group by clicking on the name
          click_on name
          expect(page).to have_content "#{name}"


          within("tr.admin_protected") { expect(page).to have_text("yes") }
          within("tr.system_admin_protected") { expect(page).to have_text("yes") }
          within("tr.organization") { expect(page).to have_text("example.com") }
          within("tr.org_id") { expect(page).to have_text("123") }

          # we can see the full description here too
          expect(page.text.tr("\n\r\s"," ")).to have_content description.tr("\n\r\s"," ")

        end
      end
    end



    context "an admin" do
      before :each do
        @admin = @admins.sample
      end

      context "via the UI" do
        before :each do
          sign_in_as @admin
        end

        scenario 'creates a new group ' do

          description = <<~TEXT
          Describir es explicar, de manera detallada y ordenada, cómo son las personas, animales, lugares, objetos, etc.
          La descripción sirve sobre todo para ambientar la acción y crear una que haga más creíbles los hechos que se narran.
          TEXT
          name = "La Grupa"

          visit '/admin/'
          click_on 'Groups'
          click_on_first 'Add Group'
          within ".modal" do
            fill_in 'name', with: name
            fill_in '(organization)', with: 'example.com'
            fill_in '(org_id)', with: '123'
            fill_in 'description', with: description
          end
          expect(find_field('(system_admin_protected)', disabled: true)).not_to be_checked
          check '(admin_protected)'
          click_on 'Add'

          wait_until{ all(".modal").empty? }
          click_on_first ('Groups')
          fill_in 'term', with: name
          wait_until { all(".group").count == 1 }

          # we can open the group by clicking on the name
          click_on name
          expect(page).to have_content "#{name}"

          within("tr.admin_protected") { expect(page).to have_text("yes") }
          within("tr.system_admin_protected") { expect(page).to have_text("no") }
          within("tr.organization") { expect(page).to have_text("example.com") }
          within("tr.org_id") { expect(page).to have_text("123") }

          # we can see the full description here too
          expect(page.text.tr("\n\r\s"," ")).to have_content description.tr("\n\r\s"," ")

        end

      end

      context 'via the API' do

        let :http_client do
          plain_faraday_client
        end

        let :prepare_http_client do
          @api_token = FactoryBot.create :admin_api_token,
            user_id: @admin.id

          @token_secret = @api_token.token_secret
          http_client.headers["Authorization"] = "Token #{@token_secret}"
          http_client.headers["Content-Type"] = "application/json"
        end

        before :each do
          prepare_http_client
        end

        let(:name){ "New Group" }

        scenario "can create a new group" do
          resp = http_client.post "/admin/groups/", {name: name}.to_json
          expect(resp.status).to be== 201
          new_group = Group.where(name: name).first
          expect(new_group).to be
        end

        scenario "can set the admin_protected attribute" do
          resp = http_client.post "/admin/groups/",
            {name: name, admin_protected: true }.to_json
          expect(resp.status).to be== 201
          new_group = Group.where(name: name).first
          expect(new_group).to be
        end

        scenario "is forbidden to set the system_admin_protected attribute" do
          resp = http_client.post "/admin/groups/",
            {name: name, system_admin_protected: true }.to_json
          expect(resp.status).to be== 403
          new_group = Group.where(name: name).first
          expect(new_group).not_to be
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
        before :each do
          sign_in_as @lending_manager
        end

        scenario 'creates a new group ' do
          description = <<~TEXT
          Describir es explicar, de manera detallada y ordenada, cómo son las personas, animales, lugares, objetos, etc.
          La descripción sirve sobre todo para ambientar la acción y crear una que haga más creíbles los hechos que se narran.
          TEXT
          name = "La Grupa"

          visit '/admin/'
          click_on 'Groups'
          click_on_first 'Add Group'
          within ".modal" do
            fill_in 'name', with: name
            fill_in 'description', with: description
          end
          expect(find_field('(system_admin_protected)', disabled: true)).not_to be_checked
          expect(find_field('(admin_protected)', disabled: true)).not_to be_checked
          expect(find_field('(organization)', disabled: true)).to be
          expect(find_field('(org_id)', disabled: true)).to be
          click_on 'Add'

          click_on_first 'Groups'
          fill_in 'term', with: name
          wait_until { all(".group").count == 1 }
          click_on name
          expect(page).to have_content "#{name}"

          # we can see the full description here too
          expect(page.text.tr("\n\r\s"," ")).to have_content description.tr("\n\r\s"," ")
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

        let(:name){ "New Group" }

        scenario "can create a new group" do
          resp = http_client.post "/admin/groups/",
            {name: name}.to_json
          expect(resp.status).to be== 201
          new_group = Group.where(name: name).first
          expect(new_group).to be
        end

        scenario "is forbidden to set the admin_protected attribute" do
          resp = http_client.post "/admin/groups/",
            {name: name, admin_protected: true }.to_json
          expect(resp.status).to be== 403
          new_group = Group.where(name: name).first
          expect(new_group).not_to be
        end

        scenario "is forbidden to set the system_admin_protected attribute" do
          resp = http_client.post "/admin/groups/",
            {name: name, system_admin_protected: true }.to_json
          expect(resp.status).to be== 403
          new_group = Group.where(name: name).first
          expect(new_group).not_to be
        end

        scenario "is forbidden to set the organization attribute" do
          resp = http_client.post "/admin/groups/",
            {name: name, organization: 'example.com' }.to_json
          expect(resp.status).to be== 403
          new_group = Group.where(name: name).first
          expect(new_group).not_to be
        end

        scenario "is forbidden to set the org_id attribute" do
          resp = http_client.post "/admin/groups/",
            {name: name, org_id: '123' }.to_json
          expect(resp.status).to be== 403
          new_group = Group.where(name: name).first
          expect(new_group).not_to be
        end

      end

    end

  end

end

