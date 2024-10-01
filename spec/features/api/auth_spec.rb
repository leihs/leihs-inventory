require "spec_helper"
require "pry"



# feature "Call swagger-endpoints", type: :feature do
feature "Call swagger-endpoints" do
  # context " with accept=text/html" do
  # context "with accept=text/html", driver: :rack_test do
  context "with accept=text/html", driver: :selenium_headless do

    config.include Helpers::User

    before :each do
      # Capybara.disable_animation = true

      # first, last = full_name.split
      # login = user_login_from_full_name(full_name)
      # email = Faker::Internet.email(name: full_name)
      # login = user_login_from_full_name(full_name)
      # email = Faker::Internet.email(name: full_name)

      login = "test-user"

      # @user = User.find(login: login) || FactoryBot.create(:user, firstname: first, lastname: last, login: login, email: email)
      # @user = FactoryBot.create(:user, firstname: first, lastname: last, login: login, email: email)
      # @user = FactoryBot.create(:user, login: login, email: email)
      @user = FactoryBot.create(:user, login: login)

      # @admins = 3.times.map { FactoryBot.create :admin }
      # # @system_admins = 3.times.map { FactoryBot.create :system_admin}
      # @users = 3.times.map { FactoryBot.create :user }
      # @user =  FactoryBot.create :user,
      # # @groups = 100.times.map { FactoryBot.create :group }
    end

    let :client do
      plain_faraday_client
    end

    # let :basic_auth_client do
    #   plain_faraday_client
    # end

    it 'returns id of created request' do

      resp = client.get "/inventory/login"
      expect(resp.status).to be == 403
      binding.pry
      # expect(resp.body["message"]).to eq "Invalid credentials"
    end


    it 'returns id of created request2' do

      # resp = plain_faraday_client(@user.login, @user.password).get "/inventory/login"
      resp = basic_auth_plain_faraday_json_client("abc", "def").get("/inventory/login")
      expect(resp.status).to be == 403
      binding.pry
      # expect(resp.body["message"]).to eq "Invalid credentials"
    end

    it 'returns id of created request3' do

      puts "user.login: #{@user.login}"
      puts "user.password: #{@user.password}"

      resp = plain_faraday_json_client.get("/inventory/session/protected")

      expect(resp.status).to be == 403

      # resp = plain_faraday_client(@user.login, @user.password).get "/inventory/login"
      resp = basic_auth_plain_faraday_json_client(@user.login, @user.password).get("/inventory/login")
      expect(resp.status).to be == 200




      cookie = resp.headers["set-cookie"]





      binding.pry

      cookie = Helpers.parse_cookie(cookie)

      binding.pry

      cookie = CGI::Cookie.new("name" => "leihs-user-session",
                      # TODO encode
                      "value" => cookie)

      resp = session_auth_plain_faraday_json_client(cookie.to_s).get("/inventory/session/protected")

      binding.pry


      expect(resp.status).to be == 200



      # expect(resp.body["message"]).to eq "Invalid credentials"
    end

  end
end
