(ns leihs.inventory.server.resources.pool.items.main
  (:require
   [cheshire.core :as json]
   [clojure.string :as str]
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.pool.fields.main :as fields]
   [leihs.inventory.server.resources.pool.inventory-code :as inv-code]
   [leihs.inventory.server.resources.pool.items.fields-shared :refer [coerce-field-values
                                                                      in-coercions
                                                                      out-coercions
                                                                      flatten-properties
                                                                      split-item-data
                                                                      validate-field-permissions]]
   [leihs.inventory.server.resources.pool.items.filter-handler :refer [build-where-clause
                                                                       extract-id-type
                                                                       get-fields-response
                                                                       create-filter-query-and-validate!
                                                                       normalize-pipes
                                                                       parse-filter]]
   [leihs.inventory.server.resources.pool.items.shared :as items-shared]
   [leihs.inventory.server.resources.pool.items.types :as types]
   [leihs.inventory.server.resources.pool.models.common :refer [fetch-thumbnails-for-ids]]
   [leihs.inventory.server.utils.debug :refer [log-by-severity]]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response]]
   [leihs.inventory.server.utils.pick-fields :refer [pick-fields]]
   [leihs.inventory.server.utils.request-utils :refer [body-params path-params
                                                       query-params]]
   [leihs.inventory.server.utils.request-utils :refer [path-params query-params]]
   [next.jdbc :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [debug]])
  (:import
   [java.sql Timestamp]
   [java.time LocalDate]
   [java.time LocalDate OffsetDateTime ZonedDateTime ZoneId]
   [java.time.format DateTimeFormatter]
   [java.util UUID]))

(def ERROR_GET_ITEMS "Failed to get items")

(def base-query
  (-> (->> types/columns
           (map #(keyword "items" (name %)))
           concat
           (apply sql/select))
      (sql/from :items)
      (sql/order-by :items.inventory_code)))

(defn index-resources
  ([request]
   (let [tx (:tx request)
         {:keys [pool_id]} (path-params request)
         {:keys [fields search_term
                 model_id parent_id
                 retired borrowable
                 incomplete broken owned
                 inventory_pool_id filter_q
                 in_stock before_last_check]} (query-params request)

         extra-columns [[:ip.name :inventory_pool_name]
                        [:r.end_date :reservation_end_date]
                        [:r.user_id :reservation_user_id]
                        [:r.contract_id :reservation_contract_id]
                        [:m.is_package :is_package]
                        [:m.name :model_name]
                        [:rs.name :room_name]
                        [:rs.description :room_description]
                        [:b.name :building_name]
                        [:b.code :building_code]

                        [[:nullif [:concat_ws " " :u.firstname :u.lastname] ""] :reservation_user_name]

                        [(-> (sql/select :%count.*) ; [[:count :*]]
                             (sql/from [:items :i])
                             (sql/where [:= :i.parent_id :items.id]))
                         :package_items]]

         query (-> base-query
                   (#(apply sql/select % extra-columns))

                   ;; Join inventory pool
                   (sql/join [:inventory_pools :ip]
                             [:= :ip.id :items.inventory_pool_id])

                   ;; Join rooms
                   (sql/join [:rooms :rs]
                             [:= :items.room_id :rs.id])

                   ;; Join rooms
                   (sql/join [:buildings :b]
                             [:= :rs.building_id :b.id])

                   ;; Join models
                   (sql/left-join [:models :m]
                                  [:or
                                   [:= :items.model_id :m.id]
                                   [:= :m.id model_id]])

                   ;; Join reservations (only active)
                   (sql/left-join [:reservations :r]
                                  [:and
                                   [:= :r.item_id :items.id]
                                   [:= :r.returned_date nil]])

                    ;; Join users
                   (sql/left-join [:users :u]
                                  [:= :u.id :r.user_id])

                    ;; Filters
                   (sql/where [:or
                               [:= :items.inventory_pool_id pool_id]
                               [:= :items.owner_id pool_id]])

                   (cond-> model_id (sql/where [:= :items.model_id model_id]))
                   (cond-> parent_id (sql/where [:= :items.parent_id parent_id]))

                   (cond-> filter_q
                     (create-filter-query-and-validate! request pool_id filter_q))

                   (items-shared/item-query-params pool_id inventory_pool_id
                                                   owned in_stock before_last_check
                                                   retired borrowable broken incomplete)

                   (cond-> (seq search_term)
                     (sql/where [:or
                                 [:ilike :items.inventory_code (str "%" search_term "%")]
                                 [:ilike :m.product (str "%" search_term "%")]
                                 [:ilike :m.manufacturer (str "%" search_term "%")]])))

         post-fnc (fn [items]
                    ;; Prepare items for thumbnail fetching by using model_id as id
                    (let [items-for-fetch (map (fn [item]
                                                 (assoc item :id (:model_id item)))
                                               items)
                          ;; Fetch thumbnails using model data
                          items-with-images (fetch-thumbnails-for-ids tx items-for-fetch)]
                      ;; Merge back with original items and add image URLs
                      (map-indexed (fn [idx item-with-img]
                                     (let [original-item (nth items idx)]
                                       (cond-> original-item
                                         (:image_id item-with-img)
                                         (assoc :image_id (:image_id item-with-img)
                                                :url (str "/inventory/" pool_id "/models/" (:model_id original-item) "/images/" (:image_id item-with-img))
                                                :content_type (:content_type item-with-img)))))
                                   items-with-images)))]

     (debug (sql-format query :inline true))
     (try
       (-> request
           (create-pagination-response query nil post-fnc)
           (pick-fields fields types/index-item)
           response)
       (catch Exception e
         (log-by-severity ERROR_GET_ITEMS e)
         (exception-handler request ERROR_GET_ITEMS e))))))

(def ERROR_CREATE_ITEM "Failed to create item")

(defn inventory-code-exists? [tx inventory-code exclude-id]
  (let [query (-> (sql/select :id)
                  (sql/from :items)
                  (sql/where [:= :inventory_code inventory-code])
                  (cond-> exclude-id
                    (sql/where [:not= :id exclude-id]))
                  sql-format)]
    (-> (jdbc/execute-one! tx query)
        some?)))

(defn post-resource [request]
  (try
    (let [tx (:tx request)
          {:keys [pool_id]} (path-params request)
          body-params (body-params request)
          validation-error (validate-field-permissions request)]
      (if validation-error
        (bad-request validation-error)
        (let [{:keys [item-data properties]} (split-item-data body-params)
              inventory-code (:inventory_code item-data)]
          (if (inventory-code-exists? tx inventory-code nil)
            (status {:body {:error "Inventory code already exists"
                            :proposed_code (inv-code/propose tx pool_id)}}
                    409)
            (let [item-data-coerced (coerce-field-values item-data in-coercions)
                  properties-json (or (not-empty properties) {})
                  item-data-with-properties (assoc item-data-coerced
                                                   :properties [:lift properties-json])
                  sql-query (-> (sql/insert-into :items)
                                (sql/values [item-data-with-properties])
                                (sql/returning :*)
                                sql-format)
                  result (jdbc/execute-one! tx sql-query)]
              (if result
                (response (-> result
                              flatten-properties
                              (coerce-field-values out-coercions)))
                (bad-request {:error ERROR_CREATE_ITEM})))))))
    (catch Exception e
      (log-by-severity ERROR_CREATE_ITEM e)
      (exception-handler request ERROR_CREATE_ITEM e))))

(def ERROR_UPDATE_ITEM "Failed to update item")

(defn patch-resource [{:keys [tx] :as request}]
  (try
    (if-let [validation-error (validate-field-permissions request)]
      (bad-request validation-error)
      (let [{:keys [ids data]} (body-params request)
            ids (set ids)
            [item-fields prop-fields]
            (reduce-kv
             (fn [[norm props] k v]
               (let [kname (name k)]
                 (if (str/starts-with? kname "properties_")
                   [norm (assoc props (subs kname (count "properties_")) v)]
                   [(assoc norm k v) props])))
             [{} {}]
             data)
            set-map (cond-> item-fields
                      (seq prop-fields)
                      (assoc :properties
                             [:||
                              :properties
                              [:cast (json/generate-string prop-fields) :jsonb]]))
            query (-> (sql/update :items)
                      (sql/set set-map)
                      (sql/where [:in :id ids])
                      (sql/returning :*)
                      sql-format)
            results (jdbc/execute! tx query)]
        (if (seq results)
          (response (map flatten-properties results))
          (bad-request {:error ERROR_UPDATE_ITEM}))))
    (catch Exception e
      (log-by-severity ERROR_UPDATE_ITEM e)
      (exception-handler request ERROR_UPDATE_ITEM e))))
