(ns leihs.inventory.server.utils.response-helper
  (:require
   [clojure.java.io :as io]
   [clojure.set]
   [leihs.core.anti-csrf.back :refer [anti-csrf-token]]
   [leihs.inventory.server.constants :refer [INVENTORY_VIEW_PATH]]
   [leihs.inventory.server.utils.html-utils :refer [add-csrf-tags]]
   [ring.middleware.accept]
   [ring.util.response :as response]))

;; create spa-response
(defn index-html-response

  ([request]
   (index-html-response request 200))

  ([request status]
   (let [index (io/resource "public/inventory/index.html")
         html (slurp index)
         uuid (anti-csrf-token request)
         params {:authFlow {:returnTo INVENTORY_VIEW_PATH}
                 :csrfToken {:name "csrf-token" :value uuid}}
         html-with-csrf (add-csrf-tags request html params)]
     (-> (response/response html-with-csrf)
         (response/status status)
         (response/content-type "text/html; charset=utf-8")))))
