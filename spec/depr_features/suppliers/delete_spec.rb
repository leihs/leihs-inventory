require 'spec_helper'
require 'pry'


feature 'Manage suppliers', type: :feature do

  context ' an admin' do

    before :each do
      @admin = FactoryBot.create :admin
      @suppliers = 10.times.map { FactoryBot.create :supplier }
      sign_in_as @admin
    end

    scenario 'deleting a supplier' do

      visit '/admin/'
      within 'aside nav' do
        click_on 'Suppliers'
      end

      @suppliers.each { |supplier| expect(page).to have_content supplier.name }

      click_on @suppliers.first.name
      @supplier_path = current_path

      click_on 'Delete' # delete page
      within '.modal' do
        click_on 'Delete' # submit / confirm
      end

      wait_until { current_path ==  "/admin/suppliers/" }

      @suppliers.drop(1).each { |supplier| expect(page).to have_content supplier.name }

      expect(page).not_to have_content @suppliers.first.name

    end

  end

end
