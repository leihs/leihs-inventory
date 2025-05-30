class LeihsModel < Sequel::Model(:models)
  many_to_many(:categories,
    left_key: :model_id,
    right_key: :model_group_id,
    join_table: :model_links)
  one_to_many(:items, key: :model_id)
  one_to_many(:images, key: :target_id)
  one_to_many(:attachments, key: :model_id)
  one_to_many(:properties, key: :model_id)
  many_to_many(:compatible_models,
    class: :LeihsModel,
    left_key: :model_id,
    right_key: :compatible_id,
    join_table: :models_compatibles)
  one_to_many(:entitlements, key: :model_id)
end

FactoryBot.modify do
  factory :leihs_model do
    product { Faker::Commerce.product_name }
    version { Faker::Commerce.color }

    transient do
      categories { [] }
      items { [] }
      images { [] }
      properties { [] }
      recommends { [] }
    end

    created_at { DateTime.now }
    updated_at { DateTime.now }

    after(:create) do |model, trans|
      trans.categories.each do |category|
        model.add_category(category)
      end

      trans.items.each do |item|
        model.add_item(item)
      end

      trans.images.each do |image|
        model.add_image(image)
      end

      trans.properties.each do |property|
        model.add_property(property)
      end

      trans.recommends.each do |recommend|
        model.add_compatible_model(recommend)
      end
    end
  end
end
