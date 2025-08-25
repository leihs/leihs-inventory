(ns leihs.inventory.server.resources.pool.software.software.types
  (:require
   [clojure.spec.alpha :as sa]
   [leihs.inventory.server.resources.pool.models.basic-coercion :as sp]
   [reitit.coercion.schema]))

(sa/def :software-put/multipart (sa/keys :req-un [::sp/product]
                                         :opt-un [:nil/version
                                                  :nil/manufacturer
                                                  :nil/technical_detail]))

(sa/def ::response
  (sa/keys :req-un [:models/type
                    ::sp/product
                    ::sp/id
                    :nil/manufacturer
                    :nil/version]
           :opt-un [:nil/technical_detail
                    ::sp/attachments
                    ::sp/is_deletable]))

(def delete-response {:deleted_attachments [{:id uuid?
                                             :model_id uuid?
                                             :filename string?
                                             :size number?}]
                      :deleted_model [{:id uuid?
                                       :product string?
                                       :manufacturer any?}]})
