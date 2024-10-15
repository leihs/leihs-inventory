require "spec_helper"
require "pry"

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

shared_context :setup_models_api do
  before :each do
    @user = FactoryBot.create(:user, login: "test", password: "password")
    @inventory_pool = FactoryBot.create(:inventory_pool)

    FactoryBot.create(:direct_access_right, inventory_pool_id: @inventory_pool.id, user_id: @user.id, role: "group_manager")

    @models = create_models
    create_and_add_items_to_models(@inventory_pool, [@models.first])
  end

  include_context :setup_accessory_entitlements
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

    # links = FactoryBot.create(:model_link)
    # ok model-group
    @category = FactoryBot.create(:category, direct_models: [@models.first])

    # ok group_access_right & access_right
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
    @user = FactoryBot.create(:user, login: "test", password: "password")
    @inventory_pool = FactoryBot.create(:inventory_pool)

    FactoryBot.create(:direct_access_right, inventory_pool_id: @inventory_pool.id, user_id: @user.id, role: "group_manager")
  end
end
