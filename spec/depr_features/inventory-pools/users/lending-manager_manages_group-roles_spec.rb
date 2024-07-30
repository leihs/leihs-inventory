require 'spec_helper'
require 'pry'


feature 'Manage inventory-pool groups', type: :feature do

  context ' an admin, a pool, an lending_manager, and a group' do


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
        FactoryBot.create :direct_access_right, user_id: @lending_manager.id,
          inventory_pool_id: @pool.id, role: 'lending_manager'
        @group = FactoryBot.create :group

        prepare_http_client
      end

      scenario 'assigning the direct role lending_manager to a user works' do

        gar = GroupAccessRight.find group_id: @group.id
        expect(gar).not_to be

        payload= '{"customer":true,"group_manager":true,"lending_manager":true,"inventory_manager":false}'
        resp  = http_client.put "/admin/inventory-pools/#{@pool.id}/groups/#{@group.id}/roles", payload

        expect(resp.status).to be< 300

        gar = GroupAccessRight.find group_id: @group.id
        expect(gar).to be

        expect(gar.role).to be== 'lending_manager'

      end


      scenario 'assigning the direct role inventory_manager to a user is forbidden ' do

        gar = GroupAccessRight.find group_id: @group.id
        expect(gar).not_to be

        payload= '{"customer":true,"group_manager":true,"lending_manager":true,"inventory_manager":true}'
        resp  = http_client.put "/admin/inventory-pools/#{@pool.id}/groups/#{@group.id}/roles", payload

        expect(resp.status).to be== 403

        gar = GroupAccessRight.find group_id: @group.id
        expect(gar).not_to be

      end


      scenario 'restricting existing inventory_manager rights of a user is forbidden ' do

        gar = GroupAccessRight.find group_id: @group.id
        expect(gar).not_to be

        FactoryBot.create :group_access_right, group_id: @group.id,
          inventory_pool_id: @pool.id, role: 'inventory_manager'

        payload= '{"customer":true,"group_manager":true,"lending_manager":true,"inventory_manager":false}'
        resp  = http_client.put "/admin/inventory-pools/#{@pool.id}/groups/#{@group.id}/roles", payload

        expect(resp.status).to be== 403

        gar = GroupAccessRight.find group_id: @group.id
        expect(gar).to be
        expect(gar.role).to be== 'inventory_manager'

      end
    end
  end
end
