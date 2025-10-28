class Field < Sequel::Model
end

class DisabledField < Sequel::Model
  many_to_one :inventory_pool
  many_to_one :field
end

FactoryBot.define do
  factory :field do
    transient do
      field_name { "#{Faker::Lorem.word}_#{SecureRandom.hex(4)}" }
    end

    id { "properties_#{field_name}" }
    active { true }
    dynamic { true }
    position { 0 }
    data do
      Sequel.pg_jsonb({
        label: Faker::Lorem.word.capitalize,
        type: "text",
        attribute: ["properties", field_name],
        target_type: "item",
        required: false,
        permissions: {
          role: "inventory_manager",
          owner: false
        }
      })
    end
  end
end
