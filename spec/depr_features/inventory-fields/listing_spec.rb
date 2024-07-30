require 'spec_helper'
require 'pry'

feature 'Managing inventory-fields:', type: :feature do

  before :each do
    @admin = FactoryBot.create(:admin, password: "password")
    @fields = Field.all
    sign_in_as @admin
    within('aside nav') do
      click_on "Fields"
    end
  end

  context 'an admin via the UI' do
    scenario "can see and click all inventory-fields" do
      within("table.inventory-fields tbody") do
        @fields.each do |field|
          within("tr", text: field.id, match: :prefer_exact) do
            expect(current_scope).to have_selector("a[href='/admin/inventory-fields/#{field.id}']")
          end
        end
      end
    end

    describe 'searching for a room ' do
      before :each do
        @search_field = @fields.sample
        @other_fields = @fields - [@search_field]
      end

      scenario 'searching by name works' do
        field_id = @search_field.id
        term = field_id[1..(field_id.length - 2)]
        fill_in 'Search', with: term
        wait_until{all( "table.inventory-fields tbody tr").count == 1 }
        expect(page).to have_content @search_field.id
        @other_fields.each do |field|
          expect(page).not_to have_content field.id
        end
      end

      scenario 'filtering by configurable attribute works' do
        select 'yes', from: 'Configurable'
        wait_until { not page.has_content? "Please wait" }
        select("1000", from: "Per page")
        wait_until { all("table.inventory-fields tbody tr").count > 1 }
        
        fields = @fields.select(&:dynamic)
        expect(all("table.inventory-fields tbody tr").count).to eq fields.count

        within "table.inventory-fields tbody" do
          fields.each do |field|
            expect(current_scope).to have_content field.id
          end
        end
      end

      scenario 'filtering by active attribute works' do
        select 'yes', from: 'Active'
        wait_until { not page.has_content? "Please wait" }
        select("1000", from: "Per page")
        fields = @fields.select(&:active)
        expect(all("table.inventory-fields tbody tr").count).to eq fields.count
        within "table.inventory-fields tbody" do
          fields.each do |field|
            expect(current_scope).to have_content field.id
          end
        end
      end

      scenario 'filtering by form-group attribute works' do
        form_groups = @fields.map { |f| f.data["group"] }.uniq.compact
        form_group = form_groups.sample
        select form_group, from: 'Form Group'
        wait_until { not page.has_content? "Please wait" }
        select("1000", from: "Per page")
        wait_until { all("table.inventory-fields tbody tr").count > 1 }
        fields = @fields.select { |f| f.data["group"] == form_group }
        expect(all("table.inventory-fields tbody tr").count).to eq fields.count
        within "table.inventory-fields tbody" do
          fields.each do |field|
            expect(current_scope).to have_content field.id
          end
        end
      end

      scenario 'filtering by target-type attribute works' do
        select "Item", from: 'Target Type'
        wait_until { not page.has_content? "Please wait" }
        select("1000", from: "Per page")
        fields = @fields.select { |f| f.data["target_type"] == "item" }
        expect(all("table.inventory-fields tbody tr").count).to eq fields.count
        within "table.inventory-fields tbody" do
          fields.each do |field|
            expect(current_scope).to have_content field.id
          end
        end
      end
    end
  end
end
