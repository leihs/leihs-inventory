(ns leihs.inventory.server.resources.profile.main
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core.core :refer [presence]]
   [leihs.core.remote-navbar.shared :refer [sub-apps]]
   [leihs.core.settings :refer [settings!]]
   [leihs.inventory.server.resources.profile.common :refer [get-by-id]]
   [leihs.inventory.server.resources.profile.languages :as l]
   [leihs.inventory.server.utils.exception-handler :refer [exception-handler]]
   [leihs.inventory.server.utils.helper :refer [convert-to-map snake-case-keys log-by-severity]]
   [leihs.inventory.server.utils.request-utils :refer [query-params]]
   [next.jdbc :as jdbc]
   [ring.util.response :refer [response]]))

(def ERROR_GET_USER "Failed to get user")

(defn get-one [tx target-user-id user-id]
  (get-by-id tx (or user-id target-user-id)))

(defn get-navigation [tx authenticated-entity]
  (let [settings (settings! tx [:external_base_url :documentation_link])
        base-url (:external_base_url settings)
        sub-apps (sub-apps tx authenticated-entity)]
    {:borrow-url (when (:borrow sub-apps) (str base-url "/borrow/"))
     :admin-url (when (:admin sub-apps) (str base-url "/admin/"))
     :procure-url (when (:procure sub-apps) (str base-url "/procure/"))
     :manage-nav-items (map #(assoc % :url (:href %)) (:manage sub-apps))
     :documentation-url (:documentation_link settings)}))

(defn get-pools-access-rights-of-user-query [min-raw user-id access-right-raw]
  (let [min (boolean min-raw)
        access-right (if (and access-right-raw (not (contains? #{"direct_access_rights" "group_access_rights"} access-right-raw)))
                       nil
                       access-right-raw)
        select (if min (sql/select :i.id :i.name) (sql/select :i.is_active :i.name :u.*))
        query (-> select
                  (sql/from [:unified_access_rights :u])
                  (sql/join [:inventory_pools :i] [:= :u.inventory_pool_id :i.id])
                  (sql/where [:= :u.user_id user-id])
                  (cond-> (= access-right "direct_access_rights") (sql/where [:is-not :u.direct_access_right_id nil])
                          (= access-right "group_access_rights") (sql/where [:is-not :u.group_access_right_id nil]))
                  sql-format)]
    query))

(defn get-resource [request]
  (try
    (let [tx (:tx request)
          user-id (or (presence (-> request :path-params :user_id))
                      (:id (:authenticated-entity request)))
          auth (convert-to-map (:authenticated-entity request))
          user-details (get-one tx (:target-user-id request) user-id)
          pools (jdbc/execute! tx (get-pools-access-rights-of-user-query true user-id "direct_access_rights"))]
      (response {:navigation (snake-case-keys (get-navigation tx auth))
                 :available_inventory_pools pools
                 :user_details (snake-case-keys user-details)
                 :languages (snake-case-keys (l/get-multiple tx))}))
    (catch Exception e
      (log-by-severity ERROR_GET_USER e)
      (exception-handler request ERROR_GET_USER e))))

(defn patch-resource [request]
  (try
    (let [tx (:tx request)
          user-id (or (presence (-> request :path-params :user_id))
                      (:id (:authenticated-entity request)))
          data (get-in request [:parameters :body])
          res (jdbc/execute-one!
               tx
               (-> (sql/update :users)
                   (sql/set {:language_locale (:language data)})
                   (sql/where [:= :id user-id])
                   (sql/returning :language_locale)
                   sql-format))]
      (response {:language (:language_locale res)}))
    (catch Exception e
      (log-by-severity ERROR_GET_USER e)
      (exception-handler request ERROR_GET_USER e))))
