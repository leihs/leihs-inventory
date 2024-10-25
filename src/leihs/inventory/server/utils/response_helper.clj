(ns leihs.inventory.server.utils.response_helper
  (:require
   [clojure.java.io :as io]
   [clojure.set]

   [leihs.core.constants :as constants]
   [leihs.inventory.server.utils.html-utils :refer [add-csrf-tags]]

   [leihs.core.anti-csrf.back :refer [anti-csrf-token anti-csrf-props]]

   [leihs.core.core :refer [keyword str presence]]
   [ring.util.request :as request]
   [ring.util.response :as response]

   [ring.middleware.accept])
(:import [java.net URL JarURLConnection]
 [java.util.jar JarFile]
 (java.util UUID)
 ))

(defn index-html-response [status]
  (let [index (io/resource "public/index.html")
        default (io/resource "public/index-fallback.html")]
    {:status status
     :headers {"Content-Type" "text/html"}
     :body (slurp (if (nil? index) default index))}))



;(defn index-html-response [request status]
;  (let [index (io/resource "public/index.html")
;        default (io/resource "public/index-fallback.html")
;
;        html (slurp(if (nil? index) default index))
;
;
;        ;p (println ">o> abc1" html)
;
;
;        mtoken ( anti-csrf-token request )
;        p (println ">o> (html) anti-csrf-token" mtoken)
;
;        mprops( anti-csrf-props request)
;        p (println ">o> (html) anti-csrf-props" mprops)
;
;
;        _ (println ">o> !!!!!1 (html) fetch.anti-csrf-token1" (:anti-csrf-token request))
;        _ (println ">o> !!!!!2 (html) fetch.anti-csrf-token2" (-> request
;                                                       :cookies
;                                                       (get constants/ANTI_CSRF_TOKEN_COOKIE_NAME nil)
;                                                       :value
;                                                       presence))
;
;
;
;
;        ;uuid (if (:anti-csrf-token request)
;        ;       presence
;        ;       (let [
;        ;         uuid (str (UUID/randomUUID))
;        ;             ]
;        ;
;        ;         )
;        ;       )
;
;        ;uuid (str (UUID/randomUUID)) ;; Generate UUID for CSRF token
;
;        uuid mtoken
;
;        params {
;                :authFlow {:returnTo "/inventory/models"}
;                ;:csrfToken {:name "csrfToken"
;                :csrfToken {:name "csrf-token"
;                            :value uuid}} ;; Parameters including CSRF token
;        ;html (sign-in-view params)  ;; Generate the original HTML using the params
;
;        ;; Debugging the original HTML
;        ;_ (println ">o> html.before" html (type html))
;
;        ;; Add CSRF tokens to the HTML and debug the result
;        html-with-csrf (add-csrf-tags html params)
;        ;_ (println ">o> html.after" html-with-csrf (type html-with-csrf))
;
;        max-age 3600
;
;
;
;        ]
;
;    (->
;      (response/response        html-with-csrf)
;      (response/status        status)
;      (response/content-type "text/html; charset=utf-8")
;      ;(response/set-cookie "leihs-anti-csrf-token" uuid {:max-age max-age :path "/"})
;      )
;
;
;
;    ))

(defn index-html-response [request status]
  (let [index (io/resource "public/index.html")
        default (io/resource "public/index-fallback.html")

        ;; Load HTML content, use fallback if index.html is not found
        html (slurp (or index default))

        ;; Generate CSRF token and set parameters for the HTML
        uuid (anti-csrf-token request)
        params {:authFlow {:returnTo "/inventory/models"}
                :csrfToken {:name "csrf-token" :value uuid}}

        ;; Add CSRF tokens to the HTML
        html-with-csrf (add-csrf-tags html params)]

    ;; Return the modified HTML in the response
    (-> (response/response html-with-csrf)
      (response/status status)
      (response/content-type "text/html; charset=utf-8"))))


;(def ^:export INDEX-HTML-RESPONSE-OK (index-html-response 200))
;(def ^:export INDEX-HTML-RESPONSE-NOT-FOUND (index-html-response 404))
