class Template < Sequel::Model(:model_groups)
  many_to_many :parents,
    class: self,
    left_key: :child_id,
    right_key: :parent_id,
    join_table: :model_group_links

  many_to_many :children,
    class: self,
    left_key: :parent_id,
    right_key: :child_id,
    join_table: :model_group_links

  many_to_many :direct_models,
    class: :LeihsModel,
    left_key: :model_group_id,
    right_key: :model_id,
    join_table: :model_links

  one_to_many :model_links, key: :model_group_id

  many_to_many :inventory_pools,
    class: :InventoryPool,
    left_key: :model_group_id,
    right_key: :inventory_pool_id,
    join_table: :inventory_pools_model_groups
end

FactoryBot.define do
  factory :template do
    name { Faker::Commerce.department(max: 2) }
    type { "Template" }

    created_at { DateTime.now }
    updated_at { DateTime.now }

    transient do
      parents { [] }
      children { [] }
      direct_models { [] }
      inventory_pool { nil }
    end

    after(:create) do |template, evaluator|
      evaluator.parents.each { |parent| template.add_parent(parent) }
      evaluator.children.each { |child| template.add_child(child) }
      evaluator.direct_models.each { |model| template.add_direct_model(model) }

      if evaluator.inventory_pool
        template.add_inventory_pool(evaluator.inventory_pool)
      end
    end
  end
end
