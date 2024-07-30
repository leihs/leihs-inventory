require 'spec_helper'
require 'pry'

::BASIC_USER_PROPERTIES = {
  account_enabled: true,
  address: 'Pfingstweidstrasse 96',
  badge_id: '13456889xyz',
  city: 'Zürich',
  country: 'CH',
  email: 'Toni.Artist@zdhk.ch',
  firstname: 'Toni',
  img256_url: 'https://upload.wikimedia.org/wikipedia/commons/e/e0/Anonymous.svg',
  img32_url: 'https://upload.wikimedia.org/wikipedia/commons/e/e0/Anonymous.svg',
  img_digest: '279c1860e93da29ea5458ece3daffc72',
  lastname: 'Artist',
  password_sign_in_enabled: true,
  phone: '1-879-730-8771 x1308',
  secondary_email: 'Toni.Artest@gmail.com',
  url: 'https://museum-gestaltung.ch/de/standort/toni-areal/',
  zip: '8005' }


shared_context :all_types_of_users do
  before :each do
    @user = FactoryBot.create :user

    @admin =  FactoryBot.create :admin,
      email: 'admin@example.com', password: 'secret',
      admin_protected: true , system_admin_protected: false,
      lastname: 'Adminus'

    @system_admin =  FactoryBot.create :system_admin,
      email: 'system-admin@example.com', password: 'secret',
      admin_protected: true, system_admin_protected: true,
      lastname: 'Sysmaster'

    @pool =  FactoryBot.create :inventory_pool
    @manager = FactoryBot.create :user, lastname: 'Poolruler',
      admin_protected: false, system_admin_protected: false
    FactoryBot.create :access_right, user: @manager,
      inventory_pool: @pool, role: 'inventory_manager'
  end
end

shared_context :setup_api do
  before :each do
    @http_client = plain_faraday_client
    @api_token = FactoryBot.create :system_admin_api_token,
      user_id: @current_user.id
    @token_secret = @api_token.token_secret
    @http_client.headers["Authorization"] = "Token #{@token_secret}"
    @http_client.headers["Content-Type"] = "application/json"
  end
end

shared_context :sign_in_to_admin do
  before :each do
    sign_in_as @current_user
    visit '/admin/'
  end
end

shared_context :basic_user_properties do


  def fill_in_user_properties properties
    properties.each do |k,v|
      if v === true
        check k
      elsif v === false
        uncheck k
      else
        fill_in k, with: v
      end
    end
  end

  def assert_user_properties id, properties
    user = database[:users].where(id: id).first.try(:with_indifferent_access)
    expect(user).to be
    properties.each do |k, v|
      expect(user[k]).to be== v, "expected value for key #{k} is #{user[k]} but is #{v}"
    end
  end

end
