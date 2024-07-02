(ns leihs.inventory.backend.routes
  (:refer-clojure :exclude [keyword replace])
  (:require
   [leihs.core.anti-csrf.back :as anti-csrf]
   [leihs.core.auth.session :as session]
   [leihs.core.db :as datasource]
   [leihs.core.http-cache-buster2 :as cache-buster :refer [wrap-resource]]
   [leihs.core.locale :as locale]
   [leihs.core.ring-audits :as ring-audits]
   [leihs.core.ring-exception :as ring-exception]
   [leihs.core.routes :as core-routes]
   [leihs.core.routing.back :as core-routing]
   [leihs.core.settings :as settings]
   [leihs.core.status :as status]
   [leihs.inventory.backend.html :as html]
   [leihs.inventory.backend.paths :refer [paths path]]
   [leihs.inventory.backend.resources.models.main]
   [logbug.debug :as debug :refer [I>]]
   [logbug.ring :refer [wrap-handler-with-logging]]
   ring.middleware.accept
   [ring.middleware.content-type :refer [wrap-content-type]]
   [ring.middleware.cookies :refer [wrap-cookies]]
   [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
   [ring.middleware.multipart-params :refer [wrap-multipart-params]]
   [ring.middleware.params :refer [wrap-params]]
   [taoensso.timbre :as log]))

(def resolve-table
  (merge core-routes/resolve-table
         {:home html/html-handler,
          :api-models-index leihs.inventory.backend.resources.models.main/routes
          :not-found html/not-found-handler}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn dispatch-to-handler
  [request]
  (if-let [handler (:handler request)]
    (handler request)
    (throw
     (ex-info
      "There is no handler for this resource and the accepted content type."
      {:status 404, :uri (get request :uri)}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn wrap-accept
  [handler]
  (ring.middleware.accept/wrap-accept
   handler
   {:mime ["application/json" :qs 1 :as :json
           "application/javascript" :qs 1 :as :javascript
           "image/apng" :qs 1 :as :apng
           "image/*" :qs 1 :as :image
           "text/css" :qs 1 :as :css
           "text/html" :qs 1 :as :html]}))

(defn wrap-empty [handler]
  (fn [request]
    (or (handler request)
        {:status 404})))

(defn init [options]
  (core-routing/init paths resolve-table)
  (->
  ; (I> wrap-handler-with-logging
   dispatch-to-handler
   ring-audits/wrap
   anti-csrf/wrap
   locale/wrap
   session/wrap-authenticate
   wrap-cookies
   settings/wrap
   datasource/wrap-tx
   wrap-json-response
   (wrap-json-body {:keywords? true})
   wrap-empty
   core-routing/wrap-canonicalize-params-maps
   wrap-params
   wrap-multipart-params
   (status/wrap (path :status))
   wrap-content-type
   (wrap-resource "public"
                  {:allow-symlinks? true
                   :cache-bust-paths ["/inventory/ui/styles.css"
                                      "/inventory/js/main.js"]
                   :never-expire-paths [#".*fontawesome-[^\/]*\d+\.\d+\.\d+\/.*"
                                        #".+_[0-9a-f]{40}\..+"]
                   :enabled? true})
   (core-routing/wrap-resolve-handler html/html-handler)
   wrap-accept
   ring-exception/wrap))

;#### debug ###################################################################
; (debug/debug-ns 'cider-ci.utils.shutdown)
; (debug/debug-ns *ns*)
