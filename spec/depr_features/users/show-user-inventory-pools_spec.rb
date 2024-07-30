require 'spec_helper'
require 'pry'

feature 'Show users inventory-pools', type: :feature do

  context 'some users exist' do

    before :each do
      @admins = 3.times.map { FactoryBot.create :admin }
      @users = 100.times.map { FactoryBot.create :user }
    end

    context 'one @user has two roles in different pools' do

      before :each do
        @user = @users.sample

        @inventory_pool1 = FactoryBot.create :inventory_pool
        FactoryBot.create :access_right, user: @user,
          inventory_pool: @inventory_pool1, role: 'customer'

        @inventory_pool2 = FactoryBot.create :inventory_pool
        FactoryBot.create :access_right, user: @user,
          inventory_pool: @inventory_pool2, role: 'inventory_manager'

      end

      context "an admin via the UI" do

        before :each do
          @admin = @admins.sample
          sign_in_as @admin
        end

        scenario 'can see all inventory_pool users with with roles and links to the pools' do

          visit '/admin/'
          click_on 'Users'

          fill_in 'Search', with: "#{@user.firstname} #{@user.lastname}"
          click_on_first_user @user

          wait_until { page.has_content? @inventory_pool1.name }
          expect(page).to have_content "customer"
          expect(page).to have_content @inventory_pool2.name
          expect(page).to have_content "inventory_manager"

        end

      end


      context 'two users with exclusive lending_manager roles in two pools' do

        before :each do

          @pool1 = FactoryBot.create :inventory_pool, name: "Manager1's Pool"
          @pool2 = FactoryBot.create :inventory_pool, name: "Manager2's Pool"

          @manager1 = @users.filter{ |u| u[:is_admin] == false}.sample
          @manager2 = @users \
            .filter{|u| u[:is_admin] == false and u[:id] != @manager1[:id] }.sample

          FactoryBot.create :access_right, user: @manager1,
            inventory_pool: @pool1, role: 'lending_manager'

          FactoryBot.create :access_right, user: @manager2,
            inventory_pool: @pool1, role: 'customer'

          FactoryBot.create :access_right, user: @manager1,
            inventory_pool: @pool2, role: 'customer'

          FactoryBot.create :access_right, user: @manager2,
            inventory_pool: @pool2, role: 'inventory_manager'


          @pool_shared = FactoryBot.create :inventory_pool, name: "Shared Pool"

          FactoryBot.create :access_right, user: @manager1,
            inventory_pool: @pool_shared, role: 'customer'

          FactoryBot.create :access_right, user: @manager2,
            inventory_pool: @pool_shared, role: 'customer'

        end

        context 'the manager1 of pool1 via the UI' do
          before(:each){ sign_in_as @manager1 }

          scenario "can see all pool's data but only click on the ones where he is manager" do

            visit "/admin/users/#{@manager2[:id]}"
            pool1_row = find(".user-inventory-pools table tbody tr", text: @pool1.name)
            within pool1_row do
              expect(all("a")).not_to be_empty
            end
            expect(pool1_row).to have_content 'customer'

            pool2_row = find(".user-inventory-pools table tbody tr", text: @pool2.name)
            within pool2_row do
              expect(all("a")).to be_empty
            end
            expect(pool2_row).to have_content 'inventory_manager'

          end

        end

      end

    end

  end

end
