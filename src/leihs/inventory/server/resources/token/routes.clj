(ns leihs.inventory.server.resources.token.routes
  (:require
   [buddy.auth.backends.token :refer [jws-backend]]
   [buddy.auth.middleware :refer [wrap-authentication]]
   [buddy.sign.jwt :as jwt]
   [cider-ci.open-session.bcrypt :refer [checkpw hashpw]]
   [clojure.set]
   [clojure.string :as str]
   [clojure.test :refer :all]
   [clojure.tools.logging :as log]
   [crypto.random]
   [cryptohash-clj.api :refer :all]
   [digest :as d]
   [honey.sql :refer [format] :rename {format sql-format}]
   [honey.sql.helpers :as sql]
   [leihs.inventory.server.constants :refer [HIDE_BASIC_ENDPOINTS APPLY_DEV_ENDPOINTS]]
   [leihs.inventory.server.resources.token.main :refer [post-resource]]

   ;[leihs.inventory.server.resources.token.public.session :as ab]
   [leihs.inventory.server.resources.utils.request :refer [AUTHENTICATED_ENTITY authenticated? get-auth-entity]]

   [next.jdbc :as jdbc]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.util.response :as response]
   [schema.core :as s])
  (:import (com.google.common.io BaseEncoding)
           (java.time Duration Instant)
           (java.util Base64 UUID)))

(defn get-token-routes []
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
