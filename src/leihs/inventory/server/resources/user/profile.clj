(ns leihs.inventory.server.resources.user.profile
  (:require
   [clojure.set]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core.core :refer [presence raise]]
   [leihs.core.remote-navbar.shared :refer [sub-apps]]
   [leihs.core.settings :refer [settings!]]
   [leihs.inventory.server.resources.user.common :refer [get-by-id]]
   [leihs.inventory.server.resources.utils.request :refer [path-params]]
   [leihs.inventory.server.utils.helper :refer [convert-to-map]]
   [next.jdbc.sql :as jdbc]
   [ring.middleware.accept]
   [ring.util.response :refer [bad-request response status]]
   [taoensso.timbre :refer [error]]))

(defn get-one [tx target-user-id user-id]
  (get-by-id tx (or user-id target-user-id)))

(defn get-current [tx authenticated-entity]
  (let [user-id (:id authenticated-entity)
        session-id (:user_session_id authenticated-entity)]
    {:id user-id
     :user (get-by-id tx user-id)
     :session-id session-id}))

(defn get-navigation [tx authenticated-entity]
  (let [settings (settings! tx [:external_base_url :documentation_link])
        base-url (:external_base_url settings)
        sub-apps (sub-apps tx authenticated-entity)]
    {:admin-url (when (:admin sub-apps) (str base-url "/admin/"))
     :procure-url (when (:procure sub-apps) (str base-url "/procure/"))
     :manage-nav-items (map #(assoc % :url (:href %)) (:manage sub-apps))
     :documentation-url (:documentation_link settings)}))

(defn get-settings [tx user-id]
  (let [settings (settings! tx [:lending_terms_acceptance_required_for_order
                                :lending_terms_url
                                :show_contact_details_on_customer_order
                                :timeout_minutes])]
    {:lending-terms-acceptance-required-for-order (:lending_terms_acceptance_required_for_order settings)
     :lending-terms-url (:lending_terms_url settings)
     :show-contact-details-on-customer-order (:show_contact_details_on_customer_order settings)
     :timeout-minutes (:timeout_minutes settings)}))
