FactoryBot.define do
  factory :model_link_template do
    model_group { FactoryBot.create :template }
    model { FactoryBot.create :leihs_model }
    quantity { 1 }
  end
end
