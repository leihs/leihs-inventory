(ns leihs.inventory.common.routes)

(def routes
  [["/inventory"
    ["" {:name :home}]

    ["/models" {:name :models-index}]
    ["/debug" {:name :debug-index}]]])
