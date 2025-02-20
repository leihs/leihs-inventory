require "spec_helper"
require "pry"
require "faker"

def create_accessory(inventory_pool_id, model)
  database[:accessories_inventory_pools].insert(
    accessory_id: FactoryBot.create(:accessory).id,
    inventory_pool_id: inventory_pool_id
  )
  FactoryBot.create(:accessory, leihs_model: model)
end

def create_models(count = 3)
  @models = count.times.map do
    FactoryBot.create(:leihs_model, id: SecureRandom.uuid)
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

shared_context :setup_models_api do |role = "inventory_manager"|
  before :each do
    @user = FactoryBot.create(:user, login: "test", password: "password")
    @inventory_pool = FactoryBot.create(:inventory_pool)

    @direct_access_right = FactoryBot.create(:direct_access_right, inventory_pool_id: @inventory_pool.id, user_id: @user.id, role: role)

    @models = create_models
    create_and_add_items_to_models(@inventory_pool, [@models.first])
  end

  include_context :setup_accessory_entitlements
end

shared_context :generate_session_header do
  before :each do
    resp = basic_auth_plain_faraday_json_client(@user.login, @user.password).get("/inventory/login")
    expect(resp.status).to eq(200)

    cookie_token = parse_cookie(resp.headers["set-cookie"])["leihs-user-session"]
    cookie = CGI::Cookie.new("name" => "leihs-user-session", "value" => cookie_token)

    @cookie_header = {"Accept" => "application/json", "Cookie" => cookie.to_s}
  end
end

shared_context :setup_models_api_model do |role = "inventory_manager"|
  include_context :setup_models_api, role

  before :each do
    @form_categories = [FactoryBot.create(:category), FactoryBot.create(:category)]

    @models = []
    model = FactoryBot.create(:leihs_model, manufacturer: Faker::Company.name)
    @models << model
    compatible_model1 = FactoryBot.create(:leihs_model, id: SecureRandom.uuid)
    model.add_recommend(compatible_model1)

    model = FactoryBot.create(:leihs_model, manufacturer: Faker::Company.name)
    @models << model
    compatible_model2 = FactoryBot.create(:leihs_model, id: SecureRandom.uuid)
    model.add_recommend(compatible_model2)

    model = FactoryBot.create(:leihs_model, manufacturer: Faker::Company.name, type: "Software")
    @models << model
    compatible_model3 = FactoryBot.create(:leihs_model, id: SecureRandom.uuid)
    model.add_recommend(compatible_model3)

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
    model.add_recommend(compatible_model1)

    model = FactoryBot.create(:leihs_model, manufacturer: Faker::Company.name)
    compatible_model2 = FactoryBot.create(:leihs_model, id: SecureRandom.uuid)
    model.add_recommend(compatible_model2)

    model = FactoryBot.create(:leihs_model, manufacturer: Faker::Company.name, type: "Software")
    compatible_model3 = FactoryBot.create(:leihs_model, id: SecureRandom.uuid)
    model.add_recommend(compatible_model3)

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
    @direct_access_right = FactoryBot.create(:direct_access_right, inventory_pool_id: @inventory_pool.id, user_id: @user.id, role: "group_manager")
  end
end

shared_context :setup_models_api_base do
  before :each do
    @user = FactoryBot.create(:user, login: "test", password: "password")
    @inventory_pool = FactoryBot.create(:inventory_pool)
  end
end
