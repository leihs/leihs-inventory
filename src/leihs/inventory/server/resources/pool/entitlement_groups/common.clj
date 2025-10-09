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
  (println ">o> abc.delete-entitlements1" )

     (if (seq entitlement-ids) (let [

           p (println ">o> abc.entitlement-ids2" entitlement-ids)

           models (if (seq entitlement-ids)
                    (jdbc/execute! tx (-> (sql/delete-from :entitlements)
                               (sql/where [:in :id entitlement-ids] )
                                        (sql/returning :*)
                               sql-format))
                    [])

              ]
  (println ">o> abc.delete-entitlements.DONE")
       models
)
                               []
                               )


  )


(defn create-entitlements [tx new-models]
  (println ">o> abc.create-entitlements1" )

     (if (seq new-models)
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
  (println ">o> abc.create-entitlements.DONE")
       models
) []
       )

  )


(defn update-entitlements [tx models]
  (println ">o> abc.update-entitlements1" models)
  (if (seq models)
    (let [results
        (mapv
          (fn [model]
            (println ">o> abc.update-entitlements2" model)
            (let [data (select-keys model [:quantity])
                  _ (println ">o> abc.data" data)
                  query (-> (sql/update :entitlements)
                          (sql/set data)
                          (sql/where [:= :id (:id model)])
                          (sql/returning :*)
                          sql-format)
                  result (jdbc/execute! tx query)]
              (println ">o> abc.updated-model" result)
              result))
          models)]
    (println ">o> abc.update-entitlements.DONE" results)
    results)
  )
  []
  )
