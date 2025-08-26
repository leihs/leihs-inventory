(ns leihs.inventory.server.resources.pool.options.main
  (:require
   [clojure.set]
   [clojure.string :as str]
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.cast-helper :refer [double-to-numeric-or-nil]]
   [leihs.inventory.server.resources.pool.models.common :refer [filter-map-by-spec filter-and-coerce-by-spec]]
   [leihs.inventory.server.resources.pool.options.types :as ty]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [debug error]]))

(defn post-resource [request]
  (let [tx (:tx request)
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        multipart (get-in request [:parameters :body])
        price (double-to-numeric-or-nil (:price multipart))
        multipart (assoc multipart :price price :inventory_pool_id pool-id)]

    (try
      (let [res (jdbc/execute-one! tx (-> (sql/insert-into :options)
                                          (sql/values [multipart])
                                          (sql/returning :*)
                                          sql-format))]

        (if res
          (response (-> res
                        (filter-map-by-spec ::ty/response-option-object)))
          (bad-request {:error "Failed to create option"})))
      (catch Exception e
        (debug e)
        (error "Failed to create option" (.getMessage e))
        (cond
          (str/includes? (.getMessage e) "case_insensitive_inventory_code_for_options")
          (-> (response {:status "failure"
                         :message "Inventory code already exists"
                         :detail {:product (:product multipart)}})
              (status 409))
          :else (bad-request {:error "Failed to create option" :details (.getMessage e)}))))))

(defn index-resources [request]
  (let [pool-id (get-in request [:path-params :pool_id])]
    (try
      (let [base-query (->
                        (sql/select :o.*)
                        (sql/from [:options :o])
                        (sql/where [:= :o.inventory_pool_id [:cast pool-id :uuid]]))

            post-fnc (fn [models] (filter-and-coerce-by-spec models ::ty/response-option-object))]
        (response (create-pagination-response request base-query nil post-fnc)))
      (catch Exception e
        (debug e)
        (error "Failed to fetch options" (.getMessage e))
        (bad-request {:error "Failed to fetch options" :details (.getMessage e)})))))
