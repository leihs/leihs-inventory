(ns leihs.inventory.server.utils.response
  "HTTP response utilities including HTML, JSON, and error responses."
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.walk :as walk]
   [hickory.core :as h]
   [hickory.render :as render]
   [leihs.core.anti-csrf.back :refer [anti-csrf-token]]
   [leihs.inventory.server.constants :refer [INVENTORY_VIEW_PATH]]
   [leihs.inventory.server.middlewares.exception-handler :refer [exception-handler]]
   [ring.util.response :as ring-response]))

;; --- CSRF tag helpers ---

(defn- add-meta-tag [tree csrf-name csrf-value]
  (walk/postwalk
   (fn [node]
     (if (and (map? node) (= (:tag node) :head))
       (update node :content conj {:type :element
                                   :tag :meta
                                   :attrs {:name csrf-name :content csrf-value}})
       node))
   tree))

(defn- update-csrf-input [tree csrf-value]
  (walk/postwalk
   (fn [node]
     (if (and (map? node)
              (= (:tag node) :input)
              (= (get-in node [:attrs :name]) "csrfToken")
              (= (get-in node [:attrs :type]) "hidden"))
       (assoc-in node [:attrs :value] csrf-value)
       node))
   tree))

(defn- add-form-if-missing [tree csrf-name csrf-value]
  (walk/postwalk
   (fn [node]
     (if (and (map? node) (= (:tag node) :body))
       (if (empty? (filter #(= (:tag %) :form) (:content node)))
         (update node :content conj {:type :element
                                     :tag :form
                                     :attrs {:name csrf-name}
                                     :content [{:type :element
                                                :tag :input
                                                :attrs {:type "hidden" :name csrf-name :value csrf-value}}]})
         node)
       node))
   tree))

(defn add-csrf-tags
  [request html-str {:keys [csrfToken]}]
  (try
    (let [parsed-html (h/parse html-str)
          hickory-tree (h/as-hickory parsed-html)
          csrf-name (:name csrfToken)
          csrf-value (:value csrfToken)
          updated-tree (as-> hickory-tree $
                         (add-meta-tag $ csrf-name csrf-value)
                         (update-csrf-input $ csrf-value)
                         (add-form-if-missing $ csrf-name csrf-value))
          raw-html (render/hickory-to-html updated-tree)]
      raw-html)

    (catch Exception e
      (exception-handler request "Error in add-csrf-and-return-tags" e)
      (.printStackTrace e)
      html-str)))

;; --- HTML response ---

(defn index-html-response [request status]
  (let [index (io/resource "public/inventory/index.html")
        html (slurp index)
        uuid (anti-csrf-token request)
        params {:authFlow {:returnTo INVENTORY_VIEW_PATH}
                :csrfToken {:name "csrf-token" :value uuid}}
        html-with-csrf (add-csrf-tags request html params)]
    (-> (ring-response/response html-with-csrf)
        (ring-response/status status)
        (ring-response/content-type "text/html; charset=utf-8"))))

;; --- Not found handler ---

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
      (index-html-response request 200)

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
