(ns leihs.inventory.server.resources.settings.types
  (:require
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(def response-schema
  {:user_image_url (s/maybe s/Str) ; Optional string
   :logo_url (s/maybe s/Str) ; Optional string
   :local_currency_string (s/maybe s/Str) ; Optional string
   :time_zone s/Str ; Required string
   :lending_terms_url (s/maybe s/Str) ; Optional string
   :documentation_link (s/maybe s/Str) ; Optional string
   })
