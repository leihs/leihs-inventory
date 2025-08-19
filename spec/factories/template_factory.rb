FactoryBot.define do
  factory :template do
    name { Faker::Commerce.department(max: 2) }
    type { "Template" }

    created_at { DateTime.now }
    updated_at { DateTime.now }

    transient do
      direct_models { [] }
      inventory_pool { nil }
    end

    after(:create) do |template, evaluator|
      evaluator.direct_models.each { |model| template.add_direct_model(model) }

      if evaluator.inventory_pool
        template.add_inventory_pool(evaluator.inventory_pool)
      end
    end
  end
end
