(ns leihs.inventory.server.resources.main
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [honey.sql :refer [format] :as sq :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.core.anti-csrf.back :as anti-csrf :refer [anti-csrf-token]]
   [leihs.core.constants :as constants]
   [leihs.core.sign-in.back :as be]
   [leihs.core.sign-in.simple-login :refer [sign-in-view]]
   [leihs.core.sign-out.back :as so]
   [leihs.inventory.server.constants :refer [INVENTORY_VIEW_PATH]]
   [leihs.inventory.server.resources.profile.main :refer [get-pools-access-rights-of-user-query]]
   [leihs.inventory.server.utils.helper :refer [convert-to-map]]
   [leihs.inventory.server.utils.html-utils :refer [add-csrf-tags]]
   [next.jdbc :as jdbc]
   [pandect.core]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.util.response :refer [redirect response status]])
  (:gen-class)
  (:import
   (org.jsoup Jsoup)))

(defn swagger-api-docs-handler [request]
  (let [path (:uri request)]
    (cond
      (= path "/inventory/api-docs") (redirect "/inventory/api-docs/index.html")
      (= path "/inventory/index.html") (redirect INVENTORY_VIEW_PATH)
      :else (status (response "File not found") 404))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn convert-params [request]
  (if-let [form-params (:form-params request)]
    (let [converted-form-params (into {} (map (fn [[k v]] [(keyword k) v]) form-params))]
      (assoc request :form-params converted-form-params :form-params-raw converted-form-params))
    request))

(defn- extract-csrf-token [^String html]
  (let [doc (Jsoup/parse (or html ""))
        meta (.select doc "meta[name=csrf-token]")
        input (.select doc "input[name=csrf-token]")
        token (cond
                (pos? (.size meta)) (.attr (.first meta) "content")
                (pos? (.size input)) (.attr (.first input) "value")
                :else nil)]
    (some-> token str/trim not-empty)))

(defn- fetch-sign-in-view [request]
  (let [mtoken (anti-csrf-token request)
        query (convert-to-map (:query-params request))
        params (-> {:authFlow {:returnTo (or (:return-to query) INVENTORY_VIEW_PATH)}
                    :flashMessages []}
                   (assoc :csrfToken {:name "csrf-token" :value mtoken})
                   (cond-> (:message query)
                     (assoc :flashMessages [{:level "error" :messageID (:message query)}])))
        html (add-csrf-tags request (sign-in-view params) params)]
    html))

(defn get-sign-in [request]
  (let [html (fetch-sign-in-view request)]
    {:status 200 :headers {"Content-Type" "text/html; charset=utf-8"} :body html}))

(defn get-csrf-token [request]
  (let [html (fetch-sign-in-view request)
        res (extract-csrf-token html)]
    {:status 200 :body {:csrf-token res}}))

(defn post-sign-in [req]
  (let [{:keys [user password]} (:form-params req)]
    (if (or (str/blank? user) (str/blank? password))
      (be/create-error-response user req)
      (let [resp (-> req convert-params be/routes)
            created-session (or (get-in resp [:cookies :leihs-user-session :value])
                                (get-in resp [:cookies "leihs-user-session" :value]))]
        (if (nil? created-session)
          resp
          (let [tx (:tx req)
                token-hash (pandect.core/sha256 created-session)
                {:keys [user_id]} (jdbc/execute-one! tx
                                                     (-> (sql/select :*)
                                                         (sql/from :user_sessions)
                                                         (sql/where [:= :token_hash token-hash])
                                                         sql-format))
                pools (jdbc/execute! tx
                                     (get-pools-access-rights-of-user-query
                                      true user_id "direct_access_rights"))
                return-to (if (empty? pools)
                            INVENTORY_VIEW_PATH
                            (->> (first pools)
                                 :id
                                 str
                                 (format "/inventory/%s/list")))]

            (assoc-in resp [:headers "Location"] return-to)))))))

(defn get-sign-out [request]
  (let [uuid (get-in request [:cookies constants/ANTI_CSRF_TOKEN_COOKIE_NAME :value])
        params {:authFlow {:returnTo INVENTORY_VIEW_PATH}
                :csrfToken {:name "csrf-token" :value uuid}}
        html (add-csrf-tags request (slurp (io/resource "public/dev-logout.html")) params)]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body html}))

(defn post-sign-out [request]
  (let [params (-> request
                   convert-params
                   (assoc-in [:accept :mime] :html))
        accept (get-in params [:headers "accept"])]
    (if (str/includes? accept "application/json")
      {:status (if (so/routes params) 200 409)}
      (so/routes params))))
