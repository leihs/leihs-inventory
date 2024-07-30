require 'spec_helper'
require 'pry'


feature 'Manage inventory-pool users ', type: :feature do

  context ' an admin, a pool, an lending_manager, and  several users ' do


    let :http_client do
      plain_faraday_client
    end

    let :prepare_http_client do
      @api_token = FactoryBot.create :api_token,
        user_id: @lending_manager.id
      @token_secret = @api_token.token_secret
      http_client.headers["Authorization"] = "Token #{@token_secret}"
      http_client.headers["Content-Type"] = "application/json"
    end


    context 'as a lending_manager' do

      before :each do
        @admin = FactoryBot.create :admin
        @pool =  FactoryBot.create :inventory_pool
        @lending_manager = FactoryBot.create :user
        FactoryBot.create :access_right, user: @lending_manager,
          inventory_pool: @pool, role: 'lending_manager'
        @user = FactoryBot.create :user

        prepare_http_client
      end

      scenario 'assigning the direct role lending_manager to a user works' do

        dar = DirectAccessRight.find user_id: @user.id
        expect(dar).not_to be

        payload= '{"customer":true,"group_manager":true,"lending_manager":true,"inventory_manager":false}'
        resp  = http_client.put "/admin/inventory-pools/#{@pool.id}/users/#{@user.id}/direct-roles", payload

        expect(resp.status).to be< 300

        dar = DirectAccessRight.find user_id: @user.id
        expect(dar).to be

        expect(dar.role).to be== 'lending_manager'

      end


      scenario 'assigning the direct role inventory_manager to a user is forbidden ' do

        dar = DirectAccessRight.find user_id: @user.id
        expect(dar).not_to be

        payload= '{"customer":true,"group_manager":true,"lending_manager":true,"inventory_manager":true}'
        resp  = http_client.put "/admin/inventory-pools/#{@pool.id}/users/#{@user.id}/direct-roles", payload

        expect(resp.status).to be== 403

        dar = DirectAccessRight.find user_id: @user.id
        expect(dar).not_to be

      end


      scenario 'restricting existing inventory_manager rights of a user is forbidden ' do

        dar = DirectAccessRight.find user_id: @user.id
        expect(dar).not_to be

        FactoryBot.create :direct_access_right, user_id: @user.id,
          inventory_pool_id: @pool.id, role: 'inventory_manager'

        payload= '{"customer":true,"group_manager":true,"lending_manager":true,"inventory_manager":false}'
        resp  = http_client.put "/admin/inventory-pools/#{@pool.id}/users/#{@user.id}/direct-roles", payload

        expect(resp.status).to be== 403

        dar = DirectAccessRight.find user_id: @user.id
        expect(dar).to be
        expect(dar.role).to be== 'inventory_manager'

      end
    end
  end
end
