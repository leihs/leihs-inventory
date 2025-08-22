class Template < Sequel::Model(:model_groups)
  many_to_many :parents,
    class: self,
    left_key: :child_id,
    right_key: :parent_id,
    join_table: :model_group_links

  many_to_many :children,
    class: self,
    left_key: :parent_id,
    right_key: :child_id,
    join_table: :model_group_links

  many_to_many :direct_models,
    class: :LeihsModel,
    left_key: :model_group_id,
    right_key: :model_id,
    join_table: :model_links

  one_to_many :model_links, key: :model_group_id

  many_to_many :inventory_pools,
    class: :InventoryPool,
    left_key: :model_group_id,
    right_key: :inventory_pool_id,
    join_table: :inventory_pools_model_groups
end
