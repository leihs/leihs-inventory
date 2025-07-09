(ns leihs.inventory.server.resources.pool.manufacturers.main
  (:require
   [clojure.set]
   [clojure.string :as str]
   [honey.sql :refer [format]
    :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.common :refer [str-to-bool remove-nil-entries-fnc remove-nil-entries  apply-is_deleted-context-if-valid
                                                         apply-is_deleted-where-context-if-valid]]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.helper :refer [convert-map-if-exist url-ends-with-uuid?]]
   [leihs.inventory.server.utils.pagination :refer [create-paginated-response fetch-pagination-params fetch-pagination-params-raw]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [debug error spy]])
  (:import [java.net URL JarURLConnection]
           (java.time LocalDateTime)
           [java.util.jar JarFile]))

(defn extract-manufacturers [data]
  (mapv :manufacturer data))

(defn index-resources [request]
  (try
    (let [tx (:tx request)
          query-params (query-params request)
          mtype (:type query-params)
          search-term (:search-term query-params)
          in-detail (str-to-bool (:in-detail query-params))
          select-stm (if in-detail
                       (sql/select-distinct :m.id :m.manufacturer :m.product :m.version [:m.id :model_id])
                       (sql/select-distinct :m.manufacturer))

          base-query (-> select-stm
                         (sql/from [:models :m])
                         (sql/where [:is-not-null :m.manufacturer])
                         (sql/where [:not-like :m.manufacturer " %"])
                         (sql/where [:not-in :m.manufacturer [""]])
                         (sql/order-by [:m.manufacturer :asc])
                         (cond-> (not (str/blank? search-term))
                           (sql/where [:or [:ilike :m.manufacturer (str "%" search-term "%")]
                                       [:ilike :m.product (str "%" search-term "%")]]))
                         (cond-> (some? mtype)
                           (sql/where [:= :m.type mtype])))

          result (jdbc/execute! tx (-> base-query sql-format))]

      (response (if in-detail result (extract-manufacturers result))))
    (catch Exception e
      (error "Failed to get models/manufacturer" e)
      (bad-request {:error "Failed to get models/manufacture" :details (.getMessage e)}))))
