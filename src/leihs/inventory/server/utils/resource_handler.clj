(ns leihs.inventory.server.utils.resource-handler
  (:require
   [cheshire.core :as json]
   [clojure.string :as str]
   [leihs.inventory.server.utils.response-helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]))

(def supported-accepts
  "Whitelist of Accept header values requiring explicit support.
   Used for 406 validation. Attachments use */* so not listed.
   image/ prefix matches all image types."
  #{"text/html"
    "application/json"
    "image/"
    "text/csv"
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"})

(defn custom-not-found-handler [request]
  (let [accept (str/lower-case (or (get-in request [:headers "accept"]) "*/*"))
        uri (:uri request)
        is-html? (or (str/includes? accept "text/html")
                     (str/includes? accept "*/*"))
        is-image? (str/includes? accept "image/")
        is-inventory? (str/includes? uri "/inventory")
        authenticated? (-> request :authenticated-entity boolean)
        supported? (or (= accept "*/*")
                       (some #(str/includes? accept %) supported-accepts))]
    (cond
      ;; Unsupported Accept header → 406
      (not supported?)
      {:status 406
       :headers {"content-type" "text/plain"}
       :body "Not Acceptable"}

      ;; HTML + inventory → SPA/200 (client-side routing)
      (and is-html? is-inventory?)
      (rh/index-html-response request 200)

      ;; Non-HTML inventory request without auth → 401
      (and is-inventory? (not authenticated?) (not is-html?))
      {:status 401
       :headers {"content-type" "application/json"}
       :body (json/generate-string {:status "failure" :message "Not authenticated"})}

      ;; Image → 404 text/plain (generic message)
      is-image?
      {:status 404
       :headers {"content-type" "text/plain"}
       :body "Not Found"}

      ;; All other formats (JSON, CSV, Excel, etc.) → 404 JSON
      :else
      {:status 404
       :headers {"content-type" "application/json"}
       :body (json/generate-string {:error "Not Found"})})))
