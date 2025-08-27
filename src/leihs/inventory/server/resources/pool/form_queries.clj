(ns leihs.inventory.server.resources.pool.form-queries
  (:require
   [honey.sql.helpers :as sql]))

(defn package-base-query [item-id model-id pool-id]
  (-> (sql/select
       :i.* [:s.id :supplier_id] [:s.name :supplier_name] [:m.id :model_id] [:m.product :product_name]
       :r.building_id)
      (sql/from [:models :m])
      (sql/join [:items :i] [:= :m.id :i.model_id])
      (sql/join [:rooms :r] [:= :r.id :i.room_id])
      (sql/left-join [:suppliers :s] [:= :i.supplier_id :s.id])
      (sql/where [:= :i.id item-id] [:= :i.model_id model-id] [:= :i.inventory_pool_id pool-id])))

(defn inventory-manager-package-subquery [query]
  (-> query
      (sql/from [[:raw
                  "(SELECT
                  f.id,
                  f.active,
                  f.position,
                  f.data,

                  jsonb_extract_path_text(f.data, 'label') AS label,

                  jsonb_extract_path_text(f.data, 'group') AS group,
                  COALESCE(jsonb_extract_path_text(f.data, 'group'), 'none') AS group_default,

                  jsonb_extract_path_text(f.data, 'target_type') AS target,
                  COALESCE(jsonb_extract_path_text(f.data, 'target_type'), '') AS target_default,
                            jsonb_extract_path_text(f.data, 'permissions', 'role') AS role,
                            COALESCE(jsonb_extract_path_text(f.data, 'permissions', 'role'), '') AS role_default,
                                      jsonb_extract_path_text(f.data, 'permissions', 'owner') AS owner
                                      FROM fields f
                                      WHERE f.active = true) ff
                            WHERE (ff.group_default IN ('Inventory',  'Status',  'Location', 'none'))
                            AND (ff.target_default IN ('item', ''))
                                  OR ff.id in ('inventory_code', 'model_id', 'price', 'last_check', 'note')
                                  ORDER BY ff.group_default, ff.position
                                  "]])))

(defn lending-manager-package-subquery [query]
  (-> query
      (sql/from [[:raw
                  "(SELECT
                  f.id,
                  f.active,
                  f.position,
                  f.data,

                  jsonb_extract_path_text(f.data, 'label') AS label,

                  jsonb_extract_path_text(f.data, 'group') AS group,
                  COALESCE(jsonb_extract_path_text(f.data, 'group'), 'none') AS group_default,

                  jsonb_extract_path_text(f.data, 'target_type') AS target,
                  COALESCE(jsonb_extract_path_text(f.data, 'target_type'), '') AS target_default,

                            jsonb_extract_path_text(f.data, 'permissions', 'role') AS role,
                            COALESCE(jsonb_extract_path_text(f.data, 'permissions', 'role'), '') AS role_default,

                                      jsonb_extract_path_text(f.data, 'permissions', 'owner') AS owner

                                      FROM fields f
                                      WHERE f.active = true) ff

                            WHERE (ff.group_default IN (  'Status',  'Location', 'none')) -- lending_manager

                            AND (ff.target_default IN ('item', ''))
                                  OR ff.id in ('inventory_code', 'model_id', 'price', 'last_check', 'note')
                                  ORDER BY ff.group_default, ff.position"]])))
