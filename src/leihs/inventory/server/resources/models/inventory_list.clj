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
          it.id AS it_id,
          CASE
              WHEN m.is_package = TRUE and m.type ='Model' and i.id is null and it.id is null THEN true
              WHEN m.is_package = FALSE and m.type ='Model' and i.id is null and it.id is null THEN true
              WHEN m.is_package = FALSE and m.type ='Software' and i.id is null and it.id is null THEN true
              ELSE false
          END AS to_delete
      FROM models m
          LEFT JOIN items i ON i.model_id = m.id
          LEFT JOIN items it ON i.id = it.parent_id AND i.parent_id IS NULL

      UNION

      SELECT o.id, o.product, false as is_package, 'Option' as type, 'Option' as entry_type,
             NULL as item_id, o.inventory_pool_id, NULL as it_id, false as to_delete
      FROM options o

  ) AS x

    WHERE (?::UUID IS NULL OR x.id > ?::UUID)

  ORDER BY x.id ASC
  LIMIT ?")

  ;WHERE (? IS NULL OR x.id > ?)
  ;WHERE x.id > COALESCE(?::UUID, '00000000-0000-0000-0000-000000000000'::UUID)

(defn get-paginated-data
  "Fetches paginated data using keyset pagination.
   - `page-size`: Number of records to return.
   - `cursor-id`: The last seen UUID (or nil for the first page)."
  [request page-size cursor-id]
  (println ">o> abc1" page-size cursor-id)
  ;(jdbc/execute! (:tx request) [pagination-query nil nil page-size]))
  (jdbc/execute! (:tx request) [pagination-query cursor-id cursor-id page-size]))

;; Example Usage:

;; First page (cursor-id = nil, page-size = 10)
;(prn (get-paginated-data 10 nil))

;; Next page (fetch after given UUID)
;(prn (get-paginated-data 10 "03f57b2b-070a-56d3-98b5-3a017427ef1d"))
