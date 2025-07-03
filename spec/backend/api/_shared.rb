require "spec_helper"
require "pry"
require "faker"

require 'faraday'
# require 'faraday-cookie_jar'

def create_accessory(inventory_pool_id, model)
  accessory = FactoryBot.create(:accessory, leihs_model: model)
  database[:accessories_inventory_pools].insert(
    accessory_id: accessory.id,
    inventory_pool_id: inventory_pool_id
  )
  accessory
end

def create_models(count = 3)
  @models = count.times.map do
    FactoryBot.create(:leihs_model, id: SecureRandom.uuid)
  end
  add_image_to_model(@models.first)
  @models
end

# FIXME: DEPRECATED
def create_model_post(client, inventory_pool_id, product, category_ids)
  client.post "/inventory/#{inventory_pool_id}/models" do |req|
    req.body = {
      product: product,
      category_ids: category_ids,
      version: "1",
      type: "Model",
      is_package: false
    }.to_json
    req.headers["Content-Type"] = "application/json"
    req.headers["Accept"] = "application/json"
    req.headers["x-csrf-token"] = X_CSRF_TOKEN
  end
end

def create_and_add_items_to_all_existing_models(inventory_pool)
  LeihsModel.all.each do |model|
    FactoryBot.create(:item, leihs_model: model, inventory_pool_id: inventory_pool.id, responsible: inventory_pool, is_borrowable: true)
  end
end

def create_and_add_items_to_models(inventory_pool, models)
  models.each do |model|
    FactoryBot.create(:item, leihs_model: model, inventory_pool_id: inventory_pool.id, responsible: inventory_pool, is_borrowable: true)
  end
end

def create_and_add_category_to_models(models)
  models.each do |model|
    FactoryBot.create(:category, direct_models: [model])
    category.add_direct_model(model)
  end
end

def create_and_add_category_to_model(models, category = nil)
  created_categories = []

  if category.nil?
    category = FactoryBot.create(:category)
  end

  models.each do |model|
    category.add_direct_model(model)
    created_categories << category
  end

  created_categories
end

def link_categories_to_pool(categories, inventory_pool)
  categories.each do |category|
    database[:inventory_pools_model_groups].insert(inventory_pool.id, category.id)
  end
end

def create_and_add_category_to_model_and_link_to_pool(models, inventory_pool, category = nil)
  categories = create_and_add_category_to_model(models, category)
  link_categories_to_pool(categories, inventory_pool)
  categories
end

def create_and_add_property_to_model(model, key = "my-test-property-key", value = "my-test-property-value")
  model.add_property(FactoryBot.create(:property, key: key, value: value))
end

def create_and_add_items(inventory_pool, models)
  models.each do |model|
    FactoryBot.create(:item, leihs_model: model, inventory_pool_id: inventory_pool.id, responsible: inventory_pool, is_borrowable: true)
  end
end

def create_and_add_entitlements(inventory_pool, model)
  group = FactoryBot.create(:entitlement_group, inventory_pool_id: inventory_pool.id)
  FactoryBot.create(:entitlement, leihs_model: model, entitlement_group: group, quantity: 2)
end

shared_context :setup_accessory_entitlements do
  before :each do
    first_model = @models.first
    create_accessory(@inventory_pool.id, first_model)
    create_and_add_property_to_model(first_model)
    create_and_add_entitlements(@inventory_pool, first_model)
    create_and_add_entitlements(@inventory_pool, first_model)
  end
end

shared_context :setup_category_model_linked_to_pool do
  before :each do
    @categories = create_and_add_category_to_model_and_link_to_pool([@models.third], @inventory_pool)
  end
end

shared_context :setup_category_model_linked_all_to_pool do
  before :each do
    @categories = create_and_add_category_to_model_and_link_to_pool(@models, @inventory_pool)
  end
end

shared_context :setup_user_with_direct_access_right do
  before :each do
    @user = FactoryBot.create(:user, login: "user", password: "password")
    @inventory_pool = FactoryBot.create(:inventory_pool)
  end
end

shared_context :setup_admin_with_direct_access_right do
  before :each do
    @user = FactoryBot.create(:admin, login: "admin", password: "password")
    @inventory_pool = FactoryBot.create(:inventory_pool)
  end
end

shared_context :setup_models_api do |role = "inventory_manager"|
  before :each do
    @user = FactoryBot.create(:user, login: "test", password: "password")
    @inventory_pool = FactoryBot.create(:inventory_pool)
    @inventory_pool_id = @inventory_pool.id

    @direct_access_right = FactoryBot.create(:direct_access_right, inventory_pool_id: @inventory_pool.id, user_id: @user.id, role: role)

    @models = create_models
    create_and_add_items_to_models(@inventory_pool, [@models.first])

    model = @models.first
    image = add_image_to_model(model)
    update_cover_image(model, image)
  end

  include_context :setup_accessory_entitlements
end

shared_context :setup_models_for_duplicates_api do |role = "inventory_manager"|
  before :each do
    @user = FactoryBot.create(:user, login: "test", password: "password")
    @inventory_pool = FactoryBot.create(:inventory_pool)

    @direct_access_right = FactoryBot.create(:direct_access_right, inventory_pool_id: @inventory_pool.id, user_id: @user.id, role: role)

    @models = create_models
    create_and_add_items_to_models(@inventory_pool, [@models.first])

    2.times do
      create_and_add_items_to_models(@inventory_pool, [@models.second])
    end

    model = @models.first
    image = add_image_to_model(model)
    update_cover_image(model, image)
  end

  include_context :setup_accessory_entitlements
end

def generate_csrf_session_data(cookie_token)
  cookies = [
    CGI::Cookie.new("name" => "leihs-user-session", "value" => cookie_token),
    CGI::Cookie.new("name" => "leihs-anti-csrf-token", "value" => X_CSRF_TOKEN)
  ]

  cookies_str = cookies.map(&:to_s).join("; ")
  [cookies, cookies_str]
end

def generate_csrf_data(cookie_token)
  cookies = [
    CGI::Cookie.new("name" => "leihs-anti-csrf-token", "value" => X_CSRF_TOKEN)
  ]

  cookies_str = cookies.map(&:to_s).join("; ")
  [cookies, cookies_str]
end

shared_context :generate_session_header do |accept = "application/json", cookie_attributes = []|
  before :each do
    @user_cookies, @user_cookies_str, @cookie_token = create_and_login_by(@user)

    @cookie_header = {
      "Accept" => accept,
      "Cookie" => @user_cookies_str
    }
  end
end

def add_image_to_model(model)
  image = FactoryBot.create(:image, :for_leihs_model)
  model.add_image(image)
  image
end

def update_cover_image(model, image)
  model.update(cover_image_id: image.id)
end

shared_context :setup_models_api_model do |role = "inventory_manager"|
  include_context :setup_models_api, role

  before :each do
    @form_categories = [FactoryBot.create(:category), FactoryBot.create(:category)]

    @models = []
    model = FactoryBot.create(:leihs_model, manufacturer: Faker::Company.name)
    @models << model
    compatible_model1 = FactoryBot.create(:leihs_model, id: SecureRandom.uuid)
    model.add_compatible_model(compatible_model1)

    image = add_image_to_model(compatible_model1)
    update_cover_image(compatible_model1, image)

    model = FactoryBot.create(:leihs_model, manufacturer: Faker::Company.name)
    @models << model
    compatible_model2 = FactoryBot.create(:leihs_model, id: SecureRandom.uuid)
    model.add_compatible_model(compatible_model2)

    model = FactoryBot.create(:leihs_model, manufacturer: Faker::Company.name, type: "Software")
    @models << model
    compatible_model3 = FactoryBot.create(:leihs_model, id: SecureRandom.uuid)
    model.add_compatible_model(compatible_model3)

    image = add_image_to_model(compatible_model3)
    update_cover_image(compatible_model3, image)

    @model = model
    @form_compatible_models = [compatible_model1, compatible_model2, compatible_model3]
  end
end

shared_context :setup_unknown_building_room_supplier do
  before :each do
    @building = FactoryBot.create(:building, name: "Unbekanntes Gebäude")
    @room = FactoryBot.create(:room, building: @building, name: "nicht bekannt")

    @supplier = FactoryBot.create(:supplier)
  end
end

shared_context :setup_models_api_license do |role = "inventory_manager"|
  include_context :setup_models_api, role

  before :each do
    @form_categories = [FactoryBot.create(:category), FactoryBot.create(:category)]

    model = FactoryBot.create(:leihs_model, manufacturer: Faker::Company.name)
    compatible_model1 = FactoryBot.create(:leihs_model, id: SecureRandom.uuid)
    model.add_compatible_model(compatible_model1)

    model = FactoryBot.create(:leihs_model, manufacturer: Faker::Company.name)
    compatible_model2 = FactoryBot.create(:leihs_model, id: SecureRandom.uuid)
    model.add_compatible_model(compatible_model2)

    model = FactoryBot.create(:leihs_model, manufacturer: Faker::Company.name, type: "Software")
    compatible_model3 = FactoryBot.create(:leihs_model, id: SecureRandom.uuid)
    model.add_compatible_model(compatible_model3)

    @software_model = model

    # FIXME: owner_id / inventory_pool_id correct?
    LeihsModel.where(type: "Software").each do |model|
      @license_item = FactoryBot.create(:item,
        inventory_code: "TEST#{SecureRandom.random_number(1000)}",
        leihs_model: model,
        inventory_pool_id: @inventory_pool.id,
        owner_id: @inventory_pool.id,
        responsible: @inventory_pool,
        is_borrowable: true)
    end

    @form_compatible_models = [compatible_model1, compatible_model2, compatible_model3]
  end
end

def create_and_add_group_permission(inventory_pool, group, role)
  FactoryBot.create :group_access_right, group_id: group.id,
    inventory_pool_id: inventory_pool.id, role: role
end

def create_and_add_user_permission(inventory_pool, user, role)
  FactoryBot.create :access_right, user: user, inventory_pool: inventory_pool, role: role
end

shared_context :setup_models_api2 do
  before :each do
    @user = FactoryBot.create(:user, login: "test", password: "password")
    @inventory_pool = FactoryBot.create(:inventory_pool)

    create_and_add_user_permission(@inventory_pool, @user, "customer")
    create_and_add_group_permission(@inventory_pool, @group, "customer")
  end
end

shared_context :setup_access_rights do
  before :each do
    @user = FactoryBot.create(:user, login: "test", password: "password")
    @inventory_pool = FactoryBot.create(:inventory_pool)

    FactoryBot.create(:direct_access_right, inventory_pool_id: @inventory_pool.id, user_id: @user.id, role: "group_manager")

    @models = 3.times.map do
      FactoryBot.create(:leihs_model, id: SecureRandom.uuid)
    end

    LeihsModel.all.each do |model|
      FactoryBot.create(:item, leihs_model: model, inventory_pool_id: @inventory_pool.id, responsible: @inventory_pool, is_borrowable: true)
    end

    @models.first

    # -------------------------

    @category = FactoryBot.create(:category, direct_models: [@models.first])

    @manager = FactoryBot.create :user
    @group = FactoryBot.create(:group, name: "Group 1")
    FactoryBot.create :access_right, user: @manager, inventory_pool: @inventory_pool, role: "lending_manager"
    FactoryBot.create :group_access_right, group_id: @group.id,
      inventory_pool_id: @inventory_pool.id, role: "customer"

    FactoryBot.create(:supplier)
    building = FactoryBot.create(:building)

    b = Building.find(name: building.name)
    FactoryBot.create(:room, building: b)

    @model = FactoryBot.create(:leihs_model)
    @image = FactoryBot.create(:image, :for_category)
    @filename = @image.filename

    @image = FactoryBot.create(:image, :for_leihs_model)
    @filename = @image.filename
  end
end

shared_context :setup_models_min_api do
  before :each do
    @user = FactoryBot.create(:user, login: Faker::Lorem.word, password: "password")
    @inventory_pool = FactoryBot.create(:inventory_pool)
    # @direct_access_right = FactoryBot.create(:direct_access_right, inventory_pool_id: @inventory_pool.id, user_id: @user.id, role: "group_manager")
    @direct_access_right = FactoryBot.create(:direct_access_right, inventory_pool_id: @inventory_pool.id, user_id: @user.id, role: "inventory_manager")
  end
end

shared_context :setup_models_api_base do
  before :each do
    @user = FactoryBot.create(:user, login: Faker::Lorem.word, password: "password")
    @inventory_pool = FactoryBot.create(:inventory_pool)
  end
end

def create_and_login(role, login = nil, password = nil)
  user = if login.nil? && password.nil?
    FactoryBot.create(role, login: Faker::Lorem.word, password: "password")
  else
    FactoryBot.create(role, login: login, password: password)
  end

  resp = plain_faraday_json_client().get("/inventory/csrf-token/")
  token = resp.body["csrfToken"]["value"]
  _, cookie_str = generate_csrf_data(token)

  response = common_plain_faraday_client(:post, "/sign-in", body: {
    "user" => user.login,
    "password" => user.password,
    "csrf-token" => token  }, multipart: true, headers: {Cookie: cookie_str}
  )
  expect(response.status).to eq(200)

  session_cookie = parse_cookie(response.headers["set-cookie"])["leihs-user-session"]

  [user] + generate_csrf_session_data(session_cookie) + [session_cookie]
end

def create_and_login_by(user)
  # resp = basic_auth_plain_faraday_json_client(user.login, user.password).get("/sign-in")

  resp = plain_faraday_json_client().get("/inventory/csrf-token/")
  token = resp.body["csrfToken"]["value"]
  _, cookie_str = generate_csrf_data(token)

  resp = common_plain_faraday_client(:post, "/sign-in", body: {
    "user" => user.login,
    "password" => user.password,
    "csrf-token" => token  }, multipart: true, headers: {Cookie: cookie_str}
  )

  expect(resp.status).to eq(200)
  session_cookie = parse_cookie(resp.headers["set-cookie"])["leihs-user-session"]

  generate_csrf_session_data(session_cookie) + [session_cookie]
end
