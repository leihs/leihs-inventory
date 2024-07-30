require 'spec_helper'
require 'pry'

feature 'Managing suppliers:', type: :feature do

  before :each do
    @admins = 3.times.map { FactoryBot.create :admin }
    @pools = 5.times.map { FactoryBot.create :inventory_pool }
    @suppliers = 10.times.map do 
      supplier = FactoryBot.create(:supplier)
      rand(10).times do
        FactoryBot.create(:item,
                          owner: @pools.sample,
                          responsible: @pools.sample,
                          supplier: supplier)
      end
      supplier
    end
    sign_in_as @admins.sample
    within 'aside nav' do
      click_on "Suppliers"
    end
  end

  context 'an admin via the UI' do
    scenario "can see and click all suppliers" do
      within("table.suppliers tbody") do
        @suppliers.each do |supplier|
          within("tr", text: supplier.name) do
            expect(current_scope).to have_selector("a[href='/admin/suppliers/#{supplier.id}']")
          end
        end
      end
    end

    describe 'searching for a supplier ' do
      before :each do
        @search_supplier = @suppliers.sample
        @other_suppliers= @suppliers - [@search_supplier]
      end

      scenario 'searching by name works' do
        sup_name = @search_supplier.name
        term = sup_name[1..(sup_name.length - 2)]
        fill_in 'Search', with: term
        wait_until{all( "table.suppliers tbody tr").count == 1 }
        expect(page).to have_content @search_supplier.name
        @other_suppliers.each do |other_supplier|
          expect(page).not_to have_content other_supplier.name
        end
      end

      scenario 'filtering by pools works' do
        pool = @pools.sample
        suppliers = pool.items.map(&:supplier).uniq

        binding.pry
        select pool.name, from: 'Inventory Pool'
        wait_until { not page.has_content? "Please wait" }
        expect(all("table.suppliers tbody tr").count).to eq suppliers.count
        within "table.suppliers tbody" do
          suppliers.each do |supplier|
            expect(current_scope).to have_content supplier.name
          end
        end
      end
    end
  end
end
