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

(def query
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
             LEFT JOIN items it ON i.id = it.parent_id
      AND i.parent_id IS NULL
    UNION
    SELECT o.id, o.product, false as is_package, 'Option' as type,  'Option' as entry_type,
           NULL as item_id, o.inventory_pool_id, NULL as it_id, false as to_delete
    FROM options o
  ) AS x
  limit 10
  ")

(defn get-models [request]
  (jdbc/execute! (:tx request) [query]))

;; Call the function to get the results
;(get-models)
