(ns leihs.inventory.server.constants)

(def ACTIVATE-CSRF true)
(def APPLY_API_ENDPOINTS_NOT_USED_IN_FE true)
(def APPLY_DEV_ENDPOINTS false)
(def MAX_REQUEST_BODY_SIZE_MB 100)
(def HIDE_BASIC_ENDPOINTS
  "- sign-in / sign-out (html/endpoints)
- csrf-token / test-endpoints
- session-endpoints
- token-endpoints
" true)

(def INVENTORY_VIEW_PATH "/inventory/")

;; nil sets "no cache"-control
(def IMAGE_RESPONSE_CACHE_CONTROL "public, max-age=2592000, immutable") ;30days

(defn fe [s] (if false (str "[fe] | " s) s))
