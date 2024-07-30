require 'spec_helper'
require 'pry'

feature 'Managing Mail Templates:', type: :feature do

  before :each do
    @admins = 3.times.map { FactoryBot.create :admin }
    @mail_templates =
      MailTemplate
      .where(is_template_template: true)
      .order(:name, :language_locale)
      .all
    sign_in_as @admins.sample
    click_on "Mail Templates"
  end

  context 'an admin via the UI' do
    scenario "can see and click all mail templates" do
      within("table.mail-templates tbody") do
        @mail_templates.each_with_index do |mail_template, i|
          within("tr.mail-template", text: /#{i + 1} #{mail_template.name}.*#{mail_template.language_locale}/) do
            expect(current_scope).to have_selector("a[href='/admin/mail-templates/#{mail_template.id}']")
          end
        end
      end
    end

    describe 'searching for a mail_template ' do
      before :each do
        @search_mail_template = @mail_templates.sample
        @other_mail_templates= @mail_templates - [@search_mail_template]
      end

      scenario 'searching by name works' do
        term = "reason"
        fill_in 'Search', with: term
        matches = @mail_templates.select { |mt| mt.body.match? /#{term}/ }
        wait_until { all( "table.mail-templates tbody tr").count == matches.count }
        matches.each_with_index do |m, i|
          within("tr.mail-template", text: /#{i + 1} #{m.name}.*#{m.language_locale}/) do
            expect(current_scope).to have_selector("a[href='/admin/mail-templates/#{m.id}']")
          end
        end
      end

      scenario 'filtering by name works' do
        select @search_mail_template.name, from: 'Name'
        wait_until { not page.has_content? "Please wait" }
        matches = @mail_templates.select { |mt| mt.name == @search_mail_template.name }
        wait_until { all( "table.mail-templates tbody tr").count == matches.count }
        matches.each_with_index do |m, i|
          within("tr.mail-template", text: /#{i + 1} #{m.name}.*#{m.language_locale}/) do
            expect(current_scope).to have_selector("a[href='/admin/mail-templates/#{m.id}']")
          end
        end
      end

      scenario 'filtering by type works' do
        select @search_mail_template.type, from: 'Type'
        wait_until { not page.has_content? "Please wait" }
        matches = @mail_templates.select { |mt| mt.type == @search_mail_template.type }
        wait_until { all( "table.mail-templates tbody tr").count == matches.count }
        matches.each_with_index do |m, i|
          within("tr.mail-template", text: /#{i + 1} #{m.name}.*#{m.language_locale}/) do
            expect(current_scope).to have_selector("a[href='/admin/mail-templates/#{m.id}']")
          end
        end
      end

      scenario 'filtering by language_locale works' do
        select @search_mail_template.language_locale, from: 'Language Locale'
        wait_until { not page.has_content? "Please wait" }
        matches = @mail_templates.select { |mt| mt.language_locale == @search_mail_template.language_locale }
        wait_until { all( "table.mail-templates tbody tr").count == matches.count }
        matches.each_with_index do |m, i|
          within("tr.mail-template", text: /#{i + 1} #{m.name}.*#{m.language_locale}/) do
            expect(current_scope).to have_selector("a[href='/admin/mail-templates/#{m.id}']")
          end
        end
      end
    end
  end
end
