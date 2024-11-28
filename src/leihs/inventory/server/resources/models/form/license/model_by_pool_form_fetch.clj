(ns leihs.inventory.server.resources.models.form.license.model-by-pool-form-fetch
  (:require
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [honey.sql :as sq :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]

   [leihs.inventory.server.resources.models.helper :refer [fetch-latest-inventory-code]]

      [leihs.inventory.server.resources.models.queries :refer [accessories-query attachments-query
                                                            entitlements-query item-query
                                                            model-links-query properties-query]]
   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.converter :refer [to-uuid]]
   [leihs.inventory.server.utils.core :refer [single-entity-get-request?]]
   [leihs.inventory.server.utils.helper :refer [convert-map-if-exist]]
   [leihs.inventory.server.utils.pagination :refer [fetch-pagination-params pagination-response create-pagination-response]]
   [next.jdbc :as jdbc]

   [next.jdbc.sql :as jdbco]


   [ring.util.response :refer [bad-request response]]
   [taoensso.timbre :refer [error]])

   (:import [java.time LocalDateTime]
    [java.time.format DateTimeFormatter]
           [java.util UUID]))




(defn select-entries [tx table columns where-clause]
  (jdbc/execute! tx
                 (-> (apply sql/select columns)
                     (sql/from table)
                     (sql/where where-clause)
                     sql-format)))

(defn fetch-attachments [tx model-id]
  (select-entries tx :attachments [:id :filename :content_type] [:= :model_id model-id]))

(defn fetch-images [tx model-id]
  (let [query (-> (sql/select :m.cover_image_id :i.id :i.filename :i.content_type)
                  (sql/from [:models :m])
                  (sql/right-join [:images :i] [:= :i.target_id :m.id])
                  (sql/where [:and [:= :m.id model-id] [:= :i.thumbnail false]])
                  sql-format)
        images (jdbc/execute! tx query)]
    (map (fn [row]
           (assoc row :url (str "/inventory/images/" (:id row))
                  :thumbnail-url (str "/inventory/images/" (:id row) "/thumbnail")))
         images)))

(defn fetch-accessories [tx model-id]
  (let [query (-> (sql/select :a.id :a.name [:aip.inventory_pool_id :has_inventory_pool]
                              [(sq/call :not= :aip.inventory_pool_id nil) :has_inventory_pool])
                  (sql/from [:accessories :a])
                  (sql/left-join [:accessories_inventory_pools :aip] [:= :a.id :aip.accessory_id])
                  (sql/where [:= :a.model_id model-id])
                  (sql/order-by :a.name)
                  sql-format)]
    (jdbc/execute! tx query)))

(defn fetch-compatibles [tx model-id]
  (let [query (-> (sql/select :mm.id :mm.product)
                  (sql/from [:models_compatibles :mc])
                  (sql/left-join [:models :m] [:= :mc.model_id :m.id])
                  (sql/left-join [:models :mm] [:= :mc.compatible_id :mm.id])
                  (sql/where [:= :mc.model_id model-id])
                  sql-format)]
    (jdbc/execute! tx query)))

(defn fetch-properties [tx model-id]
  (select-entries tx :properties [:id :key :value] [:= :model_id model-id]))

(defn fetch-entitlements [tx model-id]
  (let [query (-> (sql/select :e.id :e.quantity :e.position :eg.name [:eg.id :group_id])
                  (sql/from [:entitlements :e])
                  (sql/join [:entitlement_groups :eg] [:= :e.entitlement_group_id :eg.id])
                  (sql/where [:= :e.model_id model-id])
                  sql-format)]
    (jdbc/execute! tx query)))

(defn fetch-categories [tx model-id]
  (let [category-type "Category"
        query (-> (sql/select :mg.id :mg.type :mg.name)
                  (sql/from [:model_groups :mg])
                  (sql/left-join [:model_links :ml] [:= :mg.id :ml.model_group_id])
                  (sql/where [:ilike :mg.type (str category-type)])
                  (sql/where [:= :ml.model_id model-id])
                  (sql/order-by :mg.name)
                  sql-format)]
    (jdbc/execute! tx query)))


;; TODO: Replace this with a real function
(defn random-5-digit []
  (+ 10000 (rand-int 90000)))

(defn get-current-timestamp []
  (let [current-timestamp (LocalDateTime/now)
        formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss")]
    (.format current-timestamp formatter)))

(defn build-select [fields]
  (let [columns (mapv (fn [field]
                        (keyword (str "i." (:id field)))) ; Add namespace "i" to each id
                  fields)]
    (println ">o> build-select columns:" columns) ; Debug output
    columns)) ; Return the columns as a flat vector

(defn build-select2 [fields]
  (let [columns (mapv (fn [field]
                        (:id field)) ; Add namespace "i" to each id
                        ;(keyword (str "i." (:id field)))) ; Add namespace "i" to each id
                  fields)]
    (println ">o> build-select columns:" columns) ; Debug output
    columns)) ; Return the columns as a flat vector



(defn filter-by-allowed-keys [data allowed-keys whitelisted-keys]
  (let [allowed-keywords (set (map keyword allowed-keys)) ; Convert allowed-keys to a set of keywords
        base-level-filter (fn [m keys]
                            (reduce-kv
                              (fn [acc k v]
                                (if (contains? keys k) ; keys is a set here
                                  (assoc acc k v)
                                  acc))
                              {}
                              m))
        deep-filter (fn [props-path props]
                      (let [filtered-props (base-level-filter props
                                             (->> allowed-keys
                                               (filter #(str/starts-with? % (str props-path ".")))
                                               (map #(keyword (last (str/split % #"\."))))
                                               set))] ; Ensure keys is a set
                        (when (seq filtered-props)
                          filtered-props)))]
    (reduce-kv
      (fn [result k v]
        (cond
          ;; Handle nested maps for :properties
          (and (= k :properties) (map? v))
          (let [filtered-props (deep-filter "properties" v)]
            (if filtered-props
              (assoc result k filtered-props)
              result))

          ;; Include key if it exists in the allowed keys
          (contains? allowed-keywords k)
          (assoc result k v)

          ;; Otherwise exclude the key
          :else
          result))
      {}
      data)))


;(ns my-app.core
;  (:require [clojure.string :as str]))

(defn filter-by-allowed-keys [data allowed-keys whitelisted-keys]
  (let [allowed-keywords (set (map keyword allowed-keys))          ; Convert allowed keys to keywords
        whitelisted-keywords (set (map keyword whitelisted-keys))  ; Convert whitelisted keys to keywords
        all-keys (clojure.set/union allowed-keywords whitelisted-keywords) ; Union of allowed and whitelisted keys
        base-level-filter (fn [m keys]
                            (reduce-kv
                              (fn [acc k v]
                                (if (contains? keys k)
                                  (assoc acc k v)
                                  acc))
                              {}
                              m))
        deep-filter (fn [props-path props]
                      (let [filtered-props (base-level-filter props
                                             (->> allowed-keys
                                               (filter #(str/starts-with? % (str props-path ".")))
                                               (map #(keyword (last (str/split % #"\."))))
                                               set))] ; Ensure filtered keys are a set
                        (when (seq filtered-props)
                          filtered-props)))]
    (reduce-kv
      (fn [result k v]
        (cond
          ;; Handle nested maps for :properties
          (and (= k :properties) (map? v))
          (let [filtered-props (deep-filter "properties" v)]
            (if filtered-props
              (assoc result k filtered-props)
              result))

          ;; Include key if it exists in all-keys (allowed or whitelisted)
          (contains? all-keys k)
          (assoc result k v)

          ;; Otherwise exclude the key
          :else
          result))
      {}
      data)))


(defn filter-by-allowed-keys [data allowed-keys whitelisted-keys blacklisted-keys]
  (let [allowed-keywords (set (map keyword allowed-keys))          ; Convert allowed keys to keywords
        whitelisted-keywords (set (map keyword whitelisted-keys))  ; Convert whitelisted keys to keywords
        all-keys (clojure.set/union allowed-keywords whitelisted-keywords)] ; Union of allowed and whitelisted keys
    (reduce-kv
      (fn [result k v]
        ;; Include key if it exists in all-keys (allowed or whitelisted)
        (if (contains? all-keys k)
          (assoc result k v)
          result))
      {}
      data)))


(defn filter-by-allowed-keys
  [data allowed-keys whitelisted-keys blacklisted-keys]
  (let [allowed-keywords (set (map keyword allowed-keys))          ; Convert allowed keys to keywords
        whitelisted-keywords (set (map keyword whitelisted-keys))  ; Convert whitelisted keys to keywords
        blacklisted-keywords (set (map keyword blacklisted-keys))  ; Convert blacklisted keys to keywords
        all-keys (clojure.set/difference
                   (clojure.set/union allowed-keywords whitelisted-keywords) ; Union of allowed and whitelisted keys
                   blacklisted-keywords)] ; Remove blacklisted keys
    (reduce-kv
      (fn [result k v]
        ;; Include key if it exists in all-keys (allowed or whitelisted but not blacklisted)
        (if (contains? all-keys k)
          (assoc result k v)
          result))
      {}
      data)))


(defn rename-keys
  "Renames keys in a map based on a provided key mapping.
   `key-map` is a map where the keys are old keys and the values are new keys."
  [m key-map]
  (reduce
    (fn [acc [old-key new-key]]
      (if (contains? m old-key)
        (assoc acc new-key (get m old-key))
        acc))
    (apply dissoc m (keys key-map)) ;; Start with the original map minus old keys
    key-map))

(defn filter-entries
  "Filters a collection of maps, keeping only the specified keys in each map."
  [maps keys-to-keep]
  (map #(select-keys % keys-to-keep) maps))

(defn create-license-handler-by-pool-form-fetch [request]
  (let [
        ;current-timestamp (LocalDateTime/now)

        current-timestamp (get-current-timestamp)

        tx (get-in request [:tx])
        model-id (to-uuid (get-in request [:path-params :model_id]))
        pool-id (to-uuid (get-in request [:path-params :pool_id]))
        pool_id pool-id




        p (println ">o> params => " pool-id model-id)
        ]
    (try
      (let [
            ;; TODO: make a union of
            ;;   - fields without target_type/permissions
            ;;   - fields especially for lending/inventory-manager
            query (-> (sql/select :f.id
                        :f.active
                        :f.position
                        :f.data
                        :f.dynamic
                        [(sq/call :cast
                           (sq/call :jsonb_extract_path_text :f.data "label")
                           :text) :label]

                        [(sq/call :cast
                           (sq/call :jsonb_extract_path_text :f.data "permissions" "owner")
                           :text) :owner]

                        [(sq/call :cast
                           (sq/call :jsonb_extract_path_text :f.data "permissions" "role")
                           :text) :role]

                        [(sq/call :cast
                           (sq/call :jsonb_extract_path_text :f.data "group")
                           :text) :group]

                        [(sq/call :cast
                           (sq/call :jsonb_extract_path_text :f.data "target_type")
                           :text) :target_type]


                        )
                    (sql/from [:fields :f])
                    (sql/where [:= :f.active true])
                    (sql/where [:or
                                [:in (sq/call :jsonb_extract_path_text :f.data "group")
                                 ["Status" "Invoice Information" "General Information" "Inventory" "Maintenance"]]
                                [:is (sq/call :jsonb_extract_path_text :f.data "group") nil]])


                    (sql/where [:or [:ilike (sq/call :jsonb_extract_path_text :f.data "target_type") "%license%"]
                                [:is (sq/call :jsonb_extract_path_text :f.data "target_type") nil]])


                    ;;; TODO: fetch all license-fields
                    ;(sql/where [:and
                    ;            [:or [:ilike (sq/call :jsonb_extract_path_text :f.data "target_type") "%license%"]
                    ;             [:is-null (sq/call :jsonb_extract_path_text :f.data "target_type")]]
                    ;            [:or
                    ;             ;; TODO:
                    ;             ;[:in (sq/call :jsonb_extract_path_text :f.data "permissions" "role") ["inventory_manager"]]
                    ;             [:in (sq/call :jsonb_extract_path_text :f.data "permissions" "role") ["lending_manager"]]
                    ;             [:is-null (sq/call :jsonb_extract_path_text :f.data "permissions" "role")]
                    ;
                    ;             ; whitelist (needed for role: lending-manager)
                    ;             [:in :f.id ["inventory_code"]]
                    ;             ]
                    ;            ]



                    (sql/where [:not-in :f.id ["properties_project_number"]])

                    (sql/order-by [(sq/call :jsonb_extract_path_text :f.data "group") :asc]
                      [:f.position :asc])
                    sql-format
                    )

            ;p (println ">o> query" query)

            fields-result (jdbc/execute! tx query)


            filtered (filter-entries fields-result [:group  :label :role ])

            p (println ">o> filtered >> " filtered)
            ;p (println ">o> fields >> " fields-result)

            fields fields-result

            ;; -----------------------

            dyn-select (build-select2 fields)
            p (println ">o> dyn-dyn-select-??" dyn-select)


            model-result (if model-id
                           (let [
                                 model-query (-> (sql/select :m.id :m.product :m.manufacturer :m.version :m.type
                                                   :m.hand_over_note :m.description :m.internal_description
                                                   ;:m.technical_detail :m.is_package)
                                                   :m.technical_detail :m.is_package :i.* [:s.id :supplier_id][:s.name :supplier_name])
                                               ;dyn-select
                                               (sql/from [:models :m])

                                               (sql/join [:items :i] [:= :m.id :i.model_id])
                                               (sql/join [:suppliers :s] [:= :i.supplier_id :s.id])
                                               (sql/where [:= :m.id model-id])
                                               sql-format)
                                 model-result (jdbc/execute-one! tx model-query)



                                 model-result (assoc model-result :product {
                                                                            :name (:product model-result)
                                                                            :model_id (:id model-result)
                                                                            })

                                 model-result (assoc model-result :supplier {
                                                                            :name (:supplier_name model-result)
                                                                            :supplier_id (:supplier_id model-result)
                                                                            })

                                 model-result (rename-keys model-result {:item_version :version})


                                 ]
                             model-result

                                        )


                           (let [;; create default-values for new license
                                 ;; FIXME:
                                 responsible_department "b582d569-05c1-5d60-aeb8-b67a10bb2957"


                                 ;model-query (-> (sql/select :ip.*)
                                 ;               (sql/from [:inventory_pools :ip])
                                 ;               (sql/where [:= :ip.id pool_id])
                                 ;               sql-format)
                                 ;model-result (jdbc/execute-one! tx model-query)
                                 ;
                                 ;;p (println ">o> model-result" model-result)

                                 p (println ">o> res1" pool-id)

                                 {:keys [next-code]} (fetch-latest-inventory-code tx pool-id)
                                 p (println ">o> next-code" next-code)


                                 ;inventory_code (str (:shortname model-result) (random-5-digit) )
                                 ]

                           {:inventory_pool_id pool-id
                            ;:inventory_code inventory_code
                            :responsible_department responsible_department
                            :inventory_code next-code
                            ;:properties {:maintenance_currency "CHF"}
                            }
                           )
                             )


            p (println ">o> model-result" model-result (count model-result))

            model-result (filter-by-allowed-keys model-result dyn-select ["properties"
                                                                          "inventory_code" "inventory_pool_id"  "responsible_department" ;; init values

                                                                         "product" "license_version"

                                                                          "supplier" ;"supplier_id"
                                                                          "version"
                                                                          ]
                           ["supplier_name" "supplier_id"])
            ;model-result (filter-by-allowed-keys model-result dyn-select [])
            ;model-result (filter-by-allowed-keys model-result dyn-select ["properties" "properties_license_type" "license_type" "total_quantity"])
            p (println ">o> model-result2" (count model-result))



          ;model-result (rename-keys model-result {:item_version :version})


            result (if model-result
                     {
                      :data model-result
                      :fields fields
                      }
                     {}
                     )]
        (if result
          (response result)
          (bad-request {:error "Failed to fetch license"})))
      (catch Exception e
        (error "Failed to fetch license" (.getMessage e))
        (bad-request {:error "Failed to fetch license" :details (.getMessage e)})))))
