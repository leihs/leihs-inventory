(ns leihs.inventory.server.resources.models.inventory-list
  (:require
   [clojure.set]
   [honey.sql :as sq :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [clojure.walk :as walk]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]]
   [clojure.string :as str]))

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


(defn quote-sql-value [v]
  (cond
    (string? v) (str "'" (clojure.string/replace v "'" "''") "'")
    (nil? v) "NULL"
    :else (str v)))

(defn inject-sql-params [query-str params]
  (loop [query query-str
         ps params]
    (if (empty? ps)
      query
      (recur (clojure.string/replace-first query #"\?" (quote-sql-value (first ps)))
        (rest ps)))))




(defn generic-query-or-count [inventory_pool_id search_str last_check  entry-type process-query]



  (str (if process-query "SELECT *" "SELECT COUNT(*) AS total")
    " FROM (
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
          END AS is_deletable
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
          NULL as it_id,
          false as is_deletable
      FROM options o
  ) AS x
  WHERE 1=1
  "

    (if entry-type (str " AND x.entry_type = '" entry-type "' ") "")




    (when inventory_pool_id (str " AND x.inventory_pool_id = '" (str inventory_pool_id) "' "))
    (when search_str (str " AND x.product ILIKE '%" search_str "%' "))
    (when last_check (str " AND (x.item_last_check IS NULL OR x.item_last_check >= '" last_check "' ) "))


    (if process-query " ORDER BY x.id ASC LIMIT ? OFFSET ?" "")


    ))


(defn create-pagination-count [inventory_pool_id search_str last_check entry-type]
  (generic-query-or-count inventory_pool_id search_str last_check entry-type  false)
  )

(defn create-pagination-query [inventory_pool_id search_str last_check entry-type]
  (generic-query-or-count inventory_pool_id search_str last_check entry-type  true)
  )


(defn total-rows-query [inventory_pool_id search_str last_check]
  
          ;LEFT JOIN rooms r ON r.id = i.room_id
          ;LEFT JOIN buildings b ON b.id = r.building_id
          ;LEFT JOIN models itm ON itm.id = it.model_id
          ;LEFT JOIN images im ON m.id = im.target_id AND im.thumbnail = TRUE
          ;'Model' AS entry_type

  (str "SELECT COUNT(*) AS total FROM (
      SELECT x.id, x.entry_type FROM (
          SELECT
          m.id,

          CASE
              WHEN m.is_package = TRUE and m.type ='Model' THEN 'Package'
              WHEN m.is_package = FALSE and m.type ='Model' THEN 'Model'
              WHEN m.is_package = FALSE and m.type ='Software' THEN 'Software'
              ELSE 'unknown'
          END AS entry_type
          
          FROM models m
          LEFT JOIN items i ON i.model_id = m.id
          LEFT JOIN items it ON i.id = it.parent_id AND i.parent_id IS NULL
          UNION
          SELECT o.id, 'Option' AS entry_type FROM options o
      ) AS x
      WHERE x.entry_type = ANY(?::text[])"

    ;; Filters applied dynamically
    (when inventory_pool_id (str " AND x.inventory_pool_id = '" (str inventory_pool_id) "' "))
    (when search_str (str " AND x.product ILIKE '%" search_str "%' "))
    (when last_check (str " AND (x.item_last_check IS NULL OR x.item_last_check >= '" last_check "' ) "))

    ") AS count_table"))  ;; Correct closing of the query



(defn rename-keys [m]
  "Removes `item_` and `it_` prefixes from map keys."
  (into {} (map (fn [[k v]]
                  (let [new-k (cond
                                (str/starts-with? (name k) "item_")
                                (keyword (subs (name k) 5))
                                (str/starts-with? (name k) "it_")
                                (keyword (subs (name k) 3))
                                :else k)]
                    [new-k v]))
             m)))

(defn grouped-data [data]
  (->> data
    (group-by :id)
    (map (fn [[id items]]
           (let [first-item (first items)]
             {:id id
              :is_deletable (:is_deletable first-item)
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
                                                                      :item_properties]))]
                                     (merge item-details
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
                                                                                          :it_entry_type]))))
                                                    vec)})))))
                          (remove #(and (not (:item_id %)) (empty? (:children %))))
                          vec)})))
    vec))


(defn grouped-data [data]
  (->> data
    (group-by :id)
    (map (fn [[id items]]
           (let [first-item (first items)]
             {:id id
              :is_deletable (:is_deletable first-item)
              :product (:product first-item)
              :version (:version first-item)
              :image_id (:image_id first-item)
              :entry_type (:entry_type first-item)
              :children (->> items
                          (group-by :item_id)
                          (map (fn [[item_id subitems]]
                                 (if (nil? item_id)
                                   {:item_id nil
                                    :is_deletable false} ;; <- Add here if needed
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
                                                                      :item_properties]))]
                                     (merge item-details
                                       {:is_deletable false ;; <- Add here
                                        :children (->> subitems
                                                    (keep #(when (:it_id %)
                                                             (assoc (rename-keys (select-keys % [:it_id
                                                                                                 :it_last_check
                                                                                                 :it_retired
                                                                                                 :it_is_broken
                                                                                                 :it_is_incomplete
                                                                                                 :it_is_borrowable
                                                                                                 :it_owner_id
                                                                                                 :it_product
                                                                                                 :it_entry_type]))
                                                               :is_deletable false))) ;; <- And here
                                                    vec)})))))
                          (remove #(and (not (:item_id %)) (empty? (:children %))))
                          vec)})))
    vec))


(defn ensure-is-deletable [m]
  (if (contains? m :is_deletable)
    (if (nil? (:is_deletable m))
      (assoc m :is_deletable false)
      m)
    (assoc m :is_deletable false)))

(defn grouped-data [data]
  (->> data
    (group-by :id)
    (map (fn [[id items]]
           (let [first-item (ensure-is-deletable (first items))]
             {:id id
              :is_deletable (:is_deletable first-item)
              :product (:product first-item)
              :version (:version first-item)
              :image_id (:image_id first-item)
              :entry_type (:entry_type first-item)
              :children (->> items
                          (group-by :item_id)
                          (map (fn [[item_id subitems]]
                                 (if (nil? item_id)
                                   {:item_id nil}
                                   (let [item-details (-> (rename-keys (select-keys (first subitems)
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
                                                                          :item_properties]))
                                                        ensure-is-deletable)]
                                     (merge item-details
                                       {:children (->> subitems
                                                    (keep #(when (:it_id %)
                                                             (-> (rename-keys (select-keys % [:it_id
                                                                                              :it_last_check
                                                                                              :it_retired
                                                                                              :it_is_broken
                                                                                              :it_is_incomplete
                                                                                              :it_is_borrowable
                                                                                              :it_owner_id
                                                                                              :it_product
                                                                                              :it_entry_type]))
                                                               ensure-is-deletable)))
                                                    vec)})))))
                          (remove #(and (not (:item_id %)) (empty? (:children %))))
                          vec)})))
    vec))


(defn get-paginated-data
  "Fetches paginated data with pagination info."
  [request page page-size entry-type process-grouping inventory_pool_id search_str last_check]
  (let [
        ;; TODO: make separate reduced query for total-rows

        p (println ">o> abc.total-query0.page" page)
        p (println ">o> abc.total-query0.page-size" page-size)
        p (println ">o> abc.total-query0.entry-type" entry-type)
        p (println ">o> abc.total-query0.process-grouping" process-grouping)
        p (println ">o> abc.total-query0.inventory_pool_id" inventory_pool_id)
        p (println ">o> abc.total-query0.search_str" search_str)
        p (println ">o> abc.total-query0.last_check" last_check)


        offset (* (dec page) page-size)

        ;total-query (create-pagination-count inventory_pool_id search_str  last_check entry-type)
        ;p (println ">o> abc.total-query1a" total-query)


        total-query (create-pagination-count inventory_pool_id search_str  last_check entry-type)
        ;p (println ">o> abc.total-query1a" total-query)

        total-rows (-> (jdbc/execute-one! (:tx request) [total-query])
                     :total)
        p (println ">o> abc.total-query1c" total-rows)



        paged-query (inject-sql-params (create-pagination-query inventory_pool_id search_str last_check entry-type) [page-size offset])
        ;p (println ">o> abc.total-query1b" paged-query)

        paged-result (jdbc/execute! (:tx request) [paged-query])
        ;p (println ">o> abc.total-query1d" paged-result)
        p (println ">o> abc.total-query1e" (count paged-result))




        ;;total-rows (jdbc/execute-one! (:tx request) [(create-pagination-count inventory_pool_id search_str last_check ) (into-array entry-type)])
        ;total-rows (jdbc/execute-one! (:tx request) [(create-pagination-count inventory_pool_id search_str last_check (into-array entry-type)) ])
        ;p (println ">o> abc.total-rows1" total-rows)
        ;
        ;
        ;
        ;total-rows (-> (jdbc/execute-one! (:tx request) [(create-pagination-count inventory_pool_id search_str last_check (into-array entry-type))])
        ;;total-rows (-> (jdbc/execute-one! (:tx request) [(create-pagination-count inventory_pool_id search_str last_check) (into-array entry-type)])
        ;             :total)
        ;p (println ">o> abc.paged-result" total-rows)


        p (println ">o> abc.total-pages1" total-rows page-size)


        total-pages (if (zero? total-rows) 1 (Math/ceil (/ total-rows page-size)))

;; check if total-pages is a number
total-pages (if (number? total-pages)
              (int total-pages)
              (throw (ex-info "The anti-csrf-token cookie value is not set." {:status 409})))


        p (println ">o> abc.total-pages2" total-pages)

        ;
        ;res (jdbc/execute! (:tx request) [(create-pagination-query inventory_pool_id search_str last_check) (into-array entry-type) page-size offset])
        ;res (jdbc/execute! (:tx request) [(create-pagination-query inventory_pool_id search_str last_check) (into-array entry-type) page-size offset])
        ;p (println ">o> abc.res" res)
        ;
        paged-result (if process-grouping (clean-keys (grouped-data paged-result)) paged-result)

        ;res "nix"
        ]
        ;res (if clean-keys (process-grouping res) res)]
    {:data paged-result
     :pagination {:total_rows total-rows
                  :total_pages total-pages
                  :page page
                  :size page-size}
     }))
