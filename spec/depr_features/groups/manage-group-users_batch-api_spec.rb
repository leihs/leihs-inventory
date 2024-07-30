require 'spec_helper'
require 'pry'

feature 'Managing group users via API batch put', type: :feature do

  context 'an system admin, one group and some prepared users exist' do

    let :http_client do
      plain_faraday_client
    end

    let :prepare_http_client do
      @api_token = FactoryBot.create :api_token, user_id: @admin.id,
        scope_admin_read: true, scope_admin_write: true,
        scope_system_admin_read: true, scope_system_admin_write: true
      @token_secret = @api_token.token_secret
      http_client.headers["Authorization"] = "Token #{@token_secret}"
      http_client.headers["Content-Type"] = "application/json"
    end

    before :each do
      @admin = FactoryBot.create :system_admin

      @group = FactoryBot.create :group

      @user_to_be_added_by_id = FactoryBot.create :user
      @user_to_be_added_by_email = \
        FactoryBot.create :user, email: 'user@example.com'
      @user_to_be_added_by_org_id = \
        FactoryBot.create :user, org_id: '12345-x'

      @group_users = 15.times.map do
        user = FactoryBot.create :user
        database[:groups_users].insert(group_id: @group.id, user_id: user[:id])
        user
      end.to_set

      @to_be_kept_user = @group_users.first

      @other_users = 15.times.map do
        FactoryBot.create :user
      end.to_set

      sign_in_as @admin

      prepare_http_client
    end

    scenario 'adding and removing users via ids' do

      # verify that currently the 15 group_users are given via the API
      get_resp = http_client.get "/admin/groups/#{@group.id}/users/?per-page=100"
      user_ids = Set.new get_resp.body["users"].map{|u| u["id"]}
      expect(user_ids).to be== Set.new(@group_users.map(&:id))

      # update the group-users
      data = {ids: [@user_to_be_added_by_id.id, @to_be_kept_user.id]}
      put_resp = http_client.put "/admin/groups/#{@group.id}/users/", data.to_json
      expect(put_resp.status).to be== 200

      # verify that exactly the updated users are in the group
      get_resp = http_client.get "/admin/groups/#{@group.id}/users/"
      user_ids = Set.new get_resp.body["users"].map{|u| u["id"]}
      expect(user_ids.count).to be== 2
      expect(user_ids).to include @to_be_kept_user.id
      expect(user_ids).to include @user_to_be_added_by_id.id
    end


    scenario 'managing users via org_ids unprocessable ' do

      # verify that currently the 15 group_users are given via the API
      get_resp = http_client.get "/admin/groups/#{@group.id}/users/?per-page=100"
      user_ids = Set.new get_resp.body["users"].map{|u| u["id"]}
      expect(user_ids).to be== Set.new(@group_users.map(&:id))

      # update the group-users
      data = { emails: [@user_to_be_added_by_email.email] }
      put_resp = http_client.put "/admin/groups/#{@group.id}/users/", data.to_json
      expect(put_resp.status).to be== 422

    end


    scenario 'managing users via emails is unprocessable ' do

      # verify that currently the 15 group_users are given via the API
      get_resp = http_client.get "/admin/groups/#{@group.id}/users/?per-page=100"
      user_ids = Set.new get_resp.body["users"].map{|u| u["id"]}
      expect(user_ids).to be== Set.new(@group_users.map(&:id))

      # update the group-users
      data = { org_ids: [@user_to_be_added_by_org_id.org_id]}
      put_resp = http_client.put "/admin/groups/#{@group.id}/users/", data.to_json
      expect(put_resp.status).to be== 422

    end

  end

end

