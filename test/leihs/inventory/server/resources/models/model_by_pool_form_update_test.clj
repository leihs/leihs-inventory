(ns leihs.inventory.server.resources.models.model-by-pool-form-update-test
  (:require [cheshire.core :as cjson]
            [clojure.test :refer :all]
            [leihs.inventory.server.resources.models.helper :refer [parse-json-array]])
  (:require [leihs.inventory.server.resources.models.form.model.model-by-pool-form-update :refer :all]))

(deftest parse-json-array-tests
  (testing "Empty input"
    (is (= (parse-json-array {:parameters {:multipart {:properties ""}}} :properties) []))
    (is (= (parse-json-array {:parameters {:multipart {:properties nil}}} :properties) []))
    (is (= (parse-json-array {:parameters {:multipart {:properties "[]"}}} :properties) [])))

  (testing "Single map string"
    (is (= (parse-json-array {:parameters {:multipart {:properties "{}"}}} :properties)
           [])))

  (testing "Single map string"
    (is (= (parse-json-array {:parameters {:multipart {:properties "{\"key\": \"string\"}"}}} :properties)
           [{:key "string"}])))

  (testing "Multiple map string"
    (is (= (parse-json-array {:parameters {:multipart
                                           {:properties "{\"key\": \"string\", \"value\": \"string\"}"}}} :properties)
           [{:key "string", :value "string"}])))

  (testing "Multiple maps without brackets"
    (is (= (parse-json-array {:parameters {:multipart
                                           {:properties "{\"key\": \"string\", \"value\": \"string\"},
                                           {\"key\": \"string2\", \"value\": \"string\"}"}}} :properties)
           [{:key "string", :value "string"}
            {:key "string2", :value "string"}])))

  (testing "Multiple maps with brackets"
    (is (= (parse-json-array {:parameters {:multipart
                                           {:properties "[{\"key\": \"string\", \"value\": \"string\"},
                                           {\"key\": \"string2\", \"value\": \"string\"}]"}}} :properties)
           [{:key "string", :value "string"}
            {:key "string2", :value "string"}])))

  (testing "Invalid JSON input"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"Invalid JSON Array Format"
         (parse-json-array {:parameters {:multipart {:properties "Invalid JSON"}}} :properties)))))
