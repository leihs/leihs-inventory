class Template < Sequel::Model(:model_groups)
  many_to_many :direct_models,
    class: :LeihsModel,
    left_key: :model_group_id,
    right_key: :model_id,
    join_table: :model_links

  many_to_many :inventory_pools,
    class: :InventoryPool,
    left_key: :model_group_id,
    right_key: :inventory_pool_id,
    join_table: :inventory_pools_model_groups
end
