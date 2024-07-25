(ns leihs.inventory.client.routing
  (:require [leihs.inventory.client.routes :as routes]
            [leihs.inventory.client.state :as state]
            [leihs.inventory.common.routes :as common-routes]
            [reitit.coercion.spec :as rcs]
            [reitit.frontend :as rf]
            [reitit.frontend.controllers :as rc]
            [reitit.frontend.easy :as re]))

(defn on-navigate [new-match]

  (println ">o> on-navigate.new-match=" new-match)

  (swap! state/route-match
    (fn [old-match]
      (println ">o> on-navigate.old-match=" old-match)
      (println ">o> on-navigate.old-match=" (:controllers old-match))
      (if new-match
        (assoc new-match
               :controllers
               (rc/apply-controllers (:controllers old-match) new-match))

        (println ">o> No new macht????" new-match old-match)
        ))))

(defn- recursive-routes-update
  "Takes a reitit-style route map, i.e. a vector of vectors, where each vector represents either
   a branch like this: [path child1 child2 ...] or a leaf like this [path settings-map].
   Applies f to each settings-map."
  [v f]
   (println ">o> abc???1" )
  (vec
    (map-indexed (fn [idx item]
                   (cond
                     (and (map? item) (= idx 1)) (f item)
                     (vector? item) (vec (recursive-routes-update item f))
                     :else item))
      v)))

(defn- resolve-routes [raw-routes handler-map]

  (println ">o> raw-routes=" raw-routes)
  (println ">o> handler-map=" handler-map)

  (recursive-routes-update
    raw-routes
    #(assoc % :view (-> % :name handler-map))))

(defn start! []
  (re/start!
    ; router
    (let [routes (resolve-routes common-routes/routes routes/handler-map)

          p (println ">o> routes=" routes)


          route-map (reduce (fn [m [k v]] (assoc m k v)) {} routes)

          p (println ">o> routes.map="  route-map)
          p (println ">o> routes.map.keys=" (keys route-map))
          ]
      (rf/router routes {:data {:coercion rcs/coercion}}))
    ; on-navigate
    on-navigate
    ; opts
    {:use-fragment false}))
