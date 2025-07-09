(ns leihs.inventory.server.resources.pool.suppliers.types
  (:require
   ;[clojure.set]
   ;[leihs.inventory.server.constants :refer [fe]]
   ;;[leihs.inventory.server.resources.auth.session :as session]
   ;[leihs.inventory.server.resources.pool.suppliers.main :refer [index-resources]]
   ;[leihs.inventory.server.resources.utils.middleware :refer [accept-json-middleware]]
   [leihs.inventory.server.utils.coercion.core :refer [pagination]]
   ;[leihs.inventory.server.utils.response_helper :as rh]
   ;[reitit.coercion.schema]
   ;[reitit.coercion.spec]
   ;[ring.middleware.accept]
   [schema.core :as s]))

(s/defschema  resp-supplier [{:id s/Uuid
                     :name s/Str
                     :note (s/maybe s/Str)}])

(s/defschema get-response (s/->Either [resp-supplier {:data resp-supplier
                                                                  :pagination pagination}]))
