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
            :package_item_with_parts,
            # inventory_pool: evaluator.inventory_pool
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

      after(:create) do |package, evaluator|
        package.items << FactoryBot.create(
          :package_item_with_parts,
          inventory_pool_id: evaluator.inventory_pool.id,
          owner: evaluator.inventory_pool
        )

        parent_id = package.items.first.id
        3.times do
          package.items << FactoryBot.create(
            :package_item_with_parts,
            inventory_pool_id: evaluator.inventory_pool.id,
            owner: evaluator.inventory_pool,
            parent_id: parent_id
          )
        end
      end
    end
  end

  factory :package_item, parent: :item do
    factory :package_item_with_parts do
      # after(:create) do |item, evaluator|
      #     3.times do
      #       binding.pry
      #       item.children << \
      #         FactoryBot.create(:item,
      #                           owner: evaluator.owner,
      #                           parent: item)
      #   end
      # end
      #
    end
  end
end
