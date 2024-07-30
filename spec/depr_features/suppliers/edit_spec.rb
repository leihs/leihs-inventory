require 'spec_helper'
require 'pry'

feature 'Manage suppliers', type: :feature do

  let(:name) { Faker::Company.name}
  let(:note) { Faker::Markdown.sandwich }

  before :each do
    @admin = FactoryBot.create :admin
    @suppliers = 10.times.map do
      supplier = FactoryBot.create(:supplier)
      5.times.map do
        FactoryBot.create(:item, supplier: supplier)
      end
      5.times.map do
        FactoryBot.create(:item, responsible: nil, supplier: supplier)
      end
      supplier
    end
    @supplier = @suppliers.sample
  end

  context "an admin via the UI" do
    before(:each){ sign_in_as @admin }

    scenario 'edits a supplier' do
      visit '/admin/'
      within 'aside nav' do
        click_on 'Suppliers'
      end
      click_on @supplier.name
      @supplier_path = current_path

      # check items
      within("table.items") do
        @supplier.items.each do |item|
          within("tr.item", text: item.inventory_code) do
            expect(current_scope).to have_content item.responsible.name
            expect(current_scope).to have_selector "a[href='/admin/inventory-pools/#{item.inventory_pool_id}']"
            expect(current_scope).to have_content item.inventory_code
            expect(current_scope).to have_selector "a[href='/manage/#{item.inventory_pool_id}/items/#{item.id}/edit']"
            expect(current_scope).to have_content item.leihs_model.name
            expect(current_scope).to have_selector "a[href='/manage/#{item.inventory_pool_id}/models/#{item.leihs_model.id}/edit']"
          end
        end
      end

      click_on 'Edit'
      fill_in 'name', with: name
      fill_in 'note', with: note
      click_on 'Save'
      wait_until { all(".modal").empty? }
      wait_until {current_path == @supplier_path}
      wait_until { all(".wait-component").empty? }

      expect(page.text).to have_content name
      expect(page.text).to have_content note

      within("aside nav") do
        click_on "Suppliers"
      end

      wait_until { current_path ==  "/admin/suppliers/" }
      expect(page).to have_content name
    end

  end

end
