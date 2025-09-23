(ns leihs.inventory.server.resources.pool.models.model.images.image.common
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [leihs.inventory.server.resources.pool.models.model.images.image.constants :refer [CONTENT_NEGOTIATION_TYPE_IMAGE
                                                                                      ALLOWED_IMAGE_CONTENT_TYPES]]
   [leihs.inventory.server.utils.helper :refer [log-by-severity]]
   [ring.util.response :refer [response status]])
  (:import
   [java.io ByteArrayInputStream]
   [java.util Base64]))

(def CONVERTING_ERROR "Failed to convert Base64 string")
(defn- clean-base64-string [base64-str]
  (clojure.string/replace base64-str #"\s+" ""))

(defn- url-safe-to-standard-base64 [base64-str]
  (-> base64-str
      (clojure.string/replace "-" "+")
      (clojure.string/replace "_" "/")))

(defn- add-padding [base64-str]
  (let [mod (mod (count base64-str) 4)]
    (cond
      (= mod 2) (str base64-str "==")
      (= mod 3) (str base64-str "=")
      :else base64-str)))

(defn- decode-base64-str [base64-str]
  (let [cleaned-str (-> base64-str
                        clean-base64-string
                        url-safe-to-standard-base64
                        add-padding)
        decoder (Base64/getDecoder)]
    (.decode decoder cleaned-str)))

(defn convert-base64-to-byte-stream [image-data]
  (try
    (let [content-type (:content_type image-data)
          base64-str (:content image-data)
          decoded-bytes (decode-base64-str base64-str)]
      {:status 200
       :headers {"Content-Type" content-type
                 "Content-Disposition" "inline"}
       :body (io/input-stream (ByteArrayInputStream. decoded-bytes))})
    (catch IllegalArgumentException e
      (log-by-severity CONVERTING_ERROR e)
      {:status 400
       :body (str CONVERTING_ERROR (.getMessage e))})))

(defn handle-image-response
  [request image-data]
  (let [accept-header (if (str/includes? (get-in request [:headers "accept"])
                                         CONTENT_NEGOTIATION_TYPE_IMAGE)
                        CONTENT_NEGOTIATION_TYPE_IMAGE
                        (get-in request [:headers "accept"]))
        json-request? (= accept-header "application/json")
        content-negotiation? (str/includes? accept-header CONTENT_NEGOTIATION_TYPE_IMAGE)
        valid-content-type? (boolean (and accept-header
                                          (some #(= accept-header %) ALLOWED_IMAGE_CONTENT_TYPES)))]

    (cond
      (nil? image-data)
      (status (response {:status "failure" :message "No image found"}) 404)

      json-request?
      (response image-data)

      content-negotiation?
      (convert-base64-to-byte-stream image-data)

      (or (not= (:content_type image-data) accept-header)
          (not valid-content-type?))
      (status (response {:status "failure" :message "Requested content type not supported"}) 406)

      :else (convert-base64-to-byte-stream image-data))))
