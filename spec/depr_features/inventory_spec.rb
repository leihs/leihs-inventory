require 'spec_helper'
require 'pry'

feature 'Inventory' do
  context 'an admin via the UI' do
    before :each do
      @admin = FactoryBot.create :admin
      sign_in_as @admin
    end
    scenario 'Export inventory links exist' do
      click_on 'Inventory-Export'
      expect(all('a', text: 'CSV')).not_to be_empty
    end
  end
end
