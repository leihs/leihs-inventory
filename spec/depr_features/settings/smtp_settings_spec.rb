require 'spec_helper'
require 'pry'

feature 'SMTP-Settings' do

  context 'a system_admin and a plain admin exist' do
    before :each do
      @system_admin = FactoryBot.create :system_admin
      @plain_admin = FactoryBot.create :admin
    end

    context 'a system_admin' do
      before(:each){@user = @system_admin}

      context 'via the UI' do
        before(:each){sign_in_as @user}

        scenario 'updates the SMTP-Settings' do
          within 'aside nav' do 
            click_on "Settings"
            click_on "SMTP"
          end

          wait_until { page.has_content? "Domain Name" }
          click_on "Edit"
          check "Sending emails enabled"
          fill_in "Server port", with: "2525"
          fill_in "Server address", with: "my-smtp-host"
          fill_in "Domain name", with: "my-domain"
          fill_in "From", with: "noryply@my-domain"
          fill_in "Sender", with: "smtp-sender@my-domain"
          fill_in "User", with: "smtp-user"
          fill_in "Password", with: "smtp-password"
          fill_in "authentication_type", with: "CRAM-MD5"
          fill_in "openssl_verify_mode", with: "peer"
          check "enable_starttls_auto"
          click_on "Save"
          sleep 0.5
          wait_until{ all(".modal").empty? }
          wait_until { page.has_content? "Domain Name" }

          within find('tr', text: "Sending EMails enabled") do
            expect(page.text).to have_content 'true'
          end

          expect(page.text).to have_content '2525'
          expect(page.text).to have_content 'my-smtp-host'
          expect(page.text).to have_content 'my-domain'
          expect(page.text).to have_content 'noryply@my-domain'
          expect(page.text).to have_content 'smtp-sender@my-domain'
          expect(page.text).to have_content 'smtp-user'
          expect(page.text).to have_content 'smtp-password'
          expect(page.text).to have_content 'CRAM-MD5'
          expect(page.text).to have_content 'peer'

          within find('tr', text: "Enable Starttls Auto") do
            expect(page.text).to have_content 'true'
          end
        end

      end


      context 'via the API' do

        before :each do
          @http_client = plain_faraday_client
          @api_token = FactoryBot.create :system_admin_api_token,
            user_id: @user.id
          @token_secret = @api_token.token_secret
          @http_client.headers["Authorization"] = "Token #{@token_secret}"
          @http_client.headers["Content-Type"] = "application/json"
        end

        scenario 'updating a single property via PATCH works' do

          get = @http_client.get "/admin/settings/smtp/"
          expect(get).to be_success
          expect(get.body["enabled"]).to be false

          patch = @http_client.patch "/admin/settings/smtp/", {enabled: true}.to_json
          expect(patch).to be_success
          expect(patch.body["enabled"]).to be true

          get = @http_client.get "/admin/settings/smtp/"
          expect(get).to be_success
          expect(get.body["enabled"]).to be true

        end

      end

    end

    context 'a plain leihs_admin (not system_admin)' do
      before(:each){@user = @plain_admin}

      context 'via the UI' do
        before(:each){sign_in_as @user}

        scenario 'does not have access to the SMTP-Settings' do
          click_on "Settings"
          expect(all("a", text: "SMTP")).to be_empty
        end

      end

      context 'via the API' do

        before :each do
          @http_client = plain_faraday_client
          @api_token = FactoryBot.create :system_admin_api_token,
            user_id: @user.id
          @token_secret = @api_token.token_secret
          @http_client.headers["Authorization"] = "Token #{@token_secret}"
          @http_client.headers["Content-Type"] = "application/json"
        end

        scenario 'getting the SMTP-settings is forbidden ' do
          get = @http_client.get "/admin/settings/smtp/"
          expect(get).not_to be_success
          expect(get.status).to be== 403
        end

        scenario 'updating the SMTP-settings is forbidden' do
          patch = @http_client.patch "/admin/settings/smtp/", {enabled: true}.to_json
          expect(patch).not_to be_success
          expect(patch.status).to be== 403
        end
      end
    end
  end
end
