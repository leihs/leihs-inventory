require "spec_helper"
require "pry"

def create_accessory(inventory_pool_id, model)
  # accessory
  cde = database[:accessories_inventory_pools].insert(
    accessory_id: FactoryBot.create(:accessory).id,
    inventory_pool_id: inventory_pool_id
  )

  # FIXME
  abc = FactoryBot.create(:accessory, leihs_model: model)
  puts ">>> Accessory.abc: #{abc.id}"
  abc
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

def create_and_add_category_to_model(models)
  models.each do |model|
    FactoryBot.create(:category, direct_models: [model])
    category.add_direct_model(model)
  end
  end

def create_and_add_category_to_model(models, category = nil)
  created_categories = []  # Array to store all created categories

  if category.nil?
    category = FactoryBot.create(:category)  # Create category if not passed as argument
  end

  # # binding.pry

  models.each do |model|
    # category = FactoryBot.create(:category, direct_models: [model])  # Create category
    category.add_direct_model(model)  # Add model to the category

    puts ">>> ADD category: #{category.id}"
    puts ">>> ADD model: #{model.id}"

    created_categories << category  # Store the created category
  end

  created_categories  # Return the array of created categories
end


def link_categories_to_pool(categories, inventory_pool)
  categories.each do |category|
    # category.add_direct_model(model)
    database[:inventory_pools_model_groups].insert(inventory_pool.id, category.id)

  end
end


def create_and_add_category_to_model_and_link_to_pool(model,inventory_pool, category=nil )
  categories =create_and_add_category_to_model([model], category)
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
    # add model-property
    first_model = @models.first
    puts ">>>>>>> first_model: #{first_model.id}"

    create_accessory(@inventory_pool.id, first_model)
    create_and_add_property_to_model(first_model)
    create_and_add_entitlements(@inventory_pool, first_model)
  end
  end

shared_context :setup_category_model_linked_to_pool do
  before :each do

    # category = FactoryBot.create(:category)

    @categories = create_and_add_category_to_model_and_link_to_pool(@models.third, @inventory_pool )
    
    
  end
end

shared_context :setup_models_api do
  before :each do
    @user = FactoryBot.create(:user, login: "test", password: "password")
    @inventory_pool = FactoryBot.create(:inventory_pool)

    FactoryBot.create(:direct_access_right, inventory_pool_id: @inventory_pool.id, user_id: @user.id, role: "group_manager")

    @models = create_models
    # create_and_add_items_to_all_existing_models(@inventory_pool)
    create_and_add_items_to_models(@inventory_pool, [@models.first])
  end

  include_context :setup_accessory_entitlements
  # include_context :setup_category_model_linked_to_pool
end


shared_context :setup_models_min_api do
  before :each do
    @user = FactoryBot.create(:user, login: "test", password: "password")
    @inventory_pool = FactoryBot.create(:inventory_pool)

    FactoryBot.create(:direct_access_right, inventory_pool_id: @inventory_pool.id, user_id: @user.id, role: "group_manager")

    # @models = create_models

    # create_and_add_items_to_all_existing_models(@inventory_pool)
    # create_and_add_items_to_models(@inventory_pool, [@models.first])
  end
  end