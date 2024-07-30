class Field < Sequel::Model
end

class DisabledField < Sequel::Model
  many_to_one :inventory_pool
  many_to_one :field
end

FactoryBot.define do
  factory :field do
  end
end
