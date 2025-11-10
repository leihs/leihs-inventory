(ns leihs.inventory.server.resources.pool.list.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [leihs.core.core :refer [presence]]
   [leihs.inventory.server.resources.pool.models.common :refer [fetch-thumbnails-for-ids
                                                                model->enrich-with-image-attr]]
   [leihs.inventory.server.resources.pool.models.queries :refer [base-inventory-query
                                                                 filter-by-type
                                                                 from-category
                                                                 with-items
                                                                 with-search
                                                                 without-items]]
[leihs.inventory.server.resources.pool.export.csv.main :refer [maps-to-csv]]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [clojure.java.io :as io]
   [clojure.set]
   [dk.ative.docjure.spreadsheet :as ss]

   [leihs.inventory.server.utils.debug :refer [log-by-severity]]


   [clojure.data.csv :as csv]
   [clojure.java.io :as io]
   [clojure.set]
   [clojure.string :as str]
   [ring.middleware.accept]

   [leihs.inventory.server.resources.pool.items.main :as helper]
   [leihs.inventory.server.resources.pool.items.filter-handler :as filter]
   [leihs.inventory.server.resources.pool.fields.main :refer [fetch-properties-fields]]

   [leihs.inventory.server.utils.pagination :refer [create-pagination-response]]
   [leihs.inventory.server.utils.request-utils :refer [path-params
                                                       query-params]]
   [ring.util.response :refer [response]]
   [taoensso.timbre :refer [debug]]))

(defn items-sub-query [query ]
  (-> query
    (sql/select :items.properties)
    (sql/right-join :items [:= :items.model_id :inventory.id])  ) )


(defn to-csv "Handler that generates an CSV file from a given map."
  [data]
  (let [
        ;data [{:name "Alice" :age 30}
        ;      {:name "Bob" :age 25}]
        output-stream (java.io.ByteArrayOutputStream.)
        csv-data (maps-to-csv data)]
    (with-open [writer (io/writer output-stream)]
      (csv/write-csv writer csv-data))
    {:status 200
     :headers {"Content-Type" "text/csv"
               "Content-Disposition" "attachment; filename=output.csv"}
     :body (java.io.ByteArrayInputStream. (.toByteArray output-stream))}))



(defn generate-excel-from-map
  "Generates an Excel file from a map and returns a Java File object."
  [data-map]
  (try
    (when (empty? data-map)
      (throw (IllegalArgumentException. "Data map cannot be empty")))
    (let [workbook (ss/create-workbook "Sheet1" [(map name (keys (first data-map)))])
          sheet (ss/select-sheet "Sheet1" workbook)]
      (doseq [row data-map]
        (let [row-values (map #(if (or (string? %) (number? %)) % (str %)) (vals row))]
          (ss/add-row! sheet row-values)))
      (let [temp-file (doto (java.io.File/createTempFile "export" ".xlsx") (.deleteOnExit))]
        (with-open [output-stream (io/output-stream temp-file)]
          (ss/save-workbook! output-stream workbook))
        temp-file))
    (catch Exception e
      (log-by-severity "Failed to generate Excel from map" e)
      (throw e))))

(defn to-excel
  "Handler that generates an Excel file from a given map."
  [data]
  (try
    (let [
          ;data [{:name "Alice" :age 30 :city "New York"}
          ;      {:name "Bob" :age 25 :city "San Francisco"}
          ;      {:name "Charlie" :age 35 :city "Boston"}]
          excel-file (generate-excel-from-map data)]
      {:status 200
       :headers {"Content-Type" "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                 "Content-Disposition" "attachment; filename=export.xlsx"}
       :body (io/input-stream excel-file)})
    (catch IllegalArgumentException e
      (log-by-severity "Invalid input to Excel handler" e)
      {:status 400
       :body "Invalid input to generate Excel file."})
    (catch Exception e
      (log-by-severity "Internal Server Error in Excel handler" e)
      {:status 500
       :body "Internal Server Error."})))


(defn index-resources
  ([request]
   (let [tx (:tx request)
         {pool-id :pool_id} (path-params request)
         {:keys [with_items type
                 retired borrowable incomplete broken
                 inventory_pool_id owned in_stock
                 category_id
                 search before_last_check filters]} (query-params request)



         accept-type (-> request :accept :mime)
         p (println ">o> abc.accept-type2" accept-type)


         p (println ">o> abc.before, filters" filters)
         parsed-filters (filter/parse-json-param filters)
         p (println ">o> abc.after, filters" parsed-filters)

         properties-fields (fetch-properties-fields request)
         ;p (println ">o> abc.properties-fields.type??" (type properties-fields))
         p (println ">o> abc.properties-fields.count" (first properties-fields))

         {:keys [filter-keys properties raw-filter-keys]} (helper/extract-ids properties-fields "properties_")
         WHITELIST-ITEM-FILTER filter-keys
         p (println ">o> abc.WHITELIST-ITEM-FILTER2" WHITELIST-ITEM-FILTER)

         parsed-filters (helper/prepare-filters parsed-filters)
         p (println ">o> abc.parsed-filters" parsed-filters)
         validation-result (filter/validate-filters parsed-filters WHITELIST-ITEM-FILTER)

         query (-> (base-inventory-query pool-id)
                   (cond-> type (filter-by-type type))
                   (cond->
                    (not= type :option)
                     (cond->
                      (and pool-id (true? with_items))
                       (with-items pool-id
                         (cond-> {:retired retired
                                  :borrowable borrowable
                                  :incomplete incomplete
                                  :broken broken
                                  :inventory_pool_id inventory_pool_id
                                  :owned owned
                                  :in_stock in_stock
                                  }
                           (not= type :software)
                           (assoc :before_last_check before_last_check)))
                       (and pool-id (false? with_items))
                       (without-items pool-id)))

                 (cond-> (seq parsed-filters)
                   (-> items-sub-query
                   (filter/add-filter-groups parsed-filters raw-filter-keys)))

                   (cond-> (and pool-id (presence search))
                     (with-search search))
                   (cond-> (and category_id (not (some #{type} [:option :software])))
                     (#(from-category tx % category_id))))

         post-fnc (fn [models]
                    (->> models
                         (fetch-thumbnails-for-ids tx)
                         (map (model->enrich-with-image-attr pool-id))))]
     (debug (sql-format query :inline true))
     ;(response (create-pagination-response request query nil post-fnc))


     (cond
       ;accept-type
       ;(response (create-pagination-response request query nil post-fnc))
       ;(response (to-csv (create-pagination-response request query false post-fnc)))

       (= accept-type :csv) (to-csv (create-pagination-response request query false post-fnc))
       (= accept-type :excel) (to-excel (create-pagination-response request query false post-fnc))
        :else (response (create-pagination-response request query nil post-fnc))
       )


     )))


