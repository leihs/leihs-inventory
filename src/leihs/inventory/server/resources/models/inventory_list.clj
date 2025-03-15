(ns leihs.inventory.server.resources.models.inventory-list
  (:require
   [clojure.set]
   [honey.sql :as sq :refer [format] :rename {format sql-format}]

   [honey.sql.helpers :as sql]
   ;[leihs.inventory.server.resources.models.queries :refer [accessories-query attachments-query base-pool-query
   ;                                                         entitlements-query item-query
   ;                                                         model-links-query properties-query]]
   ;[leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [clojure.walk :as walk]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]])

)
(def pagination-query
  "SELECT * FROM (
      SELECT
          m.id,
          m.product,
          m.version,
          m.is_package,

          im.id AS image_id,

          m.type,
          CASE
              WHEN m.is_package = TRUE and m.type ='Model' THEN 'Package'
              WHEN m.is_package = FALSE and m.type ='Model' THEN 'Model'
              WHEN m.is_package = FALSE and m.type ='Software' THEN 'Software'
              ELSE 'unknown'
          END AS entry_type,
          i.id AS item_id,
          i.inventory_pool_id,
          i.inventory_code,

          i.last_check AS item_last_check,
          i.retired AS item_retired,
          i.is_broken AS item_is_broken,
          i.is_incomplete AS item_is_incomplete,
          i.is_borrowable AS item_is_borrowable,
          i.owner_id AS item_owner_id,

          CASE
              WHEN m.is_package = TRUE and m.type ='Model' THEN 'PackageItem'
              WHEN m.is_package = FALSE and m.type ='Model' THEN 'ModelItem'
              WHEN m.is_package = FALSE and m.type ='Software' THEN 'License'
              ELSE 'unknown'
          END AS item_entry_type,

          i.properties AS item_properties,

          r.name AS item_room_name,
          b.name AS item_building_name,
          b.code AS item_building_code,

          it.inventory_pool_id AS it_inventory_pool_id,
          it.inventory_code AS it_inventory_code,
          it.last_check AS it_last_check,
          it.retired AS it_retired,
          it.is_broken AS it_is_broken,
          it.is_incomplete AS it_is_incomplete,
          it.is_borrowable AS it_is_borrowable,
          it.owner_id AS it_owner_id,
          'Item' AS it_entry_type,

          itm.product AS it_product,
          
          
          it.id AS it_id,
          CASE
              WHEN m.is_package = TRUE and m.type ='Model' and i.id is null and it.id is null THEN true
              WHEN m.is_package = FALSE and m.type ='Model' and i.id is null and it.id is null THEN true
              WHEN m.is_package = FALSE and m.type ='Software' and i.id is null and it.id is null THEN true
              ELSE false
          END AS deletable

      FROM models m
          LEFT JOIN items i ON i.model_id = m.id
          LEFT JOIN items it ON i.id = it.parent_id AND i.parent_id IS NULL

          LEFT JOIN rooms r ON r.id = i.room_id
          LEFT JOIN buildings b ON b.id = r.building_id

          LEFT JOIN models itm ON itm.id = it.model_id
          LEFT JOIN images im ON m.id = im.target_id AND im.thumbnail = TRUE

      UNION

      SELECT
            o.id,
            o.product,
            o.version,

            false as is_package,

            NULL as image_id,

            'Option' as type,
            'Option' as entry_type,
            NULL as item_id,

            o.inventory_pool_id,
            o.inventory_code,




            NULL as item_last_check,
            NULL as item_retired,
            NULL as item_is_broken,
            NULL as item_is_incomplete,
            NULL as item_is_borrowable,
            NULL as item_owner_id,
            NULL as item_entry_type,

            NULL as item_properties,

            NULL as item_room_name,
            NULL as item_building_name,
            NULL as item_building_code,


            NULL as it_inventory_pool_id,
            NULL as it_inventory_code,

            NULL as it_last_check,
            NULL as it_retired,
            NULL as it_is_broken,
            NULL as it_is_incomplete,
            NULL as it_is_borrowable,
            NULL as it_owner_id,
            NULL as it_entry_type,

            NULL as it_product,

            NULL as it_id, false as deletable
             
      FROM options o

  ) AS x

    WHERE (?::UUID IS NULL OR x.id > ?::UUID)

    AND x.entry_type = ANY(?::text[])

  ORDER BY x.id ASC
  LIMIT ?")


;o.price,


(require '[clojure.string :as str])

(defn rename-keys [m]
  "Removes `item_` and `it_` prefixes from map keys."
  (into {} (map (fn [[k v]]
                  (let [new-k (cond
                                (str/starts-with? (name k) "item_")
                                (keyword (subs (name k) 5)) ;; Remove "item_"

                                (str/starts-with? (name k) "it_")
                                (keyword (subs (name k) 3)) ;; Remove "it_"

                                :else k)]
                    [new-k v]))
             m)))

(defn rename-keys [m] m)





(defn grouped-data [data]
  (->> data
    (group-by :id)
    (map (fn [[id items]]
           (let [first-item (first items)]
             {:id id
              :deletable (:deletable first-item)
              :product (:product first-item)
              :version (:version first-item)
              :image_id (:image_id first-item)
              :entry_type (:entry_type first-item)
              :children (->> items
                          (group-by :item_id)
                          (map (fn [[item_id subitems]]
                                 (if (nil? item_id)
                                   {:item_id nil}
                                   (let [item-details (rename-keys (select-keys (first subitems)
                                                                     [:item_id
                                                                      :item_retired
                                                                      :item_is_broken
                                                                      :item_is_incomplete
                                                                      :item_is_borrowable
                                                                      :item_owner_id
                                                                      :item_entry_type
                                                                      :inventory_pool_id
                                                                      :inventory_code


                                                                      :item_room_name
                                                                      :item_building_name
                                                                      :item_building_code

                                                                      :item_properties

                                                                      ;:entry_type
                                                                      ;:product
                                                                      ]))]
                                     (merge
                                       item-details
                                       {:children (->> subitems
                                                    (keep #(when (:it_id %)
                                                             (rename-keys (select-keys % [:it_id
                                                                                          :it_last_check
                                                                                          :it_retired
                                                                                          :it_is_broken
                                                                                          :it_is_incomplete
                                                                                          :it_is_borrowable
                                                                                          :it_owner_id
                                                                                          :it_product
                                                                                          :it_entry_type

                                                                                          ]))))
                                                    vec)})))))
                          (remove #(and (not (:item_id %)) (empty? (:children %))))
                          vec)})))
    vec))





(defn grouped-dataX [data]
  (->> data
    (group-by :id)
    (map (fn [[id items]]
           (let [first-item (first items)]                  ;; Extract first item to get common attributes
             {:id id
              :deletable (:deletable first-item)
              :product (:product first-item)
              :image_id (:image_id first-item)
              :entry_type (:entry_type first-item)
              :children (->> items
                          ;; Separate items with a valid item_id from those without one
                          (group-by :item_id)
                          (map (fn [[item_id subitems]]
                                 (if (nil? item_id)
                                   ;; Keep entries with nil item_id, but no children
                                   {:item_id nil}
                                   ;; Process items with a valid item_id
                                   {:item_id item_id

                                    :inventory_pool_id (:inventory_pool_id (first subitems))
                                    :item_last_check (:item_last_check (first subitems))
                                    :item_retired (:item_retired (first subitems))
                                    :item_is_broken (:item_is_broken (first subitems))
                                    :item_is_incomplete (:item_is_incomplete (first subitems))
                                    :item_is_borrowable (:item_is_borrowable (first subitems))
                                    :item_owner_id (:item_owner_id (first subitems))



                                    :children (->> subitems

                                                ;; todo, how to add it_last_check, it_retired, it_is_broken, it_is_incomplete, it_is_borrowable, it_owner_id

                                                (keep #(when (:it_id %) (select-keys % [:it_id]))) ;; Filter out nil it_id
                                                vec)})))
                          (remove #(and (not (:item_id %)) (empty? (:children %)))) ;; Remove empty item groups
                          vec)})))
    vec))

(defn clean-keys
  "Recursively removes 'it_' and 'item_' prefixes from map keys."
  [data]
  (walk/postwalk
    (fn [x]
      (if (map? x)
        (into {}
          (map (fn [[k v]]
                 [(keyword (clojure.string/replace (name k) #"^(it_|item_)" "")) v]))
          x)
        x))
    data))

(defn get-paginated-data
  "Fetches paginated data using keyset pagination.
   - `page-size`: Number of records to return.
   - `cursor-id`: The last seen UUID (or nil for the first page)."
  [request page-size cursor-id entry-type process-grouping]
  (println ">o> abc1" page-size cursor-id entry-type process-grouping)



  (let [
        res (jdbc/execute! (:tx request) [pagination-query cursor-id cursor-id (into-array entry-type) page-size])
        res (if (= process-grouping true)
              (clean-keys (grouped-data res))
              res)

        ] res))

