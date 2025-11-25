# frozen_string_literal: true

class EntitlementGroup < Sequel::Model(:entitlement_groups)
  many_to_one :inventory_pool
  one_to_many :entitlements
  many_to_many :users,
    left_key: :entitlement_group_id,
    right_key: :user_id,
    join_table: :entitlement_groups_direct_users
  many_to_many :groups,
    left_key: :entitlement_group_id,
    right_key: :group_id,
    join_table: :entitlement_groups_groups
end

FactoryBot.define do
  factory :entitlement_group do
    created_at { Time.now }
    updated_at { Time.now }
    name { Faker::Name.last_name }
    inventory_pool_id { FactoryBot.create(:inventory_pool).id }
    is_verification_required { false }

    transient do
      models { [] }
      users { [] }
      groups { [] }
    end

    after(:create) do |entitlement_group, trans|
      trans.models.each do |model_config|
        if model_config.is_a?(Hash)
          model = model_config[:model]
          quantity = model_config[:quantity] || 1
        else
          model = model_config
          quantity = 1
        end

        FactoryBot.create(:entitlement,
          entitlement_group: entitlement_group,
          leihs_model: model,
          quantity: quantity)
      end

      trans.users.each do |user|
        entitlement_group.add_user(user)
      end

      trans.groups.each do |group|
        entitlement_group.add_group(group)
      end
    end
  end
end
