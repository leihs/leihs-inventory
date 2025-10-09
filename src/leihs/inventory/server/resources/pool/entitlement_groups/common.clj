(ns leihs.inventory.server.resources.pool.entitlement-groups.common
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.helper :refer [log-by-severity]]
   [leihs.inventory.server.utils.request-utils :refer [path-params body-params]]
   ;[next.jdbc.sql :as jdbc]
   [next.jdbc :as jdbc]
   ;[java.util UUID]
   [ring.middleware.accept]
   [taoensso.timbre :refer [debug error]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response]]
   [ring.util.response :refer [response ]])

  ;)

(:import
 [java.util UUID]))




(defn delete-entitlements [tx entitlement-ids]

     (let [

           p (println ">o> abc.entitlement-ids" entitlement-ids)

           models (if (seq entitlement-ids)
                    (jdbc/execute! tx (-> (sql/delete-from :entitlements)
                               (sql/where [:in :id entitlement-ids] )
                               sql-format))
                    [])

              ]
       models
))


(defn create-entitlements [tx new-models]

     (let [

              p (println ">o> abc.new-models" new-models)

              query (-> (sql/insert-into :entitlements)
                      (sql/values new-models)
                      (sql/returning :*)
                      sql-format
                      )
              models (jdbc/execute! tx query)
              p (println ">o> abc.new-models2" models)


              ]
       models
))

(defn update-entitlements [tx models]
  (println ">o> abc.models" models)
  (let [results        (for [model models]

          p (println ">o> abc" model)

          ;(let [query (-> (sql/update :entitlements)
          ;              (sql/set model)
          ;              (sql/returning :*)
          ;              sql-format)
          ;      result (jdbc/execute! tx query)]
          ;  (println ">o> abc.updated-model" result)
          ;  result)

          )]
    (do
      (println ">o> abc.new-models2" results)
      results)))

