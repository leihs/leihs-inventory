(ns leihs.inventory.server.resources.pool.options.option.types
  (:require
   [clojure.spec.alpha :as sa]
   [leihs.inventory.server.resources.pool.models.basic_coercion :as sp]
   [reitit.coercion.schema]))

(def response-option-object {:id uuid?
                             :inventory_code string?
                             :product string?
                             :name string?
                             :version (sa/nilable string?)
                             :price (sa/nilable :nil-pos-number/price)})

(sa/def :get-response/option
  (sa/keys :req-un [::sp/id
                    ::sp/inventory_code
                    ::sp/product
                    ::sp/name]
           :opt-un [:nil/version :nil-pos-number/price ::sp/is_deletable]))

(sa/def ::put-option-query-body (sa/keys :req-un [::sp/product
                                                  ::sp/inventory_code]
                                         :opt-un [:nil/version
                                                  :nil-pos-number/price]))
