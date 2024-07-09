(ns leihs.inventory.server.paths
  (:require [bidi.verbose :refer [branch leaf]]
            [leihs.core.paths]
            [leihs.inventory.common.routes :as common.routes]))

#_(def paths
    (branch
     ""
     leihs.core.paths/core-paths
     (branch "/inventory"
             (leaf "/" :home)
             (branch "/api"
                     (branch "/models"
                             (leaf "" :api-models-index))))
     (leaf true :not-found)))

; TEMPORARY ADAPTER until bidi is replaced by reitit
; TODO: does not support `param` yet
; TODO: does not add :not-found, which means that every route is status 200 (!?)
(defn reitit-to-bidi [m]
  (cond
    (-> m second map?) (leaf (-> m first) (-> m second :name))
    (-> m count (= 1)) (-> m first reitit-to-bidi)
    :else (apply branch (-> m first) (vec (map reitit-to-bidi (-> m rest))))))

(def paths (reitit-to-bidi common.routes/routes))

(reset! leihs.core.paths/paths* paths)

(def path leihs.core.paths/path)
