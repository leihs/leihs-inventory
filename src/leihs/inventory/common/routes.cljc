(ns leihs.inventory.common.routes)

(def routes
  [["/inventory"
    ["" {:name :home}]

    ["/models"
     ["" {:name :models-index}]]
    ["/debug" {:name :debug-index}]

    ;; API
    ;; TODO: Decide whether to ditch "/api" and have same routes as frontend. 
    ;;       However currently the 'accept' dispatcher does not work properly, this has to be fixed first.
    ["/api"
     ["/models"
      ["" {:name :api-models-index}]]]]])
