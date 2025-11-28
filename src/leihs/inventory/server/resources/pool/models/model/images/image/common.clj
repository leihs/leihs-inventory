(ns leihs.inventory.server.resources.pool.models.model.images.image.common
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [leihs.inventory.server.constants :refer [IMAGE_RESPONSE_CACHE_CONTROL]]
   [leihs.inventory.server.utils.debug :refer [log-by-severity]]
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
                 "Content-Disposition" "inline"
                 "Cache-Control" (or IMAGE_RESPONSE_CACHE_CONTROL "no-cache")}
       :body (io/input-stream (ByteArrayInputStream. decoded-bytes))})
    (catch IllegalArgumentException e
      (log-by-severity CONVERTING_ERROR e)
      {:status 400
       :body (str CONVERTING_ERROR (.getMessage e))})))

(defn parse-accept [accept-header]
  (let [accepts (-> accept-header
                    (str/split #",")
                    (->> (map #(first (str/split % #";")))
                         (map str/trim)))]
    (cond
      (> (count accepts) 1)
      {:accept-header nil :negotiation? true}

      ;; Firefox quirk → negotiation
      (some #{"text/html"} accepts)
      {:accept-header nil :negotiation? true}

      (some #{"image/*"} accepts)
      {:accept-header nil :negotiation? true}

      (some #{"application/json"} accepts)
      {:accept-header "application/json" :negotiation? false}

      (and (= 1 (count accepts))
           (str/starts-with? (first accepts) "image/"))
      {:accept-header (first accepts) :negotiation? false}

      (= 1 (count accepts))
      {:accept-header (first accepts) :negotiation? false}

      :else {:accept-header nil :negotiation? true})))

(defn handle-image-response
  [request image-data]
  (let [raw-accept (get-in request [:headers "accept"])
        {:keys [accept-header negotiation?]} (parse-accept raw-accept)
        json-request? (= accept-header "application/json")]

    (cond
      (nil? image-data)
      (status (response {:status "failure" :message "No image found"}) 404)

      json-request?
      (response image-data)

      negotiation?
      (convert-base64-to-byte-stream image-data)

      (not= (:content_type image-data) accept-header)
      (status (response {:status "failure" :message "Requested content type not supported"}) 406)

      :else (convert-base64-to-byte-stream image-data))))
