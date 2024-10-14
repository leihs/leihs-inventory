class Delegation < Sequel::Model(:users)
  alias_method :name, :firstname

  many_to_many(:members,
    class: :User,
    left_key: :delegation_id,
    right_key: :user_id,
    join_table: :delegations_users)

  many_to_one(:responsible_user,
    key: :delegator_user_id,
    class: :User)
end

FactoryBot.modify do
  factory :delegation do
    firstname { Faker::Name.unique.last_name + "-" + Faker::Name.unique.last_name }
    delegator_user_id { create(:user).id }
  end
end
