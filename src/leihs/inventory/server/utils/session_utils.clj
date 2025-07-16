(ns leihs.inventory.server.utils.session-utils
  (:require
   [clojure.string :as clojure.string]
   [leihs.inventory.server.utils.request-utils :refer [authenticated?]]))

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
        is-authenticated? (authenticated? request)]
    (and is-authenticated?
         (get session "leihs-user-session"))))
