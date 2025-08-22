FactoryBot.define do
  factory :template do
    name { Faker::Commerce.department(max: 2) }
    type { "Template" }

    created_at { DateTime.now }
    updated_at { DateTime.now }

    transient do
      # parents { [] }
      # children { [] }
      # direct_models { [] }
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
