class InventoryPool
  one_to_many(:items)
end

FactoryBot.modify do
  factory :inventory_pool do
    name { Faker::Commerce.department(max: 5, fixed_amount: true) }
  end
end
