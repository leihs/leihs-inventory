# # app/models/accessory.rb
# class Accessory <  Sequel::Model
#   belongs_to :leihs_model  # Assuming `leihs_model` is a related model
#   validates :name, presence: true
# end

class Accessory < Sequel::Model
  many_to_one(:leihs_model, key: :model_id)
end

FactoryBot.define do
  factory :accessory do
    leihs_model
    name { Faker::Name.name }
  end
end
