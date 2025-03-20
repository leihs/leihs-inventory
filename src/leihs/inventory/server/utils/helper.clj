(ns leihs.inventory.server.utils.helper
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [honey.sql :refer [format] :rename {format sql-format}]
            [pghstore-clj.core :refer [to-hstore]]
            [taoensso.timbre :refer [warn]])
  (:import (java.util UUID)))

(defn convert-to-map [dict]
  (into {} (map (fn [[k v]] [(clojure.core/keyword k) v]) dict)))

(defn accept-header-html? [request]
  (let [accept-header (get-in request [:headers "accept"])]
    (and accept-header (str/includes? accept-header "text/html"))))

(defn to-uuid
  ([value]
   (try
     (let [result (if (instance? String value) (UUID/fromString value) value)]
       result)
     (catch Exception e
       (warn ">>> DEV-ERROR in to-uuid[value], value=" value ", exception=" (.getMessage e))
       value)))

  ([value key]
   (def keys-to-cast-to-uuid #{:user_id :id :group_id :person_id :collection_id :media_entry_id :accepted_usage_terms_id :delegation_id
                               :uploader_id :created_by_id
                               :keyword_id})
   (let [res (try
               (if (and (contains? keys-to-cast-to-uuid (keyword key)) (instance? String value))
                 (UUID/fromString value)
                 value)
               (catch Exception e
                 (warn ">>> DEV-ERROR in to-uuid[value key], value=" value ", key=" key " exception=" (.getMessage e))
                 value))] res))

  ([value key table]
   (def blacklisted-tables #{"meta_keys" "vocabularies"})

   ;; XXX: To fix db-exceptions of io_interfaces
   (if (or (contains? blacklisted-tables (name table)) (and (= table :io_interfaces) (= key :id)))
     value
     (to-uuid value key))))

;; TODO: maybe possible with json/dump?
(defn convert-to-raw-set [urls]
  (let [transformed-urls urls
        combined-str (str "'{" (clojure.string/join "," transformed-urls) "}'")]
    [:raw combined-str]))

(defn modify-if-exists [m k f]
  (if (contains? m k)
    (update m k f)
    m))

;; Used for columns of jsonb type
(defn convert-map-if-exist [m]
  (-> m
      (modify-if-exists :is_borrowable #(if (contains? m :is_borrowable) [:cast % ::boolean]))
      (modify-if-exists :is_inventory_relevant #(if (contains? m :is_inventory_relevant) [:cast % ::boolean]))
      (modify-if-exists :is_broken #(if (contains? m :is_broken) [:cast % ::boolean]))
      (modify-if-exists :is_incomplete #(if (contains? m :is_incomplete) [:cast % ::boolean]))

      (modify-if-exists :deleted_at #(if (contains? m :deleted_at) [:cast % ::date]))
      (modify-if-exists :retired #(if (contains? m :retired) [:cast % ::date]))
      (modify-if-exists :last_check #(if (contains? m :last_check) [:cast % ::date]))
      (modify-if-exists :layout #(if (contains? m :layout) [:cast % :public.collection_layout]))
      (modify-if-exists :default_resource_type #(if (contains? m :default_resource_type) [:cast % :public.collection_default_resource_type]))
      (modify-if-exists :sorting #(if (contains? m :sorting) [:cast % :public.collection_sorting]))
      (modify-if-exists :json #(if (contains? m :json) [:cast (json/generate-string %) :jsonb]))

      ;; uuid
      (modify-if-exists :id #(if (contains? m :id) (to-uuid % :id)))
      (modify-if-exists :media_entry_default_license_id #(if (contains? m :id) (to-uuid %)))
      (modify-if-exists :edit_meta_data_power_users_group_id #(if (contains? m :edit_meta_data_power_users_group_id) (to-uuid %)))
      (modify-if-exists :creator_id #(if (contains? m :creator_id) (to-uuid %)))
      (modify-if-exists :person_id #(if (contains? m :person_id) (to-uuid %)))
      (modify-if-exists :user_id #(if (contains? m :user_id) (to-uuid %)))
      (modify-if-exists :accepted_usage_terms_id #(if (contains? m :accepted_usage_terms_id) (to-uuid %)))

      (modify-if-exists :room_id #(if (contains? m :room_id) (to-uuid %)))
      (modify-if-exists :model_id #(if (contains? m :model_id) (to-uuid %)))
      (modify-if-exists :owner_id #(if (contains? m :owner_id) (to-uuid %)))
      (modify-if-exists :created_by_id #(if (contains? m :created_by_id) (to-uuid %)))
      (modify-if-exists :uploader_id #(if (contains? m :uploader_id) (to-uuid %)))
      (modify-if-exists :media_entry_id #(if (contains? m :media_entry_id) (to-uuid %)))

      ;; jsonb / character varying
      (modify-if-exists :settings #(if (nil? %) [:raw "'{}'"] (convert-to-raw-set %)))
      (modify-if-exists :external_uris #(if (nil? %) [:raw "'{}'"] (convert-to-raw-set %)))
      (modify-if-exists :sitemap #(if (nil? %) [:raw "'{}'"] (convert-to-raw-set %)))
      (modify-if-exists :available_locales #(if (nil? %) [:raw "'{}'"] (convert-to-raw-set %)))

      ;; text[]
      (modify-if-exists :contexts_for_entry_extra #(if (nil? %) [:raw "'[]'"] (convert-to-raw-set %)))
      (modify-if-exists :contexts_for_list_details #(if (nil? %) [:raw "'[]'"] (convert-to-raw-set %)))
      (modify-if-exists :contexts_for_entry_validation #(if (nil? %) [:raw "'[]'"] (convert-to-raw-set %)))
      (modify-if-exists :contexts_for_dynamic_filters #(if (nil? %) [:raw "'[]'"] (convert-to-raw-set %)))
      (modify-if-exists :contexts_for_collection_edit #(if (nil? %) [:raw "'[]'"] (convert-to-raw-set %)))
      (modify-if-exists :contexts_for_collection_extra #(if (nil? %) [:raw "'[]'"] (convert-to-raw-set %)))
      (modify-if-exists :contexts_for_entry_edit #(if (nil? %) [:raw "'[]'"] (convert-to-raw-set %)))
      (modify-if-exists :contexts_for_context_keys #(if (nil? %) [:raw "'[]'"] (convert-to-raw-set %)))
      (modify-if-exists :catalog_context_keys #(if (nil? %) [:raw "'[]'"] (convert-to-raw-set %)))
      (modify-if-exists :copyright_notice_templates #(if (nil? %) [:raw "'[]'"] (convert-to-raw-set %)))
      (modify-if-exists :allowed_people_subtypes #(if (nil? %) [:raw "'[]'"] (convert-to-raw-set %)))))
