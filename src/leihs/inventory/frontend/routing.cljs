(ns leihs.inventory.frontend.routing
  (:require [leihs.inventory.common.routes :as common-routes]
            [leihs.inventory.frontend.routes :as routes]
            [leihs.inventory.frontend.state :as state]
            [reitit.coercion.spec :as rcs]
            [reitit.frontend :as rf]
            [reitit.frontend.controllers :as rc]
            [reitit.frontend.easy :as re]))

(defn on-navigate [new-match]
  (swap! state/route-match
         (fn [old-match]
           (when new-match
             (assoc new-match
                    :controllers
                    (rc/apply-controllers (:controllers old-match) new-match))))))

(defn- recursive-routes-update
  "Takes a reitit-style route map, i.e. a vector of vectors, where each vector represents either
   a branch like this: [path child1 child2 ...] or a leaf like this [path settings-map].
   Applies f to each settings-map."
  [v f]
  (vec
   (map-indexed (fn [idx item]
                  (cond
                    (and (map? item) (= idx 1)) (f item)
                    (vector? item) (vec (recursive-routes-update item f))
                    :else item))
                v)))

(defn- resolve-routes [raw-routes handler-map]
  (recursive-routes-update
   raw-routes
   #(assoc % :view (-> % :name handler-map))))

(defn start! []
  (re/start!
   ; router
   (let [routes (resolve-routes common-routes/routes routes/handler-map)]
     (rf/router routes {:data {:coercion rcs/coercion}}))
   ; on-navigate
   on-navigate
   ; opts
   {:use-fragment false}))
