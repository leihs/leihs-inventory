require 'spec_helper'
require 'pry'

feature 'Status-info', skip: "Information not gathered properly. See `leihs.core.status` for more details." do
  context 'a system-admin exists' do
    # before(:each){ @admin = FactoryBot.create :system_admin }
    context 'system_admin via the UI' do
    #   before(:each){ sign_in_as @admin  }

      scenario 'Visiting the Status-Info page' do
        visit '/'
        expect(page).to have_content 'Overview'
      end

    end
  end
end
