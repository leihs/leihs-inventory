require 'spec_helper'
require 'pry'


feature 'Manage inventory-fields', type: :feature do

  context ' an admin' do

    before :each do
      @admin = FactoryBot.create(:admin)
      @fields = Field.all
      @item = FactoryBot.create(:item)
      sign_in_as @admin

      visit '/admin/'
      within find("aside nav", match: :first) do
        click_on "Fields"
      end
    end

    scenario 'is able to delete an inventory-field' do

      # @fields.each { |field| expect(page).to have_content field.id }

      @field = @fields.detect { |f| not(f.data[:required]) and f.dynamic }

      click_on @field.id
      @field_path = current_path

      click_on 'Delete' # delete page
      within '.modal' do
        click_on 'Delete' # submit / confirm
      end

      wait_until { current_path ==  "/admin/inventory-fields/" }
      select("1000", from: "Per page")

      @fields.reject { |f| f.id = @field.id }.each do |field|
        expect(page).to have_content field.id 
      end

      expect(page).not_to have_content @field.id
    end

    context 'cannot delete' do
      scenario 'a core field' do
        @field = @fields.detect { |f| not f.dynamic }

        click_on @field.id
        @field_path = current_path

        expect(page).not_to have_content /delete/i
      end

      scenario 'a dynamic used field' do
        @field = @fields.detect { |f| f.dynamic }
        @item.update(properties: { @field.data["attribute"].last => Faker::Lorem.word })

        click_on @field.id
        @field_path = current_path

        expect(page).not_to have_content /delete/i

      end
    end
  end
end
