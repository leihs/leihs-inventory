(ns leihs.inventory.server.resources.session.public.main)

(defn get-resource [request]
  {:status 200
   :body {:reuqest-method (:request-method request)
          :request-url (:uri request)
          :message "Hello, World!"}})
