require 'spec_helper'
require 'pry'

feature 'Field status by pool', type: :feature do

  before :each do
    @admin = FactoryBot.create :admin
    @pool1 = FactoryBot.create :inventory_pool, name: "Pool 1"
    @pool2 = FactoryBot.create :inventory_pool, name: "Pool 2"
    @pool3 =  FactoryBot.create :inventory_pool, name: "Pool 3", is_active: false
    @fields = Field.all
    @an_optional_field = @fields.detect { |f| f.id == 'attachments' }
    @a_required_field = @fields.detect { |f| f.id == 'inventory_code' }

    # check assumptions about data
    expect(@fields.length == 46)
    expect(@fields.detect { |f| f.id == 'attachments' }.data['required']).to be_falsy
    expect(@fields.detect { |f| f.id == 'building_id' }.data['required']).to be_truthy
  end

  context 'an admin via the UI' do

    before(:each) do
      sign_in_as @admin
      visit '/admin/'
      within('aside nav') do
        click_on 'Fields'
      end
    end

    scenario 'can see all active pools and edit the field\'s status' do
      click_on @an_optional_field.id
      expect(page).to have_content 'Field status by inventory pool'
      
      rows = all('tr.pool-row')
      expect(rows.length).to eq 2 # active pools only 
      expect(rows[0].text).to eq '1 P1 Pool 1 Enabled'
      expect(rows[1].text).to eq '2 P2 Pool 2 Enabled'
      
      click_on 'Edit field status'
      expect(page).to have_css('.modal')
      click_on_toggle "#{@pool1.id}-switch"
      click_on 'Save'
      
      expect(page).to have_content 'Field status by inventory pool'
      rows = all('tr.pool-row')
      expect(rows[0].text).to eq '1 P1 Pool 1 Disabled'
      expect(rows[1].text).to eq '2 P2 Pool 2 Enabled'

      click_on 'Edit field status'
      expect(page).to have_css('.modal')
      click_on_toggle 'all-switch'
      click_on 'Save'

      expect(page).to have_content 'Field status by inventory pool'
      rows = all('tr.pool-row')
      expect(rows[0].text).to eq '1 P1 Pool 1 Disabled'
      expect(rows[1].text).to eq '2 P2 Pool 2 Disabled'
    end

    scenario 'can not the edit a required field\'s status' do
      click_on @a_required_field.id
      expect(page).to have_content 'Field status by inventory pool'
      expect(page).to have_content '(not editable because required fields can not be disabled)'
      expect(page).to have_button "Edit field status", disabled: true
    end

  end

  context 'a inventory manager via the ui' do
    scenario 'can not access fields' do
      @manager = FactoryBot.create :user
      FactoryBot.create :access_right, user: @manager, inventory_pool: @pool1, role: 'inventory_manager'
      sign_in_as @manager
      visit '/admin/inventory-fields/'
      expect(page).to have_content 'Request ERROR 403'
    end
  end

  context 'a lending manager via the ui' do
    scenario 'can not access fields' do
      @manager = FactoryBot.create :user
      FactoryBot.create :access_right, user: @manager, inventory_pool: @pool1, role: 'lending_manager'
      sign_in_as @manager
      visit '/admin/inventory-fields/'
      expect(page).to have_content 'Request ERROR 403'
    end
  end

end
