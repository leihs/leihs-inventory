(ns leihs.inventory.server.resources.pool.fields.routes
  (:require
   [clojure.set]
   [leihs.inventory.server.resources.pool.fields.main :as fields]
   [leihs.inventory.server.utils.middleware :refer [accept-json-middleware]]
   [reitit.coercion.schema]
   [reitit.coercion.spec]
   [ring.middleware.accept]
   [schema.core :as s]))

(defn routes []
  ["/fields/"
   {:get {:description
          (str "<ul>"
               "<li>Form: https://staging.leihs.zhdk.ch/manage/8bd16d45-056d-5590-bc7f-12849f034351/fields?target_type=itemRequest</li>"
               "<li>ToDo: Fields by User/:pool_id?</li>"
               "<ul/>")
          :accept "application/json"
          :coercion reitit.coercion.schema/coercion
          :middleware [accept-json-middleware]
          :swagger {:produces ["application/json"]}
          :parameters {:path {:pool_id s/Uuid}
                       :query {(s/optional-key :type) (s/enum :item :license)}}
          :handler fields/index-resources
          :responses {200 {:description "OK"
                           :body s/Any} ;;FIXME
                      404 {:description "Not Found"}
                      500 {:description "Internal Server Error"}}}}])
