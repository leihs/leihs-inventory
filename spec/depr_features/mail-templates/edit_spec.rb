require 'spec_helper'
require 'pry'

feature 'Manage Mail Templates', type: :feature do

  let(:mt_body) { Faker::Markdown.sandwich }

  before :each do
    @admin = FactoryBot.create :admin
    @mail_templates = MailTemplate.where(is_template_template: true).all
    @mail_template = @mail_templates.detect { |mt| mt.name == "approved" }
  end

  context "an admin via the UI" do
    before(:each){ sign_in_as @admin }

    scenario 'edits a mail_template' do
      visit '/admin/'
      click_on 'Mail Templates'
      within find("tr.mail-template",
                  text: /#{@mail_template.name}.*#{@mail_template.language_locale}/) do
        click_on @mail_template.name
      end
      @mail_template_path = current_path

      click_on 'Edit'
      fill_in 'body', with: mt_body
      click_on 'Save'
      wait_until { all(".modal").empty? }
      wait_until {current_path == @mail_template_path}
      wait_until { all(".wait-component").empty? }

      input_values = all("input").map(&:value).join(" ")
      expect(page.text + input_values).to have_content mt_body

      within("aside nav") do
        click_on "Mail Templates"
      end

      wait_until { current_path ==  "/admin/mail-templates/" }
      expect(page).to have_selector("tr.mail-template",
                                    text: /#{@mail_template.name}.*#{@mail_template.language_locale}/)
    end

  end

end
