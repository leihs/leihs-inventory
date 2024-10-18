(ns leihs.inventory.server.utils.response_helper
  (:require
   [clojure.java.io :as io]
   [clojure.set]

   [leihs.inventory.server.utils.html-utils :refer [add-csrf-tags]]

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



(defn index-html-response [status]
  (let [index (io/resource "public/index.html")
        default (io/resource "public/index-fallback.html")

        html (slurp(if (nil? index) default index))


        p (println ">o> abc1" html)



        uuid (str (UUID/randomUUID)) ;; Generate UUID for CSRF token
        params {
                :authFlow {:returnTo "/inventory/models"}
                :csrfToken {:name "csrfToken"
                            :value uuid}} ;; Parameters including CSRF token
        ;html (sign-in-view params)  ;; Generate the original HTML using the params

        ;; Debugging the original HTML
        _ (println ">o> html.before" html (type html))

        ;; Add CSRF tokens to the HTML and debug the result
        html-with-csrf (add-csrf-tags html params)
        _ (println ">o> html.after" html-with-csrf (type html-with-csrf))



        ]
    ;{:status status
    ; :headers {"Content-Type" "text/html"}
    ; :body (slurp (if (nil? index) default index))}

    {:status status
     :headers {"Content-Type" "text/html; charset=utf-8"}
     :body html-with-csrf}


    ))






(def ^:export INDEX-HTML-RESPONSE-OK (index-html-response 200))
(def ^:export INDEX-HTML-RESPONSE-NOT-FOUND (index-html-response 404))
