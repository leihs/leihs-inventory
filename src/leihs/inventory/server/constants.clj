(ns leihs.inventory.server.constants)

(def ACTIVATE-CSRF false)
(def APPLY_API_ENDPOINTS_NOT_USED_IN_FE true)
(def APPLY_DEV_ENDPOINTS false)
(def MAX_REQUEST_BODY_SIZE_MB 100)
(def HIDE_BASIC_ENDPOINTS
  "- sign-in / sign-out (html/endpoints)
- csrf-token / test-endpoints
- session-endpoints
- token-endpoints
- export csv/excel-endpoint
" false)

(def INVENTORY_VIEW_PATH "/inventory/")

(def PROPERTIES_PREFIX "properties_")

(defn fe [s] (if false (str "[fe] | " s) s))
