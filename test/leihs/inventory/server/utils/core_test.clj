(ns leihs.inventory.server.utils.core-test
  (:require [clojure.test :refer :all]
   [leihs.inventory.server.utils.core :refer [single-entity-get-request?]]))

(deftest single-entity-get-request-tests
  ;; Testing GET request without UUID in the URI
  ;(testing "GET request without UUID in the URI"
  ;  (let [request {:request-method :get
  ;                 :uri "/api/items/list"}]
  ;    (is (false? (single-entity-get-request? request)))))

  ;; Testing GET request with valid UUID in the URI
  (testing "GET request with UUID in the URI"
    (let [request {:request-method :get
                   :uri "/api/items/8bd16d45-056d-5590-bc7f-12849f034351"}]
      (is (true? (single-entity-get-request? request)))))

  (testing "GET request with UUID in the URI"
    (let [request {:request-method :get
                   :uri "/api/items/8bd16d45-056d-5590-bc7f-12849f034351/test"}]
      (is (false? (single-entity-get-request? request)))))

  ;; Testing non-GET request with UUID in the URI
  (testing "Non-GET request with UUID in the URI"
    (let [request {:request-method :post
                   :uri "/api/items/8bd16d45-056d-5590-bc7f-12849f034351"}]
      (is (false? (single-entity-get-request? request)))))

  ;; Testing non-GET request without UUID in the URI
  (testing "Non-GET request without UUID in the URI"
    (let [request {:request-method :put
                   :uri "/api/items/list"}]
      (is (false? (single-entity-get-request? request)))))

  ;; Testing GET request with malformed UUID in the URI
  (testing "GET request with malformed UUID in the URI"
    (let [request {:request-method :get
                   :uri "/api/items/8bd16d45-xyz-5590-bc7f-12849f034351"}]
      (is (false? (single-entity-get-request? request)))))

  ;; Testing GET request with no UUID in URI (root path)
  (testing "GET request with no UUID in URI (root path)"
    (let [request {:request-method :get
                   :uri "/"}]
      (is (false? (single-entity-get-request? request)))))

  ;; Testing GET request with a similar but incorrect UUID pattern
  (testing "GET request with a similar but incorrect UUID pattern"
    (let [request {:request-method :get
                   :uri "/api/items/8bd16d45-056d-5590-bc7f-12345678901"}]
      (is (false? (single-entity-get-request? request)))))

  ;; Testing GET request with empty URI
  (testing "GET request with empty URI"
    (let [request {:request-method :get
                   :uri ""}]
      (is (false? (single-entity-get-request? request)))))
  )
