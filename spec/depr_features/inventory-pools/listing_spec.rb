require 'spec_helper'
require 'pry'

feature 'Managing inventory-pools:', type: :feature do

  context 'some users and at least one admin' do

    before :each do
      @admins = 3.times.map { FactoryBot.create :admin }
      @users = 100.times.map { FactoryBot.create :user }
    end

    context 'as two users with exclusive lending_manager roles in two pools' do

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

      context 'an admin via the UI' do
        before(:each){ sign_in_as @admins.sample }
        scenario "can see and click all pools" do
          click_on "Inventory Pools"
          all("table.inventory-pools tbody tr").each do |tr|
            within(tr) do
              expect(all("a")).not_to be_empty
            end
          end
        end
      end

      context 'the manager1 of pool1 via the UI' do

        before(:each){ sign_in_as @manager1 }

        scenario "can see all pools but only click on the ones where he is manager" do
          click_on "Inventory Pools"
          expect(all("table.inventory-pools tbody tr").count).to be == 3
          pool2_row = find("table.inventory-pools tbody tr", text: @pool2.name)
          within pool2_row do
            expect(all("a")).to be_empty
          end
          pool1_row = find("table.inventory-pools tbody tr", text: @pool1.name)
          within pool1_row do
            expect(all("a")).not_to be_empty
          end
          click_on_first @pool1.name
          expect(current_path).to be== "/admin/inventory-pools/#{@pool1.id}"
        end

      end

    end

  end

end
