(ns leihs.inventory.server.utils.accept-parser-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [leihs.inventory.server.utils.accept-parser :as parser]))

(deftest parse-accept-header-test
  (testing "Simple cases"
    (is (= {:types #{"*/*"} :has-wildcard? true}
           (parser/parse-accept-header "*/*")))
    (is (= {:types #{"application/json"} :has-wildcard? false}
           (parser/parse-accept-header "application/json"))))

  (testing "Multiple types with q-values"
    (let [chrome-accept "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8"
          parsed (parser/parse-accept-header chrome-accept)]
      (is (contains? (:types parsed) "text/html"))
      (is (contains? (:types parsed) "image/webp"))
      (is (contains? (:types parsed) "image/apng"))
      (is (contains? (:types parsed) "*/*"))
      (is (:has-wildcard? parsed))))

  (testing "Firefox Accept header"
    (let [firefox-accept "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
          parsed (parser/parse-accept-header firefox-accept)]
      (is (contains? (:types parsed) "text/html"))
      (is (contains? (:types parsed) "*/*"))
      (is (:has-wildcard? parsed))))

  (testing "Nil/empty defaults to */*"
    (is (= {:types #{"*/*"} :has-wildcard? true}
           (parser/parse-accept-header nil)))
    (is (= {:types #{"*/*"} :has-wildcard? true}
           (parser/parse-accept-header "")))))

(deftest accepts-type-test
  (testing "Wildcard accepts everything"
    (let [parsed (parser/parse-accept-header "*/*")]
      (is (parser/accepts-type? parsed "application/json"))
      (is (parser/accepts-type? parsed "text/html"))
      (is (parser/accepts-type? parsed "image/png"))))

  (testing "Specific type matching"
    (let [parsed (parser/parse-accept-header "application/json")]
      (is (parser/accepts-type? parsed "application/json"))
      (is (not (parser/accepts-type? parsed "text/html")))))

  (testing "Image wildcard"
    (let [parsed (parser/parse-accept-header "image/*")]
      (is (parser/accepts-type? parsed "image/png"))
      (is (parser/accepts-type? parsed "image/jpeg"))
      (is (parser/accepts-type? parsed "image/webp"))
      (is (not (parser/accepts-type? parsed "application/json")))))

  (testing "Multiple types without wildcard"
    (let [parsed (parser/parse-accept-header "text/html,application/json")]
      (is (parser/accepts-type? parsed "text/html"))
      (is (parser/accepts-type? parsed "application/json"))
      (is (not (parser/accepts-type? parsed "image/png"))))))

(deftest can-satisfy-any-test
  (testing "Browser Accept with wildcard can satisfy JSON route"
    (let [chrome-accept "text/html,image/webp,image/apng,*/*;q=0.8"
          parsed (parser/parse-accept-header chrome-accept)
          json-route ["application/json"]]
      (is (parser/can-satisfy-any? parsed json-route))))

  (testing "Image-only Accept cannot satisfy JSON route"
    (let [parsed (parser/parse-accept-header "image/jpeg")
          json-route ["application/json"]]
      (is (not (parser/can-satisfy-any? parsed json-route)))))

  (testing "JSON Accept can satisfy JSON route"
    (let [parsed (parser/parse-accept-header "application/json")
          json-route ["application/json"]]
      (is (parser/can-satisfy-any? parsed json-route))))

  (testing "Image wildcard can satisfy image route"
    (let [parsed (parser/parse-accept-header "image/*")
          image-route ["image/png" "image/jpeg"]]
      (is (parser/can-satisfy-any? parsed image-route))))

  (testing "Empty produces means no constraint"
    (let [parsed (parser/parse-accept-header "image/png")]
      (is (parser/can-satisfy-any? parsed []))
      (is (parser/can-satisfy-any? parsed nil)))))

(deftest is-image-only-request-test
  (testing "Image-only requests"
    (is (parser/is-image-only-request?
         (parser/parse-accept-header "image/png")))
    (is (parser/is-image-only-request?
         (parser/parse-accept-header "image/jpeg, image/webp")))
    (is (parser/is-image-only-request?
         (parser/parse-accept-header "image/*"))))

  (testing "Browser requests with wildcards are NOT image-only"
    (is (not (parser/is-image-only-request?
              (parser/parse-accept-header "text/html,image/webp,*/*"))))
    (is (not (parser/is-image-only-request?
              (parser/parse-accept-header "image/webp,*/*")))))

  (testing "Mixed content types are NOT image-only"
    (is (not (parser/is-image-only-request?
              (parser/parse-accept-header "text/html,image/png"))))
    (is (not (parser/is-image-only-request?
              (parser/parse-accept-header "application/json,image/png")))))

  (testing "Non-image requests are NOT image-only"
    (is (not (parser/is-image-only-request?
              (parser/parse-accept-header "text/html"))))
    (is (not (parser/is-image-only-request?
              (parser/parse-accept-header "application/json"))))
    (is (not (parser/is-image-only-request?
              (parser/parse-accept-header "*/*"))))))
