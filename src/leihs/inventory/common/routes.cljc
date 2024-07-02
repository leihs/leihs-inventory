(ns leihs.inventory.common.routes)

(def routes
  [["/inventory"
    ["" {:name :home}]

    ["/models"
     ["" {:name :models-index}]]
    ["/debug" {:name :debug-index}]

    ;; API
    ;; TODO: Decide whether to ditch "/api" and have same routes as frontend, in a RESTy way. 
    ;;       However currently the 'accept' dispatcher does not work properly so this is not possible.
    ["/api"
     ["/models"
      ["" {:name :api-models-index}]]]]])
