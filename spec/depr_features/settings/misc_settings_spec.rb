require 'spec_helper'
require 'pry'

feature 'SMTP-Settings' do

  context 'a plain admin exist' do
    before :each do
      @plain_admin = FactoryBot.create :admin
    end

    context 'a plain_admin' do
      before(:each){@user = @plain_admin}

      context 'via the UI' do
        before(:each){sign_in_as @user}

        scenario 'updates the Miscellaneous-Settings' do
          within "aside nav" do
            click_on "Settings"
            click_on "Miscellaneous"
          end

          wait_until { page.has_content? "logo_url" }
          click_on "Edit"
          fill_in "logo_url", with: "https://my-server/leihs-logo.png"
          fill_in "documentation_link", with: "https://my-server/leihs-docs"
          fill_in "contract_lending_party_string", with: "Me"
          fill_in "custom_head_tag", with: "My Header ???"
          fill_in "time_zone", with: "Berlin"
          fill_in "local_currency_string", with: "CHF"
          fill_in "maximum_reservation_time", with: "500"
          fill_in "timeout_minutes", with: "21"
          check "deliver_received_order_notifications"
          check "include_customer_email_in_contracts"
          fill_in "email_signature", with: "Your awesome Lending Desk"
          check "lending_terms_acceptance_required_for_order"
          fill_in "lending_terms_url", with: "https://example.org/fileadmin/leihs-terms-2000-01-01.pdf"
          fill_in "home_page_image_url", with: "https://example.org/image.jpg"
          
          click_on "Save"
          sleep 0.5
          wait_until{ all(".modal").empty? }
          visit current_url
          wait_until { page.has_content? "logo_url" }
          expect(page.text).to have_content 'https://my-server/leihs-logo.png'
          expect(page.text).to have_content "https://my-server/leihs-docs"
          expect(page.text).to have_content 'Me'
          expect(page.text).to have_content 'My Header ???'
          expect(page.text).to have_content 'Berlin'
          expect(page.text).to have_content 'CHF'
          expect(page.text).to have_content '500'
          expect(page.text).to have_content '21'
          within 'tr .deliver-received-order-notifications' do 
            expect(page.text).to have_content 'true'
          end
          within 'tr .include-customer-email-in-contracts' do 
            expect(page.text).to have_content 'true'
          end
          within 'tr .lending-terms-acceptance-required-for_order' do 
            expect(page.text).to have_content 'true'
          end
          within 'tr .show-contact-details-on-customer-order' do 
            expect(page.text).to have_content 'false'
          end
          expect(page.text).to have_content 'Your awesome Lending Desk'
          expect(page.text).to have_content 'https://example.org/fileadmin/leihs-terms-2000-01-01.pdf'
          expect(page.text).to have_content 'https://example.org/image.jpg'
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

          get = @http_client.get "/admin/settings/misc/"
          expect(get).to be_success
          expect(get.body["deliver_received_order_notifications"]).to be false

          patch = @http_client.patch "/admin/settings/misc/", {deliver_received_order_notifications: true}.to_json
          expect(patch).to be_success
          expect(patch.body["deliver_received_order_notifications"]).to be true

          get = @http_client.get "/admin/settings/misc/"
          expect(get).to be_success
          expect(get.body["deliver_received_order_notifications"]).to be true

        end

      end

    end
  end
end
