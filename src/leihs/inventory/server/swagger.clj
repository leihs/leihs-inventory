(ns leihs.inventory.server.swagger
  (:require
   [reitit.swagger-ui :as swagger-ui]))

(defn init []
  (swagger-ui/create-swagger-ui-handler
   {:path "/inventory/api-docs/"
    :root "swagger-ui"
    :config {:validatorUrl nil
             :urls [{:name "swagger" :url "swagger.json"}]
             :urls.primaryName "openapi"
             :operationsSorter "alpha"}}))
