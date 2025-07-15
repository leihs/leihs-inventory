(ns leihs.inventory.server.resources.token.routes
  (:require
   [leihs.inventory.server.constants :refer [HIDE_BASIC_ENDPOINTS ]]
   [leihs.inventory.server.resources.token.main :refer [post-resource]]
   [schema.core :as s]
    )
  (:import (com.google.common.io BaseEncoding)
           (java.time Duration Instant)
           (java.util Base64 UUID)))

(defn routes []
  [["/"

    {:no-doc HIDE_BASIC_ENDPOINTS}

    ["token"
     {:tags ["Auth / Token"]}

     ["/"
      {:post {:summary "Create an API token with creds for a user"
              :description "Generates an API token for a user with specific permissions and scopes"
              :accept "application/json"
              :coercion reitit.coercion.schema/coercion
              :parameters {:body {:description s/Str
                                  :scopes {:read s/Bool
                                           :write s/Bool
                                           :admin_read s/Bool
                                           :admin_write s/Bool}}}
              :handler post-resource}}]]]])
