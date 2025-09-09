(ns leihs.inventory.server.resources.pool.options.option.types
  (:require
   [clojure.spec.alpha :as sa]
   [leihs.inventory.server.resources.pool.models.basic-coercion :as sp]
   [reitit.coercion.schema]))

(sa/def ::get-response-option
  (sa/keys :req-un [:any/id
                    ::sp/inventory_code
                    ::sp/product
                    ::sp/name]
           :opt-un [:nil/version :nil-pos-number/price ::sp/is_deletable]))
