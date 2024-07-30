require 'spec_helper'
require 'pry'


shared_examples :listing do
  scenario "the Inventory Pools listing displays the correct number of delegations per pool" do
    click_on 'Inventory Pools'
    within('tr', text: "Pool 1") do
      expect(find("td.delegations_count")).to have_content "2"
    end
    within('tr', text: "Pool 2") do
      expect(find("td.delegations_count")).to have_content "1"
    end
  end
end

shared_examples :overview do
  scenario "the delegation overview in the pool shows the member delegations by default" do
    click_on 'Inventory Pools'
    click_on 'Pool 1'
    click_on 'Delegations'
    expect(page).to have_content 'Delegation 1'
    expect(page).to have_content 'Delegation 2'
    expect(page).not_to have_content 'Delegation 3'
    expect(page).not_to have_content 'Delegation 4'
  end
end

shared_examples :add_protected_impossible do
  scenario 'can not add a protected delegation to the pool' do
    click_on 'Inventory Pools'
    click_on 'Pool 1'
    click_on 'Delegations'
    select 'members and non-members', from: 'Membership'
    wait_until do
      page.has_content? 'Delegation 3'
    end
    expect(page).not_to have_content 'Delegation 4'
  end
end

shared_examples :add_unprotected do
  scenario 'can add an unprotected delegation to the pool' do
    click_on 'Inventory Pools'
    click_on 'Pool 1'
    click_on 'Delegations'
    select 'members and non-members', from: 'Membership'
    within('tr', text: 'Delegation 3'){ click_on 'Add'}
    select 'members', from: 'Membership'
    expect(page).not_to have_content 'Delegation 4'
    expect(page).to have_content 'Delegation 3'
    # after Delegation 3 has been added there must be a delete button
    within('tr', text: 'Delegation 3') do
      find('td', text: 'Delete')
    end
  end
end

shared_examples :delete do
  scenario 'can delete a delegation' do
    click_on 'Inventory Pools'
    click_on 'Pool 1'
    click_on 'Delegations'
    select 'members', from: 'Membership'
    within('tr', text: 'Delegation 1'){ click_on 'Delete' }
    expect(page).not_to have_content 'Delegation 1'
    select 'non-members', from: 'Membership'
    visit current_path
    wait_until{first("tr")}
    expect(page).not_to have_content 'Delegation 1'
  end
end

shared_examples :remove do
  scenario 'can remove a delegation from the pool' do
    click_on 'Inventory Pools'
    click_on 'Pool 1'
    click_on 'Delegations'
    select 'members', from: 'Membership'
    within('tr', text: 'Delegation 2'){ click_on 'Remove' }
    sleep 1
    expect(page).not_to have_content 'Delegation 2'
    select 'non-members', from: 'Membership'
    sleep 1
    expect(page).to have_content 'Delegation 2'
  end
end

shared_examples :delete_remove do
  scenario 'can invoke delete on a delegation with a reservation, it will be removed but not deleted' do
    FactoryBot.create :reservation,
      user_id: @delegation1.id,
      inventory_pool_id: @pool1.id
    click_on 'Inventory Pools'
    click_on 'Pool 1'
    click_on 'Delegations'
    select 'members', from: 'Membership'
    within('tr', text: 'Delegation 1'){ click_on 'Delete' }
    expect(page).not_to have_content 'Delegation 1'
    select 'non-members', from: 'Membership'
    wait_until { first("tr", text: 'Delegation 1') }
  end
end


feature 'Add, remove, delete delegations within a pool ', type: :feature do

  before :each do
    @admin = FactoryBot.create :admin
  end

  context 'two pools, and some delegations in various states exist' do

    before :each do

      @pool1 = FactoryBot.create :inventory_pool, name: "Pool 1"
      @pool2 = FactoryBot.create :inventory_pool, name: "Pool 2"

      @delgator1 = FactoryBot.create :user
      @delegation1 = FactoryBot.create :delegation,
        firstname: "Delegation 1 (in pool 1)",
        pool_protected: false,
        delegator_user_id: @delgator1.id
      FactoryBot.create :access_right, user: @delegation1,
        inventory_pool: @pool1, role: 'customer'

      @delgator2 = FactoryBot.create :user
      @delegation2 = FactoryBot.create :delegation,
        firstname: "Delegation 2 (in pool 1 and 2)",
        pool_protected: false,
        delegator_user_id: @delgator2.id
      FactoryBot.create :access_right, user: @delegation2,
        inventory_pool: @pool1, role: 'customer'
      FactoryBot.create :access_right, user: @delegation2,
        inventory_pool: @pool2, role: 'customer'

      @delgator3 = FactoryBot.create :user
      @delegation3 = FactoryBot.create :delegation,
        firstname: "Delegation 3 (in no pool, unprotected)",
        pool_protected: false,
        delegator_user_id: @delgator3.id

      @delgator4 = FactoryBot.create :user
      @delegation4 = FactoryBot.create :delegation,
        firstname: "Delegation 4 (in no pool, protected)",
        pool_protected: true,
        delegator_user_id: @delgator4.id

    end

    context 'an admin via the UI' do
      before(:each){sign_in_as @admin}

      include_examples :listing
      include_examples :overview
      include_examples :add_protected_impossible
      include_examples :add_unprotected
      include_examples :remove
      include_examples :delete
      include_examples :delete_remove

    end

    context 'as a lending_manager of pool 1' do

      before :each do
        @lending_manager = FactoryBot.create :user
        FactoryBot.create :access_right, user: @lending_manager,
          inventory_pool: @pool1, role: 'lending_manager'
      end

      context 'via the UI' do
        before(:each) { sign_in_as @lending_manager }

        include_examples :listing
        include_examples :overview
        include_examples :add_protected_impossible
        include_examples :add_unprotected
        include_examples :remove
        include_examples :delete
        include_examples :delete_remove

      end

      context 'via the API' do

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

        before(:each) {prepare_http_client}

        scenario 'adding a unprotected delegation to a managed pool works' do
          resp  = http_client.put "/admin/inventory-pools/#{@pool1.id}/delegations/#{@delegation3.id}"
          expect(resp.status).to be< 300
        end


        scenario 'adding a unprotected delegation to an unmanged pool is forbidden' do
          resp  = http_client.put "/admin/inventory-pools/#{@pool2.id}/delegations/#{@delegation3.id}"
          expect(resp.status).to be== 403
        end

      end

    end

  end

end
