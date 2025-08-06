(ns leihs.inventory.server.resources.pool.software.software.types
  (:require
   [clojure.spec.alpha :as sa]
   [leihs.inventory.server.resources.pool.models.basic_coercion :as sp]
   [reitit.coercion.schema]))

(sa/def ::put-query (sa/keys :req-un [::sp/product]
                             :opt-un [:nil/version
                                      :nil/manufacturer
                                      :nil/technical_detail]))

(sa/def ::put-response
  (sa/keys :req-un [:models/type
                    ::sp/product
                    ::sp/id
                    :nil/manufacturer
                    :nil/version]
           :opt-un [:nil/technical_detail
                    ::sp/attachments]))

(sa/def ::attachment
  (sa/keys :req-un [:any/id :any/model_id ::sp/filename ::sp/size]))

(sa/def ::model
  (sa/keys :req-un [:any/id ::sp/product ::sp/manufacturer]))

(sa/def ::deleted_attachments
  (sa/coll-of ::attachment :kind vector? :min-count 0))

(sa/def ::deleted_model
  (sa/coll-of ::model :kind vector? :min-count 0))

(sa/def ::delete-response
  (sa/keys :req-un
           [::deleted_attachments
            ::deleted_model]))
