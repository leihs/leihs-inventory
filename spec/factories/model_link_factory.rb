FactoryBot.define do
  factory :model_link do
    model_group { FactoryBot.create :category }
    model { FactoryBot.create :leihs_model }
    quantity { 1 }
  end
end
