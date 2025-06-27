(ns leihs.inventory.server.resources.pool.models.form.license.queries
  (:require
   [honey.sql.helpers :as sql]))

(defn model-query [item-id model-id pool-id]
  (-> (sql/select :m.id :m.product :m.manufacturer :m.version :m.type
                  :m.hand_over_note :m.description :m.internal_description
                  :m.technical_detail :m.is_package :i.* [:s.id :supplier_id] [:s.name :supplier_name])
      (sql/from [:models :m])
      (sql/join [:items :i] [:= :m.id :i.model_id])
      (sql/left-join [:suppliers :s] [:= :i.supplier_id :s.id])
      (sql/where [:= :i.id item-id] [:= :i.model_id model-id] [:= :i.inventory_pool_id pool-id])))

(defn item-base-query [item-id model-id pool-id]
  (-> (sql/select
        ;:m.id :m.product :m.manufacturer :m.version :m.type
        ;          :m.hand_over_note :m.description :m.internal_description
        ;          :m.technical_detail :m.is_package :i.* [:s.id :supplier_id] [:s.name :supplier_name]

       :i.* [:s.id :supplier_id] [:s.name :supplier_name] [:m.id :model_id] [:m.product :product_name]
       :r.building_id)
      (sql/from [:models :m])
      (sql/join [:items :i] [:= :m.id :i.model_id])
      (sql/join [:rooms :r] [:= :r.id :i.room_id])
      (sql/left-join [:suppliers :s] [:= :i.supplier_id :s.id])
      (sql/where [:= :i.id item-id] [:= :i.model_id model-id] [:= :i.inventory_pool_id pool-id])))

(defn package-base-query [item-id model-id pool-id]
  (-> (sql/select
        ;:m.id :m.product :m.manufacturer :m.version :m.type
        ;          :m.hand_over_note :m.description :m.internal_description
        ;          :m.technical_detail :m.is_package :i.* [:s.id :supplier_id] [:s.name :supplier_name]

       :i.* [:s.id :supplier_id] [:s.name :supplier_name] [:m.id :model_id] [:m.product :product_name]
       :r.building_id)
      (sql/from [:models :m])
      (sql/join [:items :i] [:= :m.id :i.model_id])
      (sql/join [:rooms :r] [:= :r.id :i.room_id])
      (sql/left-join [:suppliers :s] [:= :i.supplier_id :s.id])
      (sql/where [:= :i.id item-id] [:= :i.model_id model-id] [:= :i.inventory_pool_id pool-id])))

(defn license-base-query [query]
  (-> query
      (sql/from [[:raw
                  "(SELECT
                  f.id,
                  f.active,
                  f.position,
                  f.data,
                  jsonb_extract_path_text(f.data, 'label')  AS label,

                  jsonb_extract_path_text(f.data, 'group')  AS group,
                  COALESCE(jsonb_extract_path_text(f.data, 'group'), '\"none\"') AS group_default,

                  jsonb_extract_path_text(f.data, 'target_type')  AS target,
                  COALESCE(jsonb_extract_path_text(f.data, 'target_type'), '\"\"') AS target_default,

                  jsonb_extract_path_text(f.data, 'permissions', 'role') AS role,
                  COALESCE(jsonb_extract_path_text(f.data, 'permissions', 'role'), '\"\"') AS role_default,

                  jsonb_extract_path_text(f.data, 'permissions', 'owner') AS owner

               FROM fields f
               WHERE f.active = true
               OR f.id = 'inventory_code') ff"]])))

(defn inventory-manager-license-subquery [query]
  (-> query
      (sql/where [:and
                  [:in :ff.target_default ["license" "\"\""]]
                  [:or
                   [:in :ff.role_default ["inventory_manager" "\"\""]]
                   [:or
                    [:is :ff.target nil]
                    [:is :owner nil]]]])))

(defn lending-manager-license-subquery [query]
;; 12 results for lending-manager, TODO: determine owner_id
  (-> query
      (sql/where
       [:not-in :ff.id ["license_version"]]
       [:or
        [:and
         [:in :ff.group_default ["General Information" "Invoice Information" "Status" "\"none\""]]
         [:in :ff.target_default ["license" "\"\""]]
         [:in :ff.role_default ["lending_manager" "\"\""]]]
        [:and
         [:= :ff.group_default "\"none\""]
         [:<> :ff.target_default "item"]]])))

(defn inventory-manager-item-subquery [query]
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

                WHERE (ff.group_default IN ('General Information', 'Invoice Information', 'Status', 'Inventory', 'Location', 'Eigenschaften', 'none'))
                AND (ff.target_default IN ('item', ''))
                OR ff.id = 'inventory_code'
                ORDER BY ff.group_default, ff.position"]])))

(defn lending-manager-item-subquery [query]
  (-> query
      (sql/from [[:raw
                  "(SELECT *
                    FROM (
                        SELECT
                          f.id,
                          jsonb_extract_path_text(f.data, 'label') AS label,
                          f.active,
                          f.position,
                          f.data,

                          COALESCE(jsonb_extract_path_text(f.data, 'group'), 'none') AS group,
                          COALESCE(jsonb_extract_path_text(f.data, 'target_type'), '') AS target,

                          jsonb_extract_path_text(f.data, 'permissions', 'role') AS role,
                          jsonb_extract_path_text(f.data, 'permissions', 'owner') AS owner
                        FROM fields f
                        WHERE f.active = true
                    ) AS ff
                    WHERE (COALESCE(ff.group, 'none') IN
                          ('General Information', 'Invoice Information', 'Status', 'Inventory', 'Location', 'Eigenschaften', 'none'))
                      AND (COALESCE(ff.target, '') IN ('item', ''))
                      AND (ff.role IS NULL OR ff.role = 'lending_manager')
                      OR ff.id = 'inventory_code'
                    ORDER BY ff.group, ff.position) k"]])))

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

                                  ;OR ff.id in ('inventory_code', 'price', 'last_check', 'note')
                                  ;OR ff.id in ('inventory_code', 'model_id', 'price', 'last_check', 'note')

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

;; TODO: queries for package