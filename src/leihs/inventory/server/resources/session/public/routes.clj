(ns leihs.inventory.server.resources.session.public.routes
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
   [leihs.inventory.server.constants :refer [HIDE_BASIC_ENDPOINTS]]
   [leihs.inventory.server.resources.session.public.main :refer [get-resource]]
   [next.jdbc :as jdbc]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.util.response :as response]
   [schema.core :as s]))

(defn get-session-public-routes []
  ["/"

   {:no-doc HIDE_BASIC_ENDPOINTS}

   ["session"
    {:tags ["Auth / Session"]}

    ["/public"
     {:get {:swagger {:security []}
            :handler get-resource}}]]])
