;(ns leihs.inventory.server.resources.pool.user.profile
;  (:require
;   [clojure.set]
;   [leihs.core.remote-navbar.shared :refer [sub-apps]]
;   [leihs.core.settings :refer [settings!]]
;   [leihs.inventory.server.resources.pool.user.common :refer [get-by-id]]
;   [ring.middleware.accept]))
;
;(defn get-one [tx target-user-id user-id]
;  (get-by-id tx (or user-id target-user-id)))
;
;(defn get-current [tx authenticated-entity]
;  (let [user-id (:id authenticated-entity)
;        session-id (:user_session_id authenticated-entity)]
;    {:id user-id
;     :user (get-by-id tx user-id)
;     :session-id session-id}))
;
;(defn get-navigation [tx authenticated-entity]
;  (let [settings (settings! tx [:external_base_url :documentation_link])
;        base-url (:external_base_url settings)
;        sub-apps (sub-apps tx authenticated-entity)]
;    {:borrow-url (when (:borrow sub-apps) (str base-url "/borrow/"))
;     :admin-url (when (:admin sub-apps) (str base-url "/admin/"))
;     :procure-url (when (:procure sub-apps) (str base-url "/procure/"))
;     :manage-nav-items (map #(assoc % :url (:href %)) (:manage sub-apps))
;     :documentation-url (:documentation_link settings)}))
;
;(defn get-settings [tx user-id]
;  (let [settings (settings! tx [:lending_terms_acceptance_required_for_order
;                                :lending_terms_url
;                                :show_contact_details_on_customer_order
;                                :timeout_minutes])]
;    {:lending-terms-acceptance-required-for-order (:lending_terms_acceptance_required_for_order settings)
;     :lending-terms-url (:lending_terms_url settings)
;     :show-contact-details-on-customer-order (:show_contact_details_on_customer_order settings)
;     :timeout-minutes (:timeout_minutes settings)}))
