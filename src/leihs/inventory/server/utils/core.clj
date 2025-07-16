(ns leihs.inventory.server.utils.core
  (:require
   [ring.middleware.accept]))

(defn single-entity-get-request? [request]
  (let [method (:request-method request)
        uri (:uri request)
        uuid-regex #"([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})$"]
    (and (= method :get)
         (boolean (re-find uuid-regex uri)))))

(defn valid-image-or-thumbnail-uri? [uri]
  (boolean (re-find #"/images/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}(/thumbnail)?$" uri)))

(defn valid-attachment-uri? [uri]
  (boolean (re-find #"/attachments/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$" uri)))

(defn deep-merge [& vals]
  (if (every? map? vals)
    (apply merge-with deep-merge vals)
    (last vals)))
