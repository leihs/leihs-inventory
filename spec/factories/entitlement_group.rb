class EntitlementGroup < Sequel::Model(:entitlement_groups)
  many_to_one :inventory_pool
end

FactoryBot.define do
  factory :entitlement_group do
    created_at { Time.now }
    updated_at { Time.now }
    name { Faker::Name.last_name }
    inventory_pool_id { FactoryBot.create(:inventory_pool).id }
  end
end
