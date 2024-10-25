(ns leihs.inventory.server.resources.utils.session
  (:require
   [leihs.inventory.server.resources.utils.request :refer [AUTHENTICATED_ENTITY authenticated? get-auth-entity]]
   [clojure.string :as clojure.string]))

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
    p (println ">o> is-authenticated? => " is-authenticated?)
    p (println ">o> is-authenticated? => " (:authenticated-entity request))
    ]

    (and is-authenticated?
         (get session "leihs-user-session"))

    ;(and (get session "leihs-user-session")
    ;     (get session "leihs-anti-csrf-token")
    ;  )
    ))