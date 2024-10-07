class User < Sequel::Model
  one_to_many(:direct_access_rights)
  many_to_one(:delegator_user, class: self)
  many_to_many(:delegation_users,
    left_key: :delegation_id,
    right_key: :user_id,
    class: self,
    join_table: :delegations_users)
  many_to_many(:favorite_models,
    class: LeihsModel,
    join_table: :favorite_models,
    left_key: :user_id,
    right_key: :model_id)
  many_to_one(:language, key: :language_locale)
end

FactoryBot.define do
  factory :user_base, class: User do
    created_at { Date.today }
    updated_at { Date.today }
    email { Faker::Internet.email }
    language do
      Language.find(locale: "en-GB") or
        create(:language,
          locale: "en-GB",
          name: "British English",
          default: true)
    end
    organization { Faker::Lorem.characters(number: 8) }

    transient do
      access_rights { [] }
    end

    after(:create) do |user, trans|
      trans.access_rights.each do |access_right|
        user.add_direct_access_right(access_right)
      end
    end
  end
end
