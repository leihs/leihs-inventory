class Item
  many_to_one(:leihs_model, key: :model_id)
  many_to_one(:responsible, class: :InventoryPool, key: :inventory_pool_id)
  many_to_one(:owner, class: :InventoryPool, key: :owner_id)
  many_to_one(:room)
  many_to_one(:supplier)
end

FactoryBot.modify do
  factory :item do
    inventory_code { Faker::Alphanumeric.alphanumeric(number: 10) }
    leihs_model
    association :owner, factory: :inventory_pool
    association :responsible, factory: :inventory_pool
    room
    supplier

    is_borrowable { true }

    created_at { DateTime.now }
    updated_at { DateTime.now }

    after(:build) do |item|
      item.responsible = item.owner unless item.responsible
    end
  end
end
