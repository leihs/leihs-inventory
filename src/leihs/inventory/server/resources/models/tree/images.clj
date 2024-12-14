(ns leihs.inventory.server.resources.models.tree.images
  (:require [leihs.core.core :refer [presence str]])
  (:import
   [java.awt.image BufferedImage]
   [java.io ByteArrayInputStream ByteArrayOutputStream]
   [java.util Base64]
   [javax.imageio ImageIO]))

(def IMG-DATA-URL-PREFIX "data:image/jpeg;base64")

(defn prefix-with-data-url [base64]
  (clojure.string/join "," [IMG-DATA-URL-PREFIX base64]))

(defn assert-proper-image-type! [prefix]
  (when-not (= IMG-DATA-URL-PREFIX prefix)
    (throw (ex-info "Image is not of expected type 'data:image/jpeg;base64'!"
                    {:status 422
                     :body (str "The first chars of img256_url must be equal to data:image/jpeg;base64. "
                                "See also https://tools.ietf.org/html/rfc2397.")}))))

(defn data-url-img->buffered-image ^BufferedImage [data-url-img]
  (let [[img-type img-s] (clojure.string/split data-url-img #"," 2)
        img-ba (.decode (Base64/getDecoder) (.getBytes img-s "UTF-8"))
        in (ByteArrayInputStream. img-ba)]
    (assert-proper-image-type! img-type)
    (ImageIO/read in)))

(defn buffered-image->data-url-img ^String [^BufferedImage img]
  (let [os (ByteArrayOutputStream.)
        _ (ImageIO/write img "jpg" os)
        ba (.toByteArray os)
        base64 (.encodeToString (Base64/getEncoder) ba)]
    (prefix-with-data-url base64)))

(defn resized-img ^BufferedImage [^BufferedImage img ^Integer dim]
  (let [img-buffer (BufferedImage. dim dim (.getType img))
        graphics (.createGraphics img-buffer)]
    (.drawImage graphics img 0 0 dim dim nil)
    (.dispose graphics)
    img-buffer))

(defn remove-images [data]
  (assoc data
         :img256_url nil
         :img32_url nil))

(defn set-images [data img-data-url]
  (let [img (data-url-img->buffered-image img-data-url)
        img256-data-url (-> img (resized-img 256) buffered-image->data-url-img)
        img32-data-url (-> img (resized-img 32) buffered-image->data-url-img)]
    (assoc data
           :img256_url img256-data-url
           :img32_url img32-data-url)))

(defn process-images [data]
  (if-let [img-data-url (-> data :img256_url presence)]
    (set-images data img-data-url)
    (if (and (contains? data :img256_url)
             (not (-> data :img256_url presence)))
      (remove-images data)
      data)))
