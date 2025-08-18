(ns leihs.inventory.server.resources.pool.software.types
  (:require
   [clojure.spec.alpha :as sa]
   [leihs.inventory.server.resources.pool.models.basic-coercion :as sp]
   [reitit.coercion.schema]))

(sa/def :software-post/multipart (sa/keys :req-un [::sp/product]
                                          :opt-un [:nil/version
                                                   :nil/manufacturer
                                                   ::sp/technical_detail]))

(sa/def ::post-response
  (sa/keys :req-un [:models/type
                    ::sp/product
                    ::sp/id
                    :nil/manufacturer
                    :nil/version]
           :opt-un [:nil/technical_detail
                    ::sp/attachments]))
