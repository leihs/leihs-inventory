FactoryBot.modify do
  factory :group do
    name { Faker::Company.unique.name }
    description { Faker::Lorem.sentence }
    admin_protected { rand < 0.5 }
    system_admin_protected { admin_protected && (rand < 0.5) }
  end
end
