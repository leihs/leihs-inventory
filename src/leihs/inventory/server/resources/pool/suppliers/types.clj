(ns leihs.inventory.server.resources.pool.suppliers.types
  (:require
   [leihs.inventory.server.utils.coercion.core :refer [pagination]]
   [schema.core :as s]))

(s/defschema resp-supplier [{:id s/Uuid
                             :name s/Str
                             :note (s/maybe s/Str)}])

(s/defschema get-response (s/->Either [resp-supplier {:data resp-supplier
                                                      :pagination pagination}]))
