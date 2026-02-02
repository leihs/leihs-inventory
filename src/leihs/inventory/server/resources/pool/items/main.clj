(ns leihs.inventory.server.resources.pool.items.main
  (:require
   [better-cond.core :refer [cond] :rename {cond bcond}]
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.constants :refer [ACCEPT-CSV ACCEPT-EXCEL]]
   [leihs.inventory.server.middlewares.debug :refer [log-by-severity]]
   [leihs.inventory.server.middlewares.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.resources.pool.inventory-code :as inv-code]
   [leihs.inventory.server.resources.pool.inventory-pools.main :as pools]
   [leihs.inventory.server.resources.pool.items.export :as items-export]
   [leihs.inventory.server.resources.pool.items.fields-shared :refer [coerce-field-values
                                                                      flatten-properties
                                                                      in-coercions
                                                                      out-coercions
                                                                      split-item-data
                                                                      validate-field-permissions]]
   [leihs.inventory.server.resources.pool.items.filter-handler :refer [create-filter-query-and-validate!]]
   [leihs.inventory.server.resources.pool.items.shared :as items-shared]
   [leihs.inventory.server.resources.pool.items.types :as types]
   [leihs.inventory.server.resources.pool.list.search :refer [with-search]]
   [leihs.inventory.server.resources.pool.models.common :refer [fetch-thumbnails-for-ids]]
   [leihs.inventory.server.utils.export :as export]
   [leihs.inventory.server.utils.pagination :refer [create-pagination-response]]
   [leihs.inventory.server.utils.pick-fields :refer [pick-fields]]
   [leihs.inventory.server.utils.request :refer [body-params path-params
                                                 query-params]]
   [next.jdbc :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [debug]]))

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
         {:keys [fields search search_term
                 model_id parent_id
                 retired borrowable
                 incomplete broken owned
                 inventory_pool_id filter_q
                 in_stock before_last_check]} (query-params request) ; query params of reitit
         search_term (or search search_term) ; search_term needed for fields
         ; getting ids from query params of ring. it handles both single and multiple ids.
         ids-raw (or (get (:query-params request) "ids[]")
                     (get (:query-params request) "ids"))
         ids (when ids-raw
               (->> (if (string? ids-raw) [ids-raw] ids-raw)
                    (map #(java.util.UUID/fromString %))))
         accept-header (get-in request [:headers "accept"])

         extra-columns [[:ip.name :inventory_pool_name]
                        [:r.end_date :reservation_end_date]
                        [:r.user_id :reservation_user_id]
                        [:r.contract_id :reservation_contract_id]
                        [:models.is_package :is_package]
                        [:models.name :model_name]
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
                   (sql/left-join :models
                                  [:or
                                   [:= :items.model_id :models.id]
                                   [:= :models.id model_id]])

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
                   (cond-> (seq ids) (sql/where [:in :items.id ids]))

                   ;; Advanced filter support (filter_q: URL-encoded EDN, MQL-style)
                   (cond-> (and filter_q (seq (str filter_q)))
                     (create-filter-query-and-validate! request filter_q {:rooms "rs"}))

                   ; in legacy no query params are passed down to the children,
                   ; speaking: all children are always showed.
                   (cond-> (not parent_id)
                     (-> (items-shared/item-query-params pool_id
                                                         :inventory_pool_id inventory_pool_id
                                                         :owned owned
                                                         :in_stock in_stock
                                                         :before_last_check before_last_check
                                                         :retired retired
                                                         :borrowable borrowable
                                                         :broken broken
                                                         :incomplete incomplete)
                         (cond-> (seq search_term) (with-search search_term :models)))))

         post-fnc (fn [items]
                    (let [items-for-fetch (mapv (fn [item]
                                                  (assoc item :id (:model_id item)))
                                                items)
                          items-with-images (fetch-thumbnails-for-ids tx items-for-fetch)]
                      (vec (map-indexed (fn [idx item-with-img]
                                          (let [original-item (nth items idx)
                                                img-id (:image_id item-with-img)]
                                            (cond-> original-item
                                              img-id
                                              (assoc :image_id (str img-id)
                                                     :url (str "/inventory/" pool_id "/models/" (:model_id original-item) "/images/" img-id)
                                                     :content_type (:content_type item-with-img)))))
                                        items-with-images))))]

     (debug (sql-format query :inline true))
     (try
       (cond
         (and accept-header (re-find (re-pattern ACCEPT-CSV) accept-header))
         (let [data (-> query
                        (#(items-export/sql-prepare tx % pool_id))
                        sql-format
                        (->> (export/jdbc-execute! tx)))]
           (export/csv-response data :filename "items.csv"))

         (and accept-header (re-find (re-pattern ACCEPT-EXCEL) accept-header))
         (let [array-data (-> query
                              (#(items-export/sql-prepare tx % pool_id))
                              sql-format
                              (->> (export/jdbc-execute! tx)))
               [header & _] array-data
               data (export/arrays-to-maps array-data)]
           (export/excel-response data
                                  :keys (map keyword header)
                                  :filename "items.xlsx"))

         :else
         (-> request
             (create-pagination-response query nil post-fnc)
             (pick-fields fields types/index-item)
             response))
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

(defn generate-inventory-codes [tx pool-id n]
  (let [starting-code (inv-code/propose tx pool-id)
        starting-number (inv-code/extract-last-number starting-code)
        shortname (:shortname (pools/get-by-id tx pool-id))]
    (map #(str shortname (+ starting-number %)) (range n))))

(defn create-item [tx item-data-coerced properties-json]
  (let [item-data-with-properties (assoc item-data-coerced
                                         :properties [:lift properties-json])
        sql-query (-> (sql/insert-into :items)
                      (sql/values [item-data-with-properties])
                      (sql/returning :*)
                      sql-format)
        result (jdbc/execute-one! tx sql-query)]
    (when-not result
      (throw (ex-info ERROR_CREATE_ITEM {:item-data item-data-coerced})))
    (-> result
        flatten-properties
        (coerce-field-values out-coercions))))

(defn post-resource [request]
  (try
    (let [tx (:tx request)
          {:keys [pool_id]} (path-params request)
          body-params (body-params request)
          {:keys [inventory_code count]} body-params
          validation-error (validate-field-permissions request)]
      (bcond
       validation-error
       (bad-request validation-error)

       (and inventory_code count (> count 1))
       (bad-request {:error "Cannot provide both inventory_code and count when count > 1"})

       (and (not inventory_code) (not count))
       (bad-request {:error "Must provide either inventory_code or count"})

       :let [{:keys [item-data properties]} (split-item-data
                                             (dissoc body-params :count))
             item-data-coerced (coerce-field-values item-data in-coercions)
             properties-json (or (not-empty properties) {})]

       (and count (> count 1))
       (let [codes (generate-inventory-codes tx pool_id count)
             created-items (doall (map #(create-item tx
                                                     (assoc item-data-coerced :inventory_code %)
                                                     properties-json)
                                       codes))]
         (response created-items))

       (inventory-code-exists? tx inventory_code nil)
       (status {:body {:error "Inventory code already exists"
                       :proposed_code (inv-code/propose tx pool_id)}}
               409)

       (response (create-item tx item-data-coerced properties-json))))
    (catch Exception e
      (log-by-severity ERROR_CREATE_ITEM e)
      (exception-handler request ERROR_CREATE_ITEM e))))
