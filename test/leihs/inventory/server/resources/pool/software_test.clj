(ns leihs.inventory.server.resources.pool.software-test
  (:require
   [clojure.spec.alpha :as sa]
   [clojure.test :refer :all]
   [leihs.inventory.server.resources.pool.models.helper :refer [normalize-model-data]]
   [leihs.inventory.server.resources.pool.software.software.types]
   [leihs.inventory.server.resources.pool.software.types]))

(deftest post-and-put-specs-accept-nil-technical-detail
  (testing "POST spec accepts explicit nil in technical_detail"
    (is (sa/valid? :software-post/multipart
                   {:product "Adobe"
                    :technical_detail nil})))
  (testing "PUT spec accepts explicit nil in technical_detail"
    (is (sa/valid? :leihs.inventory.server.resources.pool.software.software.types/put-query
                   {:product "Adobe"
                    :technical_detail nil})))
  (testing "POST and PUT keep product as required"
    (is (false? (sa/valid? :software-post/multipart
                           {:technical_detail nil})))
    (is (false? (sa/valid? :leihs.inventory.server.resources.pool.software.software.types/put-query
                           {:technical_detail nil})))))

(deftest normalize-model-data-default-ignores-nil-fields
  (let [normalized (normalize-model-data {:product "Acme"
                                          :version nil
                                          :technical_detail nil
                                          :hand_over_note nil})]
    ;; Default path is used by non-Software model PUT and must not wipe columns.
    (is (= {:product "Acme"} normalized))))

(deftest normalize-model-data-can-preserve-explicit-nil-fields
  (let [normalized (normalize-model-data {:product "Acme"
                                          :version nil
                                          :technical_detail nil
                                          :is_package nil}
                                         true)]
    ;; Software can opt in to preserve explicit nil for DB-side behavior.
    (is (contains? normalized :version))
    (is (nil? (:version normalized)))
    (is (contains? normalized :technical_detail))
    (is (nil? (:technical_detail normalized)))
    (is (contains? normalized :is_package))
    (is (nil? (:is_package normalized)))))

