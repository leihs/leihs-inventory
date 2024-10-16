(ns leihs.inventory.server.resources.items.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.resources.utils.request :refer [path-params]]

   [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
   [leihs.inventory.server.utils.pagination :refer [create-paginated-response fetch-pagination-params]]

   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]]))




(defn remove-select [query]
  (-> query
    (dissoc :select :select-distinct)))

(defn item-query [query item-id]
  (-> query
    remove-select
    (sql/select-distinct [:i.*])
    (sql/join [:items :i] [:= :m.id :i.model_id])
    (cond-> item-id
      (sql/where [:= :i.id item-id]))))

(defn entitlements-query [query entitlement-id]
  (-> query
    remove-select
    (sql/select-distinct [:e.*])
    (sql/join [:entitlements :e] [:= :m.id :e.model_id])
    (cond-> entitlement-id
      (sql/where [:= :e.id entitlement-id]))))

(defn model-links-query [query model-links-id pool_id]
  (-> query
    remove-select
    (sql/select-distinct [:ml.*])
    (cond-> (nil? pool_id) (sql/join [:model_links :ml] [:= :m.id :ml.model_id]))
    (cond-> model-links-id
      (sql/where [:= :ml.id model-links-id]))))

(defn properties-query [query properties-id]
  (-> query
    remove-select
    (sql/select-distinct [:p.*])
    (sql/join [:properties :p] [:= :m.id :p.model_id])
    (cond-> properties-id
      (sql/where [:= :p.id properties-id]))))

(defn accessories-query
  ([query accessories-id]
   (accessories-query query accessories-id "n/d"))
  ([query accessories-id type]
   (-> query
     remove-select
     (sql/select-distinct [:a.*])
     (sql/join [:accessories :a] [:= :m.id :a.model_id])
     (cond-> accessories-id
       (sql/where [:= :a.id accessories-id])))))

(defn attachments-query
  ([query attachment-id]
   (attachments-query query attachment-id "n/d"))
  ([query attachment-id type]
   (-> query
     remove-select
     (sql/select-distinct :a.id :a.content :a.filename :a.item_id)
     (sql/join [:attachments :a] [:= :m.id :a.model_id])
     (cond-> attachment-id
       (sql/where [:= :a.id attachment-id])))))

(defn base-pool-query [query pool-id ]
  (-> query
    (sql/from [:items :i])
      (sql/join [:rooms :r] [:= :r.id :i.room_id])
      (sql/join [:buildings :b] [:= :b.id :r.building_id])
    (cond->
      pool-id (sql/join [:inventory_pools :ip] [:= :ip.id :i.inventory_pool_id])
      pool-id (sql/where [:= :ip.id [:cast pool-id :uuid]]))))

(defn extract-option-by-uri [input-str]
  (let [valid-segments ["properties" "items" "accessories" "attachments" "entitlements" "model-links"]
        last-segment (-> input-str
                       (clojure.string/split #"/")
                       last)]
    (if (some #(= last-segment %) valid-segments)
      last-segment
      nil)))

(defn valid-get-request? [request]
  (let [method (:request-method request)
        uri (:uri request)
        uuid-regex #"([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})$"]
    (and (= method :get)
      (not (re-find uuid-regex uri)))))

(defn- pagination-response [request base-query]
  (let [{:keys [page size]} (fetch-pagination-params request)
        tx (:tx request)]
    (create-paginated-response base-query tx size page)))

(defn get-items-handler
  ([request]
   (get-items-handler request false))
  ([request with-pagination?]
   (let [tx (:tx request)
         {:keys [pool_id model_id item_id properties_id accessories_id attachments_id entitlement_id model_link_id]} (path-params request)
         ;option-type (extract-option-by-uri (:uri request))
         query-params (query-params request)
         {:keys [page size]} (fetch-pagination-params request)
         ;sort-by (case (:sort_by query-params)
         ;          :manufacturer-asc [:m.manufacturer :asc]
         ;          :manufacturer-desc [:m.manufacturer :desc]
         ;          :product-asc [:m.product :asc]
         ;          :product-desc [:m.product :desc]
         ;          nil)

         ;filter-manufacturer (if-not model_id (:filter_manufacturer query-params) nil)
         ;filter-product (if-not model_id (:filter_product query-params) nil)


         ;; TODO: missing
         ;warranty_expiration (present within properties)
         ;current_location
         ;location
         ;type
         ;can_destroy
         ;children

         ;base-query (-> (sql/select-distinct :i.*)
         base-query (-> (sql/select :i.* [:b.name :building_name] [:r.name :room_name])
                      ((fn [query] (base-pool-query query pool_id )))

                      (cond-> item_id (sql/where [:= :i.id item_id]))
                      (cond-> (and sort-by item_id) (sql/order-by item_id)))]
     (cond
       (and (nil? with-pagination?) (valid-get-request? request)) (pagination-response request base-query)
       with-pagination? (pagination-response request base-query)
       :else (jdbc/query tx (-> base-query sql-format))))))

(defn get-items-of-pool-with-pagination-handler [request]
  (response (get-items-handler request true)))

(defn get-items-of-pool-auto-pagination-handler [request]
  (response (get-items-handler request nil)))

(defn get-items-of-pool-handler [request]
  (let [result (get-items-handler request)]
    (response result)))


;; -------------------------------------------------------------------
;PUT
;https://test.leihs.zhdk.ch/manage/8bd16d45-056d-5590-bc7f-12849f034351/items/ce3345b4-0fe0-4cbf-9e19-cf43a470a1da
  ;{
  ; "child_items": [
  ;                 "d1cbf349-c45a-4463-975f-1e5024686780",
  ;                 "9d788d27-0d17-4f12-b5ab-58d85bd35399",
  ;                 "af46ff33-46e0-4d61-b221-b2f7776220be",
  ;                 "5e56da70-927d-4518-8e0d-412a1bba865f",
  ;                 "5f72e850-32c4-4bb9-92f2-06fe76075b85"
  ;                 ],
  ; "inventory_pool_id": "8bd16d45-056d-5590-bc7f-12849f034351",
  ; "item": {
  ;          "skip_serial_number_validation": "true",
  ;          "inventory_code": "P-AUS8700"
  ;          },
  ; "attachments_attributes": {},
  ; "inventory_code": "P-AUS8700",
  ; "inventory_pool": {
  ;                    "id": "8bd16d45-056d-5590-bc7f-12849f034351"
  ;                    },
  ; "is_borrowable": false,
  ; "is_broken": false,
  ; "is_incomplete": true,
  ; "is_inventory_relevant": false,
  ; "last_check": "2023-07-21",
  ; "model_id": "dcbb99f8-eafa-48a4-9579-da06b7c419cc",
  ; "note": "Augenmuschel fehlt, 20.7.17 GC ersetzt, 16.8.17 GC\nLCD bei P-AUS8699\nVF gebrochen. 5.10.18 GC\nRepariert Visuals AUS18283\n1.2.19 GC\nSD Slot ev. defekt? 1 SD Karte defekt, 18.9.20 AB\nSlot A : verklemmte & gebrochene Karte rausoperiert, Schalter (in/out) defekt=ERR C13:01:\nRepariert.GC/Visuals\nMetabone Deckel fehlt, 3.10.17 Ersetzt, 7.2.19 GC\nObjektiv hat Kratzer 19.9.19\n2 Deckel fehlen,\nEin Filter fehlt, 26.10.21, PE. \nSet/Sel Button nicht drückbar, 10.02.22, PE.\nRepariert(ersetzt)9.3.22GC\nBügel bei Mikhalterung fehlt, SEM M.Hendry VV7.3.22\nErsetzt,9.3.22 GC\n",
  ; "owner": {
  ;           "id": "8bd16d45-056d-5590-bc7f-12849f034351"
  ;           },
  ; "price": "10,741.00",
  ; "retired": false,
  ; "room_id": "b3443795-2ce2-498d-8987-21cabb6c5808",
  ; "shelf": "04.A.01",
  ; "skip_serial_number_validation": "true",
  ; "status_note": "2 Deckel fehlen",
  ; "user_name": ""
  ; }









;(defn get-items-of-pool-handler [request]
;  (try
;    (let [tx (:tx request)
;          pool_id (-> request path-params :pool_id)
;          item_id (-> request path-params :id)
;          query (-> (sql/select :i.*)
;                    (sql/from [:items :i])
;                    (sql/where [:= :i.inventory_pool_id pool_id])
;                    (cond-> item_id (sql/where [:= :i.id item_id]))
;                    (sql/limit 10)
;                    sql-format)
;          result (jdbc/query tx query)]
;      (response result))
;    (catch Exception e
;      (error "Failed to get items" e)
;      (bad-request {:error "Failed to get items" :details (.getMessage e)}))))
