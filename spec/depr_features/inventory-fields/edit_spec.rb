require 'spec_helper'
require 'pry'

feature 'Manage inventory-fields', type: :feature do

  before :each do
    @admin = FactoryBot.create(:admin, password: "password")
    @fields = Field.all
  end

  let(:label) { Faker::Lorem.words(number: 2).join(" ") }

  context "an admin via the UI" do
    before(:each){ sign_in_as @admin }

    scenario 'edits a required core field' do
      @required_core_field = @fields.detect do |f|
        not(f.dynamic) && f.data["required"]
      end

      visit '/admin/'
      click_on 'Fields'
      click_on @required_core_field.id
      @field_path = current_path

      click_on 'Edit'
      fill_in 'Label', with: label
      expect(find("input#active")).to be_disabled
      click_on 'Save'
      wait_until { all(".modal").empty? }
      wait_until { current_path == @field_path }
      wait_until { all(".wait-component").empty? }

      input_values = all("input").map(&:value).join(" ")
      expect(page.text + input_values).to have_content label

      within find("aside nav", match: :first) do
        click_on 'Fields'
      end
      wait_until { current_path ==  "/admin/inventory-fields/" }
      expect(page).to have_content label
    end

    scenario 'edits a not required active core field' do
      @not_required_active_core_field = @fields.detect do |f|
        not(f.dynamic) && not(f.data["required"]) && f.active
      end

      visit '/admin/'
      click_on 'Fields'
      click_on @not_required_active_core_field.id
      @field_path = current_path

      click_on 'Edit'
      fill_in 'Label', with: label
      find("input#active").click
      click_on 'Save'
      wait_until { all(".modal").empty? }
      wait_until { current_path == @field_path }
      wait_until { all(".wait-component").empty? }

      input_values = all("input").map(&:value).join(" ")
      expect(page.text + input_values).to have_content label
      expect(find("tr.active")).to have_content 'false'
      expect(page).to have_content label
    end

    scenario 'edits a dynamic field' do
      @dynamic_field = @fields.detect { |f| f.id == "properties_mac_address" }

      visit '/admin/'
      click_on 'Fields'
      click_on @dynamic_field.id
      @field_path = current_path

      click_on 'Edit'

      find("input#active").click
      fill_in 'Label', with: label
      find(:xpath, "//input[@id='data:forPackage']").click
      find(:xpath, "//input[@id='data:permissions:owner']").click
      select("inventory_manager", from: "Minimum role required for view")
      choose("Inventory")
      select("License", from: "Target")
      select("Select", from: "Type")
      click_on("+")

      label_1 = Faker::Lorem.word
      value_1 = label_1.downcase
      label_2 = Faker::Lorem.word
      value_2 = label_2.downcase

      find(".form-group", text: "data:type").all(".col-5 input")[0].set label_1
      find(".form-group", text: "data:type").all(".col-4 input")[0].set value_1
      find(".form-group", text: "data:type").all(".col-5 input")[1].set label_2
      find(".form-group", text: "data:type").all(".col-4 input")[1].set value_2

      find(".form-group", text: "data:type").all(".row input[type='radio']")[1].click

      click_on 'Save'

      wait_until { all(".modal").empty? }
      wait_until { current_path == @field_path }
      wait_until { all(".wait-component").empty? }

      click_on 'Edit'

      expect(find("input#active")).not_to be_checked
      expect(find(:xpath, "//input[@id='data:label']").value).to eq label
      expect(find(:xpath, "//input[@id='data:forPackage']")).to be_checked
      expect(find(:xpath, "//input[@id='data:permissions:owner']")).not_to be_checked
      expect(find(:xpath, "//select[@id='data:permissions:role']").value).to eq "inventory_manager"
      expect(find("#Inventory")).to be_checked
      expect(find(:xpath, "//select[@id='data:target_type']").value).to eq "license"
      expect(find(:xpath, "//select[@id='data:type']").value).to eq "select"

      expect(
        find(".form-group", text: "data:type").all(".row input[type='radio']").map(&:checked?)
      ).to eq [false, true]

      click_on 'Save'
      wait_until { all(".modal").empty? }

      expect(page).to have_content label
    end
  end
end
