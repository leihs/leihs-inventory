(ns leihs.inventory.server.utils.ressource-handler
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string :as clojure.string]
   [clojure.string :as str]
   [leihs.inventory.server.utils.request-utils :refer [authenticated?]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.util.response :refer [response status content-type]]))

(defn parse-cookie [request]
  (let [cookie-str (get-in request [:headers "cookie"])]
    (if (or (nil? cookie-str) (clojure.string/blank? cookie-str))
      {}
      (->> (clojure.string/split cookie-str #"; ")
           (map #(clojure.string/split % #"=" 2))
           (reduce
            (fn [m [k v]]
              (if (and k v)
                (assoc m k v)
                m))
            {})))))

(defn session-valid? [request]
  (let [session (parse-cookie request)
        is-authenticated? (authenticated? request)
        p (println ">o> abc.is-authenticated?" is-authenticated?)]
    (and is-authenticated?
         (get session "leihs-user-session"))))

(defn pr [str fnc]
  ;(println ">oo> HELPER / " str fnc)(println ">oo> HELPER / " str fnc)
  (println ">oo> " str fnc)
  fnc)

(defn custom-not-found-handler [request]

  (rh/index-html-response request 404)

  ;(let [accept-header (or (get-in request [:headers "accept"]) "")
  ;
  ;      uri (:uri request)
  ;      ]
  ;  (cond (clojure.string/includes? accept-header "application/json")
  ;    (pr "f2" (-> {:status "failure" :message "No entry found"}
  ;      (json/generate-string)
  ;      (response)
  ;      (status 404)
  ;      (content-type "application/json; charset=utf-8")))
  ; ;[leihs.inventory.server.utils.response-helper :as rh]
  ;
  ;    ;(and (str/includes? accept-header "text/html") (pr "session-invalid?" (not (session-valid? request))))
  ;    ;{:status 302 :headers {"Location" "/sign-in?return-to=%2Finventory" "Content-Type" "text/html"} :body ""}
  ;
  ;    ; (or (= uri "/inventory/api-docs") (= uri "/inventory/api-docs/"))
  ;    ;{:status 302 :headers {"Location" "/inventory/api-docs/index.html"} :body ""}
  ;    ;
  ;    ;(or (= uri "/inventory/swagger-ui") (= uri "/inventory/swagger-ui/"))
  ;    ;{:status 302 :headers {"Location" "/inventory/swagger-ui/index.html"} :body ""}
  ;
  ;
  ;    ;(and (nil? asset) (accept-header-html? request))
  ;    ;(rh/index-html-response request 200)
  ;
  ;    ;:else (pr "f1" (rh/index-html-response request 404)))))
  ;    :else (rh/index-html-response request 404)))
  )
