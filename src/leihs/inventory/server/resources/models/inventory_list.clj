(ns leihs.inventory.server.resources.models.inventory-list
  (:require
   [clojure.set]
   [honey.sql :as sq :refer [format] :rename {format sql-format}]

   [honey.sql.helpers :as sql]
   ;[leihs.inventory.server.resources.models.queries :refer [accessories-query attachments-query base-pool-query
   ;                                                         entitlements-query item-query
   ;                                                         model-links-query properties-query]]
   ;[leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.helper :refer [convert-map-if-exist]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response fetch-pagination-params]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]])
  (:import [java.net URL JarURLConnection]
   (java.time LocalDateTime)
   [java.util.jar JarFile]))
(def pagination-query
  "SELECT * FROM (
      SELECT
          m.id,
          m.product,
          m.is_package,
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

          it.inventory_pool_id AS it_inventory_pool_id,
          it.inventory_code AS it_inventory_code,
          it.last_check AS it_last_check,
          it.retired AS it_retired,
          it.is_broken AS it_is_broken,
          it.is_incomplete AS it_is_incomplete,
          it.is_borrowable AS it_is_borrowable,
          it.owner_id AS it_owner_id,
          
          
          
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

      UNION

      SELECT o.id, o.product, false as is_package, 'Option' as type, 'Option' as entry_type,
            NULL as item_id, o.inventory_pool_id, o.inventory_code,
             
            NULL as item_last_check,
            NULL as item_retired,
            NULL as item_is_broken,
            NULL as item_is_incomplete,
            NULL as item_is_borrowable,
            NULL as item_owner_id,


            NULL as it_inventory_pool_id,
            NULL as it_inventory_code,

            NULL as it_last_check,
            NULL as it_retired,
            NULL as it_is_broken,
            NULL as it_is_incomplete,
            NULL as it_is_borrowable,
            NULL as it_owner_id,

            NULL as it_id, false as deletable
             
      FROM options o

  ) AS x

    WHERE (?::UUID IS NULL OR x.id > ?::UUID)

    AND x.entry_type = ANY(?::text[])

  ORDER BY x.id ASC
  LIMIT ?")




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

(defn grouped-data [data]
  (->> data
    (group-by :id)
    (map (fn [[id items]]
           (let [first-item (first items)]
             {:id id
              :deletable (:deletable first-item)
              :product (:product first-item)
              :entry_type (:entry_type first-item)
              :children (->> items
                          (group-by :item_id)
                          (map (fn [[item_id subitems]]
                                 (if (nil? item_id)
                                   {:item_id nil}
                                   ;; Merge renamed keys into the map
                                   (merge
                                     {:item_id item_id
                                      :children (->> subitems
                                                  (keep #(when (:it_id %)
                                                           ;; Rename `it_*` attributes inside `children`
                                                           (rename-keys (select-keys % [:it_id
                                                                                        :it_last_check
                                                                                        :it_retired
                                                                                        :it_is_broken
                                                                                        :it_is_incomplete
                                                                                        :it_is_borrowable
                                                                                        :it_owner_id]))))
                                                  vec)}
                                     ;; Rename `item_*` attributes outside children
                                     (rename-keys (first subitems))))))
                          (remove #(and (not (:item_id %)) (empty? (:children %))))
                          vec)})))
    vec))



;; Example Usage
(def example-data
  [{:id "00dc4a77-9ca2-456d-8e14-bd69e18cd016"
    :deletable false
    :product "Videoregie SDI / HDMI"
    :entry_type "Package"
    :item_id "c060938b-c8a3-48c8-a4c1-34db90c4f6fd"
    :inventory_pool_id "27b7e10b-66ad-5dcc-ae73-4b11551dadfe"
    :item_last_check "2024-03-01"
    :item_retired false
    :item_is_broken false
    :item_is_incomplete false
    :item_is_borrowable true
    :item_owner_id "owner-123"
    :it_id "df03cce3-5f98-4497-8ad0-4813b9732e76"
    :it_last_check "2024-02-15"
    :it_retired false
    :it_is_broken false
    :it_is_incomplete false
    :it_is_borrowable true
    :it_owner_id "it-owner-456"}

   {:id "00dc4a77-9ca2-456d-8e14-bd69e18cd016"
    :deletable false
    :product "Videoregie SDI / HDMI"
    :entry_type "Package"
    :item_id "c060938b-c8a3-48c8-a4c1-34db90c4f6fd"
    :inventory_pool_id "27b7e10b-66ad-5dcc-ae73-4b11551dadfe"
    :item_last_check "2024-03-01"
    :item_retired false
    :item_is_broken false
    :item_is_incomplete false
    :item_is_borrowable true
    :item_owner_id "owner-123"
    :it_id nil} ;; Should be removed from children

   {:id "02104543-a3d5-5130-8d61-31ab1c856287"
    :deletable true
    :product "MS_Foe62_MAF_FC01"
    :entry_type "Package"
    :item_id nil
    :inventory_pool_id nil
    :item_last_check nil
    :item_retired nil
    :item_is_broken nil
    :item_is_incomplete nil
    :item_is_borrowable nil
    :item_owner_id nil
    :it_id nil} ;; Should be kept but no children

   {:id "02df06bb-6377-4652-83f0-cd35558f2b52"
    :deletable false
    :product "Streaming-Rack AVS \" ATEM mini Pro\""
    :entry_type "Package"
    :item_id "7d75004a-a25f-400f-b20a-0bfe59b22f9b"
    :inventory_pool_id "27b7e10b-66ad-5dcc-ae73-4b11551dadfe"
    :item_last_check "2024-01-10"
    :item_retired false
    :item_is_broken false
    :item_is_incomplete false
    :item_is_borrowable true
    :item_owner_id "owner-789"
    :it_id nil}]) ;; Should be kept but no children

(prn (grouped-data example-data))


(defn get-paginated-data
  "Fetches paginated data using keyset pagination.
   - `page-size`: Number of records to return.
   - `cursor-id`: The last seen UUID (or nil for the first page)."
  [request page-size cursor-id entry-type process-grouping]
  (println ">o> abc1" page-size cursor-id entry-type process-grouping)



     (let [
  res (jdbc/execute! (:tx request) [pagination-query cursor-id cursor-id  (into-array entry-type) page-size])

           res (if (= process-grouping true)
                 (grouped-data res)
                 res)

              ]res)

  ;(jdbc/execute! (:tx request) [pagination-query nil nil page-size]))
  ;(jdbc/execute! (:tx request) [pagination-query cursor-id cursor-id  (into-array entry-type) page-size]))

  )

;; Example Usage:

;; First page (cursor-id = nil, page-size = 10)
;(prn (get-paginated-data 10 nil))

;; Next page (fetch after given UUID)
;(prn (get-paginated-data 10 "03f57b2b-070a-56d3-98b5-3a017427ef1d"))
