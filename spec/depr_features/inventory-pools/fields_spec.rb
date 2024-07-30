require 'spec_helper'
require 'pry'

feature 'Fields status in pool', type: :feature do

  before :each do
    @admin = FactoryBot.create :admin
    @pools = 10.times.map { FactoryBot.create :inventory_pool }
    @pool = @pools[0]
    @other_pool = @pools[1]
    @fields = Field.all
    
    # inactivate 1 field (which must not appear in ui then)
    @inactive_field = @fields.detect { |f| f.id == 'properties_mac_address' }
    @inactive_field.active = false
    @inactive_field.save 

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
        click_on 'Inventory Pools'
      end
      click_on @pool.name
      within('.nav-tabs') { click_on 'Fields' }
    end

    scenario 'can see the fields and edit the field statuses' do
      rows = all('tr.field')
      expect(rows.length).to eq 45 # vs. 46 fields, but 1 one of them is inactive, thus not displayed
      expect(rows[0].text).to eq '1 attachments Attachments Item+License Enabled'
      expect(rows[1].text).to eq '2 building_id Building * Item Enabled'
      expect(rows[2].text).to eq '3 inventory_code Inventory Code * Item+License Enabled'
      
      click_on 'Edit'
      expect(page).to have_css('.modal')
      click_on_toggle 'attachments-switch'
      click_on_toggle 'building_id-switch'
      click_on 'Save'

      expect(page).to have_selector('tr.field', count: 45)
      rows = all('tr.field')
      expect(rows[0].text).to eq '1 attachments Attachments Item+License Disabled'
      expect(rows[1].text).to eq '2 building_id Building * Item Enabled' # <- not changed because locked

      click_on 'Edit'
      expect(page).to have_css('.modal')
      click_on_toggle 'attachments-switch'
      click_on 'Save'

      expect(page).to have_selector('tr.field', count: 45)
      rows = all('tr.field')
      expect(rows[0].text).to eq '1 attachments Attachments Item+License Enabled'
    end

  end

  context 'a inventory manager via the ui' do
    before(:each) do 
      @manager = FactoryBot.create :user
      FactoryBot.create :access_right, user: @manager, inventory_pool: @pool, role: 'inventory_manager'
      sign_in_as @manager
      visit '/admin/'
      within('aside nav') do
        click_on 'Inventory Pools'
      end
      click_on @pool.name
      within('.nav-tabs') { click_on 'Fields' }
    end

    scenario 'can see the fields and edit the field statuses in their pool' do
      rows = all('tr.field')
      expect(rows.length).to eq 45 # vs. 46 fields, but 1 one of them is inactive, thus not displayed
      expect(rows[0].text).to eq '1 attachments Attachments Item+License Enabled'
      
      click_on 'Edit'
      expect(page).to have_css('.modal')
      click_on_toggle 'attachments-switch'
      click_on 'Save'

      expect(page).to have_selector('tr.field', count: 45)
      rows = all('tr.field')
      expect(rows[0].text).to eq '1 attachments Attachments Item+License Disabled'
    end

    scenario 'can not see fields in other pools' do
      visit "/admin/inventory-pools/#{@other_pool.id}/fields"
      expect(page).to have_content 'Request ERROR 403'
    end
  end

  context 'a lending manager via the ui' do
    before(:each) do 
      @manager = FactoryBot.create :user
      FactoryBot.create :access_right, user: @manager, inventory_pool: @pool, role: 'lending_manager'
      sign_in_as @manager
      visit '/admin/'
      within('aside nav') do
        click_on 'Inventory Pools'
      end
      click_on @pool.name
      within('.nav-tabs') { click_on 'Fields' }
    end

    scenario 'can see the fields in their pool, but not edit the field statuses' do
      rows = all('tr.field')
      expect(rows.length).to eq 45 # vs. 46 fields, but 1 one of them is inactive, thus not displayed
      expect(rows[0].text).to eq '1 attachments Attachments Item+License Enabled'
      expect(page).not_to have_button('Edit')

      visit "/admin/inventory-pools/#{@pool.id}/fields?action=edit-fields"
      expect(page).to have_content('Edit Field Status')
      click_on_toggle 'attachments-switch'
      click_on 'Save'
      expect(page).to have_content 'Request ERROR 403'
      click_on 'Dismiss'
      click_on 'Cancel'
      visit "/admin/inventory-pools/#{@pool.id}/fields"

      expect(page).to have_selector('tr.field', count: 45)
      rows = all('tr.field')
      expect(rows[0].text).to eq '1 attachments Attachments Item+License Enabled' # not changed!
    end

    scenario 'can not see fields in other pools' do
      visit "/admin/inventory-pools/#{@other_pool.id}/fields"
      expect(page).to have_content 'Request ERROR 403'
    end
  end

end
