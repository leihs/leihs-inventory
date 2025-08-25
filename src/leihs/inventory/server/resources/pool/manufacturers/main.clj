(ns leihs.inventory.server.resources.pool.manufacturers.main
  (:require
   [clojure.set]
   [clojure.string :as str]
   [honey.sql :refer [format]
    :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.common :refer [str-to-bool]]
   [leihs.inventory.server.utils.request-utils :refer [query-params]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response]]
   [taoensso.timbre :refer [debug error]]))

(defn extract-manufacturers [data]
  (mapv :manufacturer data))

(defn index-resources [request]
  (try
    (let [tx (:tx request)
          query-params (query-params request)
          mtype (:type query-params)
          search-term (:search query-params)
          in-detail (str-to-bool (:in-detail query-params))
          select-stm (if in-detail
                       (sql/select-distinct :m.id :m.manufacturer :m.product :m.version [:m.id :model_id])
                       (sql/select-distinct :m.manufacturer))

          base-query (-> select-stm
                         (sql/from [:models :m])
                         (sql/where [:is-not-null :m.manufacturer])
                         (sql/where [:not-like :m.manufacturer " %"])
                         (sql/where [:not-in :m.manufacturer [""]])
                         (cond-> (some? mtype)
                           (sql/where [:= :m.type mtype]))
                         (cond-> (not (str/blank? search-term))
                           (sql/where [:ilike :m.manufacturer (str "%" search-term "%")]))
                         (sql/order-by [:m.manufacturer :asc]))
          result (jdbc/execute! tx (-> base-query sql-format))]

      (response (if in-detail result (extract-manufacturers result))))
    (catch Exception e
      (debug e)
      (error "Failed to get models/manufacturer" e)
      (bad-request {:error "Failed to get models/manufacture" :details (.getMessage e)}))))
