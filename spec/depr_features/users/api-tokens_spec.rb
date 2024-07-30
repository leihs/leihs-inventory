require 'spec_helper'
require 'pry'

feature 'API Tokens', type: :feature do

  context 'signed in as an admin' do

    before :each do
      @admin = FactoryBot.create :admin
      sign_in_as @admin
    end

    let :add_api_token do
      visit '/admin/users/' + @admin.id
      click_on 'API Tokens'
      click_on 'Add API Token'
      fill_in 'Description', with: "My first token"
      click_on 'Save'
      wait_until{ page.has_content? "has been added"}
      @token_part = find(".token_part").text
      @token_secret = find(".token_secret").text
      click_on 'Continue'
      wait_until{ page.has_content? "API Token " + @token_part}
      expect(page).to have_content "My first token"
    end

    scenario 'creating an API-Token works' do
      add_api_token
    end


    context 'an API-Token for the current user has been added' do

      before :each do
        add_api_token
      end

      scenario 'the token can be used to ' \
        ' authenticate via "authorization token" header' do

        # authentication w.o. token returns 403
        expect(plain_faraday_client.get('/admin/users/').status).to be== 403

        expect(
          plain_faraday_client.get('/admin/users/'){ |conn|
            conn.headers['authorization'] = "token #{@token_secret}"
          }.status).to be== 200
      end

      scenario 'editing the description of the token' do
        click_on_first 'Edit'
        fill_in 'Description', with: 'The updated description'
        click_on 'Save'
        wait_until { first('.modal', text: 'OK') }
        expect(page).to have_content('The updated description')
      end

      scenario 'default scopes and editing the scope of the token' do
        find('.permission-summary', exact_text: "Read+Write, Admin Read+Write")

        # enable the system admin scopes

        click_on_first 'Edit'
        check 'System Admin Read'
        check 'System Admin Write'
        click_on 'Save'
        wait_until { first('.modal', text: 'OK') }

        find('.permission-summary', exact_text: "Read+Write, Admin Read+Write, System Admin Read+Write")

        # make sure the front end doesen't show some fiction
        visit current_path
        find('.permission-summary', exact_text: "Read+Write, Admin Read+Write, System Admin Read+Write")
        db_token = ApiToken.where(token_part: @token_part).all.first
        expect(db_token[:scope_system_admin_read]).to be== true
        expect(db_token[:scope_system_admin_write]).to be== true

      end

      scenario 'an edited token such that it is expired can not be used to authenticate' do
        resp = plain_faraday_client.get('/admin/users/') do |req|
          req.headers["Authorization"] = "Token #{@token_secret}"
        end
        expect(resp.status).to be== 200
        click_on_first 'Edit'
        # capybara/webdriver sets Time values properly but events/notification seem not to work
        # we trigger those via arrow_up and then arrow_down again
        within(find("div.form-group", text: "Expires")) do
          fill_in 'Expires', with: (Time.now - 3.hours)
          find("input[type=datetime-local]").send_keys(:arrow_up)
          find("input[type=datetime-local]").send_keys(:arrow_down)
        end
        click_on 'Save'
        wait_until { first('.modal', text: 'OK') }
        resp2 = plain_faraday_client.get('/admin/users/') do |req|
          req.headers["Authorization"] = "Token #{@token_secret}"
        end
        expect(resp2.status).to be== 401
      end

      scenario 'deleting a token works and makes it useless for authentication' do
        resp = plain_faraday_client.get('/admin/users/') do |req|
          req.headers["Authorization"] = "Token #{@token_secret}"
        end
        expect(resp.status).to be== 200
        click_on_first 'Delete'
        wait_until { page.has_content? "Delete API Token" }
        within('.modal') do
          click_on 'Delete'
        end
        expect(page).to have_content('Basic User Properties')
        click_on 'API Tokens'
        expect(page).to have_content('No API Tokens')
        resp2 = plain_faraday_client.get('/admin/users/') do |req|
          req.headers["Authorization"] = "Token #{@token_secret}"
        end
        expect(resp2.status).to be== 401
      end

    end
  end


  context 'check different access rights' do

    scenario 'An ordinary user can not access/modify API tokens' do
      user = FactoryBot.create :user
      api_token = FactoryBot.create :api_token, user_id: user.id

      sign_in_as user
      
      visit "/admin/users/#{user.id}"
      wait_until{ page.has_content? 'ERROR 403'}
      
      http_client = plain_faraday_client
      http_client.headers["Authorization"] = "Token #{api_token.token_secret}"

      resp = http_client.get "/admin/users/#{user.id}/api-tokens/"
      expect(resp.status).to be== 403

      resp = http_client.patch "/admin/users/#{user.id}/api-tokens/#{api_token.id}", { description: "whatever" }.to_json
      expect(resp.status).to be== 403
    end

    scenario 'An inventory manager can not access/modify API tokens' do
      user = FactoryBot.create :user
      api_token = FactoryBot.create :api_token, user_id: user.id
      pool = FactoryBot.create :inventory_pool
      FactoryBot.create :access_right, user: user,
        inventory_pool: pool, role: 'inventory_manager'

      sign_in_as user

      visit "/admin/users/#{user.id}"
      expect(page).to have_content('Basic User Properties')
      expect(page).not_to have_content('API Tokens')
      
      http_client = plain_faraday_client
      http_client.headers["Authorization"] = "Token #{api_token.token_secret}"

      resp = http_client.get "/admin/users/#{user.id}/api-tokens/"
      expect(resp.status).to be== 403

      resp = http_client.patch "/admin/users/#{user.id}/api-tokens/#{api_token.id}", { description: "whatever" }.to_json
      expect(resp.status).to be== 403
    end

    scenario 'An admin can not access/modify API tokens for other user' do
      other_user = FactoryBot.create :user
      other_user_api_token = FactoryBot.create :api_token, user_id: other_user.id

      user = FactoryBot.create :admin
      api_token = FactoryBot.create :api_token, user_id: user.id
      sign_in_as user
      
      visit "/admin/users/#{other_user.id}"
      expect(page).to have_content('Basic User Properties')
      expect(page).not_to have_content('API Tokens')

      http_client = plain_faraday_client
      http_client.headers["Authorization"] = "Token #{api_token.token_secret}"

      resp = http_client.get "/admin/users/#{other_user.id}/api-tokens/"
      expect(resp.status).to be== 403

      resp = http_client.patch "/admin/users/#{other_user.id}/api-tokens/#{other_user_api_token.id}", { description: "whatever" }.to_json
      expect(resp.status).to be== 403
    end

    scenario 'A system-admin can access/modify API tokens for other user' do
      other_user = FactoryBot.create :user

      system_admin = FactoryBot.create :system_admin
      sign_in_as system_admin

      # List
      visit "/admin/users/#{other_user.id}"
      expect(page).to have_content('Basic User Properties')
      click_on "API Tokens"

      # Add
      click_on 'Add API Token'
      fill_in 'Description', with: "Hello token"

      loop do
        click_on 'Save'
        break if page.has_content? "has been added"
      end

      token_part = find(".token_part").text
      click_on 'Continue'
      wait_until{ page.has_content? "API Token " + token_part}
      expect(page).to have_content "Hello token"

      # Edit
      click_on_first 'Edit'
      fill_in 'Description', with: 'The updated description'
      
      loop do 
        click_on 'Save'
        break if first('.modal', text: 'OK') 
      end
      
      expect(page).to have_content('The updated description')

      # Delete
      click_on_first 'Delete'
      wait_until { page.has_content? "Delete API Token" }
      within('.modal') do
        click_on 'Delete'
      end
      expect(page).to have_content('Basic User Properties')
      click_on 'API Tokens'
      expect(page).to have_content('No API Tokens')
    end
  end
end
