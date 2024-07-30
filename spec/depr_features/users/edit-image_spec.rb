require 'spec_helper'
require 'pry'

feature 'Editing users images', type: :feature do

  context 'an admin user via the UI' do

    before :each do
      @user = FactoryBot.create :admin
    end


    scenario 'stets, views and then removes user images ' do

      sign_in_as @user

      click_on "Users"

      within 'table.users tbody' do
        click_on_first_user @user
      end
      click_on 'Edit'

      find('label', text: 'Choose file').find('input') \
        .set(File.absolute_path('./spec/data/anon.jpg'))

      wait_until {page.has_content? 'Remove image'}

      click_on 'Save'

      wait_until do
        first('img.user-image-256') and \
          first('img.user-image-256')['src'][0..22] == 'data:image/jpeg;base64,'
      end

      click_on 'Edit'

      click_on 'Remove image'

      click_on 'Save'

      wait_until do
        first('img.user-image-256') and \
          first('img.user-image-256')['src'][0..31] == "https://www.gravatar.com/avatar/"
      end

    end

  end

end




