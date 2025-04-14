FactoryBot.define do
  factory :package_model, parent: :leihs_model do
    is_package { true }

    factory :package_model_with_items do
      transient do
        inventory_pool { FactoryBot.create(:inventory_pool) }
      end

      after(:create) do |package, evaluator|
        3.times do
          package.items << FactoryBot.create(
            :item,
            inventory_pool_id: evaluator.inventory_pool.id,
            owner: evaluator.inventory_pool
          )
        end
      end
    end

    factory :package_model_with_parent_and_items do
      transient do
        inventory_pool { FactoryBot.create(:inventory_pool) }
      end

      after(:create) do |model, evaluator|
        parent = FactoryBot.create(
          :item,
          inventory_pool_id: evaluator.inventory_pool.id,
          owner: evaluator.inventory_pool,
          model_id: model.id
        )

        model.items << parent

        3.times do
          FactoryBot.create(
            :item,
            inventory_pool_id: evaluator.inventory_pool.id,
            owner: evaluator.inventory_pool,
            parent_id: parent.id
          )
        end
      end
    end
  end
end
