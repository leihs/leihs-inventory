class Accessory < Sequel::Model
  many_to_one(:leihs_model, key: :model_id)
end

FactoryBot.define do
  factory :accessory do
    leihs_model
    name { Faker::Name.name }
  end
end
