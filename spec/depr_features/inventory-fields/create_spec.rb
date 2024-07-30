require 'spec_helper'
require 'pry'

feature 'Create inventory-fields', type: :feature do

  before :each do
    @admin = FactoryBot.create(:admin, password: "password")
    @fields = Field.all
  end

  let(:label) { Faker::Lorem.words(number: 2).join(" ") }
  let(:attribute) { Faker::Lorem.words(number: 2).join("_") }

  context "an admin via the UI" do
    before(:each){ sign_in_as @admin }

    scenario 'creates a dynamic field' do
      visit '/admin/'
      within find("aside nav", match: :first) do
        click_on "Fields"
      end
      click_on_first 'Add Field'

      find("input#active").click
      fill_in 'Label', with: label
      find(:xpath, "//input[@id='data:attribute']").set attribute
      find(:xpath, "//input[@id='data:forPackage']").click
      find(:xpath, "//input[@id='data:permissions:owner']").click
      select("inventory_manager", from: "Minimum role required for view")

      within '.modal' do
        select("License", from: "Target")
        select("Checkbox", from: "Type")
      end
      click_on("+")

      label_1 = Faker::Lorem.word
      value_1 = label_1.downcase
      label_2 = Faker::Lorem.word
      value_2 = label_2.downcase

      find(".form-group", text: "data:type").all(".col-5 input")[0].set label_1
      find(".form-group", text: "data:type").all(".col-4 input")[0].set value_1
      find(".form-group", text: "data:type").all(".col-5 input")[1].set label_2
      find(".form-group", text: "data:type").all(".col-4 input")[1].set value_2

      click_on 'Save'

      wait_until { all(".modal").empty? }
      wait_until { all(".wait-component").empty? }

      click_on 'Edit'

      expect(find("input#active")).to be_checked
      expect(find(:xpath, "//input[@id='data:label']").value).to eq label
      expect(find(:xpath, "//input[@id='data:forPackage']")).to be_checked
      expect(find(:xpath, "//input[@id='data:permissions:owner']")).to be_checked
      expect(find(:xpath, "//select[@id='data:permissions:role']").value).to eq "inventory_manager"
      expect(find("#none")).to be_checked
      expect(find(:xpath, "//select[@id='data:target_type']").value).to eq "license"
      expect(find(:xpath, "//select[@id='data:type']").value).to eq "checkbox"

      expect(find(".form-group", text: "data:type").all(".col-5 input")[0].value).to eq label_1
      expect(find(".form-group", text: "data:type").all(".col-4 input")[0].value).to eq value_1
      expect(find(".form-group", text: "data:type").all(".col-5 input")[1].value).to eq label_2
      expect(find(".form-group", text: "data:type").all(".col-4 input")[1].value).to eq value_2


      click_on 'Save'
      
      wait_until { all(".modal").empty? }
      wait_until { all(".wait-component").empty? }

      within find("aside nav", match: :first) do
        click_on "Fields"
      end

      wait_until { current_path ==  "/admin/inventory-fields/" }
      expect(page).to have_content label

      expect(DisabledField.where(field_id: "properties_#{attribute}").count)
        .to eq InventoryPool.where(is_active: false).count
    end
  end
end
