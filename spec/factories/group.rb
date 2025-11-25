class Group < Sequel::Model
  many_to_many :users,
    left_key: :group_id,
    right_key: :user_id,
    join_table: :groups_users
end

FactoryBot.modify do
  factory :group do
    name { Faker::Company.unique.name }
    description { Faker::Lorem.sentence }
    admin_protected { rand < 0.5 }
    system_admin_protected { admin_protected && (rand < 0.5) }
  end
end
