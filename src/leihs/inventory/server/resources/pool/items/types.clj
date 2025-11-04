(ns leihs.inventory.server.resources.pool.items.types
  (:require
   [clojure.spec.alpha :as sa]
   [schema.core :as s]))

(s/defschema query-params {(s/optional-key :page) s/Int
                           (s/optional-key :size) s/Int
                           (s/optional-key :search_term) s/Str
                           (s/optional-key :not_packaged) s/Bool
                           (s/optional-key :packages) s/Bool
                           (s/optional-key :retired) s/Bool
                           :result_type (s/enum "Min" "Normal" "Distinct")})

(s/defschema query-params-advanced
                          {(s/optional-key :page) s/Int
                           (s/optional-key :size) s/Int
                           (s/optional-key :filters) s/Str
                           :result_type (s/enum "Min" "Normal" "Distinct")

})


;(def data-response
;                          {(s/optional-key :inventory_code) s/Str
;                           (s/optional-key :note) s/Str
;                           (s/optional-key :id) s/Str
;                           })
(def data-response {:inventory_code s/Str
                            :note s/Str
                          :id s/Str
                           })
