(ns leihs.inventory.server.middlewares.exception-handler-test
  (:require
   [cheshire.core :as json]
   [clojure.test :refer :all]
   [leihs.inventory.server.middlewares.exception-handler :refer [exception-handler]]
   [taoensso.timbre :refer [with-min-level]])
  (:import
   [org.postgresql.util PSQLException PSQLState]))

(def ^:private json-request
  {:uri "/inventory/pool/models/"
   :request-method :post
   :headers {"accept" "application/json"}})

(defn- body [response]
  (json/parse-string (:body response) true))

(def ^:private server-error-body
  {:status "failure" :message "Internal server error"})

(deftest server-errors-expose-no-details
  (testing "unexpected exception"
    (let [res (exception-handler json-request "Failed to create model"
                                 (Exception. "secret detail"))]
      (is (= 500 (:status res)))
      (is (= server-error-body (body res)))))

  (testing "ex-info without status"
    (let [res (exception-handler json-request nil (ex-info "secret detail" {}))]
      (is (= 500 (:status res)))
      (is (= server-error-body (body res)))))

  (testing "unmapped SQL state"
    (let [res (exception-handler json-request nil
                                 (PSQLException. "secret detail" PSQLState/UNDEFINED_TABLE))]
      (is (= 500 (:status res)))
      (is (= server-error-body (body res)))))

  (testing "response coercion"
    (let [res (with-min-level :error
                (exception-handler json-request nil
                                   (ex-info "Response coercion failed" {:in [:response :body]})))]
      (is (= 500 (:status res)))
      (is (= server-error-body (body res)))))

  (testing "attachment requested as html"
    (let [res (exception-handler (assoc json-request
                                        :uri "/inventory/pool/models/x/attachments/y"
                                        :headers {"accept" "text/html"})
                                 nil (Exception. "secret detail"))]
      (is (= 500 (:status res)))
      (is (= "Internal server error" (:body res))))))

(deftest client-errors-keep-message
  (testing "ex-info with status"
    (let [res (exception-handler json-request nil
                                 (ex-info "Referenced items exist" {:status 409}))]
      (is (= 409 (:status res)))
      (is (= "Referenced items exist" (:message (body res))))))

  (testing "unique violation with handler message"
    (let [res (exception-handler json-request "Failed to create model"
                                 (PSQLException. "duplicate key" PSQLState/UNIQUE_VIOLATION))]
      (is (= 409 (:status res)))
      (is (= {:status "failure" :message "Failed to create model"} (body res)))))

  (testing "unique violation without handler message"
    (let [res (exception-handler json-request nil
                                 (PSQLException. "duplicate key" PSQLState/UNIQUE_VIOLATION))]
      (is (= 409 (:status res)))
      (is (= {:status "failure" :message nil} (body res)))))

  (testing "request body coercion"
    (let [res (with-min-level :error
                (exception-handler json-request nil
                                   (ex-info "Request coercion failed"
                                            {:in [:request :body-params]
                                             :errors {:version '(not (non-blank-string ""))}})))]
      (is (= 422 (:status res)))
      (is (nil? (:message (body res))))
      (is (= {:version "non_blank"} (:fields (body res)))))))
