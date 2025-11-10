(ns leihs.inventory.server.resources.pool.list.export-csv
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


(defn convert "Handler that generates an CSV file from a given map."
  [data]
  (let [   output-stream (java.io.ByteArrayOutputStream.)
        csv-data (maps-to-csv data)]
    (with-open [writer (io/writer output-stream)]
      (csv/write-csv writer csv-data))
    {:status 200
     :headers {"Content-Type" "text/csv"
               "x-rows" (str (count data))
               "Content-Disposition" "attachment; filename=output.csv"}
     :body (java.io.ByteArrayInputStream. (.toByteArray output-stream))}))
