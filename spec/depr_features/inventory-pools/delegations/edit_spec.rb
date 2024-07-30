require 'spec_helper'
require 'pry'

shared_examples :edit do
  scenario 'editing a delegation works' do
    resp_user = @users.sample
    click_on 'Inventory Pools'
    click_on @pool.name
    click_on 'Delegations'
    fill_in 'Search', with: @delegation.firstname
    click_on @delegation.firstname
    within('section.info') { 
      click_on 'Edit' 
    }
    fill_in :name, with: 'New-Name'
    uncheck :pool_protected
    click_on 'Choose responsible user'
    fill_in 'Search', with: resp_user.email
    expect(page).to have_css('table tbody tr', count: 1)
    within('tr', text: resp_user.email) do
      click_on 'Choose user'
    end
    click_on 'Save'
    expect(page).to have_content 'New-Name'
    expect(page).to have_content resp_user.email
    expect(find('tr', text: 'Protected').text).to have_content 'no'
    within('section.info') { 
      click_on 'Edit' 
    }
    check :pool_protected
    click_on 'Save'
    expect(find('tr', text: 'Protected').text).to have_content 'yes'
  end
end

feature 'Editing a delegation' do

  before :each do
    @admin = FactoryBot.create :admin
    @users = 10.times.map{ FactoryBot.create :user }
  end

  context 'a inventory pool, and some delegations exist' do
    before(:each) do
      @pool = FactoryBot.create :inventory_pool
      @delegations = 100.times.map do
        delegation = FactoryBot.create :delegation
        FactoryBot.create :access_right,
          user_id: delegation.id,
          inventory_pool_id: @pool.id,
          role: 'customer'
        delegation
      end
      @delegation = @delegations.sample
    end

    context 'as an admin' do
      before(:each){ @user = @admin }
      context 'via the UI' do
        before(:each){sign_in_as @user}
        include_examples :edit
      end
    end

    context 'as a lending_manager of the pool ' do
      before :each do
        @user = FactoryBot.create :user
        FactoryBot.create :access_right, user: @user,
          inventory_pool: @pool, role: 'lending_manager'
      end
      context 'via the UI' do
        before(:each) { sign_in_as @user}
        include_examples :edit
      end
    end
  end
end
