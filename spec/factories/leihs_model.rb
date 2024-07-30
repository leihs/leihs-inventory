class LeihsModel < Sequel::Model(:models)
  def name
    [product, version].compact.join(" ")
  end
end

FactoryBot.modify do
  factory :leihs_model do
    version { Faker::Alphanumeric.alpha(number: 2).upcase }
  end
end
