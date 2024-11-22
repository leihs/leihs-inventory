(ns leihs.inventory.server.resources.models.form.license.queries
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
               WHERE f.active = true) ff"]])))

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

(defn inventory-manager-item-subquery [query] query
;-- item -- inventory-manager -- ok 29
;select *
;from (select f.id,
;       f.data ->> 'label'                      as label,
;       f.active,
;       f.position,
;
;       COALESCE(f.data ->> 'group', 'none')    AS group,
;       COALESCE(f.data -> 'target_type', '""') AS target,
;
;                 f.data -> 'permissions' -> 'role'       as role,
;                 f.data -> 'permissions' -> 'owner'      as owner
;                 from fields f
;                 where f.active = true) as ff
;
;       where (ff.group in ('General Information', 'Invoice Information', 'Status', 'Inventory', 'Invoice Information', 'none', 'Location', 'Eigenschaften'))
;       and (ff.target in ('"item"', '""'))
;             --     and (ff.role is null or ff.role = '"lending_manager"')
;                          --    or (ff.group = 'none' and ff.target != '"license"')
;
;                                     order by ff.group, ff.position;
  )
(defn lending-manager-item-subquery [query]
  query
  ; -- item -- lending-manager -- ok 21, quantity?
  ; select *
  ; from (select f.id,
  ;        f.data ->> 'label'                      as label,
  ;        f.active,
  ;        f.position,
  ;
  ;        COALESCE(f.data ->> 'group', 'none')    AS group,
  ;        COALESCE(f.data -> 'target_type', '""') AS target,
  ;
  ;                  f.data -> 'permissions' -> 'role'       as role,
  ;                  f.data -> 'permissions' -> 'owner'      as owner
  ;                  from fields f
  ;                  where f.active = true) as ff
  ;
  ;        where (ff.group in ('General Information', 'Invoice Information', 'Status', 'Inventory', 'Invoice Information', 'none', 'Location', 'Eigenschaften'))
  ;        and (ff.target in ('"item"', '""'))
  ;              and (ff.role is null or ff.role = '"lending_manager"')
  ;                    --    or (ff.group = 'none' and ff.target != '"license"')
  ;                               -- or ff.id like '%qu%'
  ;
  ;                               order by ff.group, ff.position;
  )
