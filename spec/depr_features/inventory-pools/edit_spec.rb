require 'spec_helper'
require 'pry'

feature 'Manage inventory-pools', type: :feature do

  let(:name) { Faker::Company.name}
  let(:shortname) { Faker::Name.initials }
  let(:email) { Faker::Internet.email }
  let(:description) { Faker::Markdown.sandwich }
  let(:default_contract_note) { Faker::Markdown.sandwich }
  let(:automatic_suspension_reason) { Faker::Markdown.sandwich }
  let(:reservation_advance_days) { 1 }

  context 'an admin and several pools ' do

    before :each do
      @admin = FactoryBot.create :admin
      @pools = 10.times.map { FactoryBot.create :inventory_pool }
      @pool = @pools.sample
    end

    context "an admin via the UI" do
      before(:each) do
        sign_in_as @admin
      end

      scenario 'edits an inventory pool' do
        visit '/admin/'
        within("aside nav") do
          click_on "Inventory Pools"
        end
        click_on @pool.name
        @inventory_pool_path = current_path
        click_on 'Edit'
        expect(page).to have_css('.modal')

        fill_in 'name', with: name
        fill_in 'description', with: description

        expect(page).to have_selector('input#shortname[disabled]')
        # fill_in 'shortname', with: shortname
        fill_in 'email', with: email
        click_on_toggle 'is_active'
        click_on 'Save'
        wait_until { all(".modal").empty? }
        wait_until { current_path == @inventory_pool_path }
        wait_until { all(".wait-component").empty? }

        expect(page.text).to have_content name
        # expect(page.text).to have_content shortname
        expect(page.text).to have_content email
        expect(page.text).to have_content description

        within("aside nav") do
          click_on "Inventory Pools"
        end
        wait_until { current_path ==  "/admin/inventory-pools/" }
        expect(page).to have_content name
      end

    end

    context 'a inventory-pool manager' do

      before :each do
        @manager = FactoryBot.create :user
        @pool = FactoryBot.create(:inventory_pool,
                                  print_contracts: false,
                                  automatic_suspension: false,
                                  required_purpose: false)
        Workday.where(inventory_pool_id: @pool.id)
          .update(monday: false,
                  tuesday: false,
                  wednesday: false,
                  thursday: false,
                  friday: false,
                  saturday: false,
                  sunday: false)
        @holiday_1 = FactoryBot.create :holiday, inventory_pool: @pool
        @holiday_2 = FactoryBot.create :holiday, inventory_pool: @pool
        FactoryBot.create :access_right, user: @manager,
          inventory_pool: @pool, role: 'inventory_manager'
      end

      context 'via the UI' do
        before(:each){ sign_in_as @manager }

        scenario 'edits the pool settings' do
          visit '/admin/'
          within("aside nav") do
            click_on "Inventory Pools"
          end
          click_on @pool.name
          @inventory_pool_path = current_path
          click_on 'Edit'

          expect(find("input#is_active", visible: false)).to be_disabled
          fill_in 'name', with: name
          expect(find("input#shortname")).to be_disabled
          fill_in 'description', with: description
          fill_in 'email', with: email
          fill_in 'default_contract_note', with: default_contract_note
          click_on_toggle 'print_contracts'
          click_on_toggle 'automatic_suspension'
          fill_in 'automatic_suspension_reason', with: automatic_suspension_reason
          click_on_toggle 'required_purpose'
          fill_in 'reservation_advance_days', with: reservation_advance_days

          click_on 'Save'
          wait_until {current_path == @inventory_pool_path}

          click_on 'Edit'
          expect(find("input#name").value).to eq name
          expect(find("textarea#description").value).to eq description
          expect(find("input#email").value).to eq email
          expect(find("textarea#default_contract_note").value).to eq default_contract_note
          expect(find("input#print_contracts", visible: false)).to be_checked
          expect(find("input#automatic_suspension", visible: false)).to be_checked
          expect(find("textarea#automatic_suspension_reason").value).to eq automatic_suspension_reason
          expect(find("input#required_purpose", visible: false)).to be_checked
          expect(find("input#reservation_advance_days").value).to eq reservation_advance_days.to_s
        end

        context 'edits the opening times' do
          scenario 'workdays' do
            visit '/admin/'
            click_on 'Inventory Pools'
            click_on @pool.name
            @inventory_pool_path = current_path
            click_on 'Opening Times'
            within('#workdays') do
              click_on 'Edit'
            end
            @workdays_path = current_path
            weekdays = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday',
                        'Saturday', 'Sunday']
            within('.modal') do
              weekdays.each do |day|
                within("tr", text: day) do
                  click_on_toggle "#{day.downcase}-switch"
                  find("input[type='number']").set 100
                end
              end
              click_on 'Save'
            end
            wait_until {current_path == @workdays_path}
            within('#workdays') do
              click_on 'Edit'
            end
            within('.modal') do
              weekdays.each do |day|
                within("tr", text: day) do
                  expect(find("input##{day.downcase}-switch", visible: false)).to be_checked
                  expect(find("input[type='number']").value).to eq 100.to_s
                end
              end
              click_on 'Save'
            end
          end

          scenario 'holidays' do
            visit '/admin/'
            click_on 'Inventory Pools'
            click_on @pool.name
            @inventory_pool_path = current_path
            click_on 'Opening Times'
            within('#holidays') do
              click_on 'Edit'
            end
            @holidays_path = current_path
            within('.modal') do
              find("input[type='text']").set "New Holiday 1"
              fill_in "start-date", with: Date.tomorrow.iso8601
              fill_in "end-date", with: (Date.tomorrow + 1).iso8601
              click_on 'Add'

              find("input[type='text']").set "New Holiday 2"
              fill_in "start-date", with: (Date.tomorrow + 2).iso8601
              fill_in "end-date", with: (Date.tomorrow + 3).iso8601
              click_on 'Add'
              find("tr", text: "New Holiday 2").click_on 'Delete'

              find("tr", text: @holiday_1.name).click_on 'Delete'
              click_on 'Save'
            end
            wait_until {current_path == @holidays_path}
            within('#holidays') do
              click_on 'Edit'
            end
            binding.pry
            within('.modal') do
              within("tbody") do
                expect(all("tr").count).to eq 2
                expect(find("tr", text: "New Holiday 1")).to be_present
                expect(find("tr", text: @holiday_2.name)).to be_present
              end
            end
          end
        end

      end

      context 'a lending manager' do

        before :each do
          @manager = FactoryBot.create :user
          FactoryBot.create :access_right, user: @manager,
            inventory_pool: @pool, role: 'lending_manager'
        end

        context 'via the UI' do
          before(:each){ sign_in_as @manager }
          scenario 'edits the pool' do
            visit '/admin/'
            within("aside nav") do
              click_on "Inventory Pools"
            end
            click_on @pool.name
            expect(all("a, button", text: 'Edit')).to be_empty
          end

        end


        context 'via API' do

          let :http_client do
            plain_faraday_client
          end

          let :prepare_http_client do
            @api_token = FactoryBot.create :api_token, user_id: @manager.id
            @token_secret = @api_token.token_secret
            http_client.headers["Authorization"] = "Token #{@token_secret}"
            http_client.headers["Content-Type"] = "application/json"
          end

          before :each do
            prepare_http_client
          end

          scenario 'editing the pool is forbidden' do
            resp = http_client.patch "/admin/inventory-pools/#{@pool[:id]}",
              {name: "New Name"}.to_json
            expect(resp.status).to be== 403
          end

        end

      end

    end

  end

end
