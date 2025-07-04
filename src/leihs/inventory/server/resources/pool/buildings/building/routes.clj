(ns leihs.inventory.server.resources.pool.buildings.building.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.pool.buildings.building.main :refer [get-buildings-handler ]]
   [leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.response_helper :as rh]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn get-buildings-single-routes []
  ["/:pool_id"
   {:swagger {:tags [""]}}

   ["/buildings/"

    [":building_id"
     {:get {:summary (fe "")
            :accept "application/json"
            :coercion reitit.coercion.schema/coercion
            :middleware [accept-json-middleware]
            :swagger {:produces ["application/json"]}
            :parameters {:path {:building_id s/Uuid}}
            :handler get-buildings-handler
            :responses {200 {:description "OK"
                             :body [{:id s/Uuid
                                     :name s/Str
                                     :code (s/maybe s/Str)}]}
                        404 {:description "Not Found"}
                        500 {:description "Internal Server Error"}}}}]

    ]
])
