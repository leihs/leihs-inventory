(ns leihs.inventory.server.constants)

(def ACTIVATE-DEV-MODE-REDIRECT true)
(def ACTIVATE-CSRF true)
(def APPLY_API_ENDPOINTS_NOT_USED_IN_FE true)
(def APPLY_DEV_ENDPOINTS false)
(def HIDE_BASIC_ENDPOINTS
  "- sign-in / sign-out (html/endpoints)
- csrf-token / test-endpoints
- session-endpoints
- token-endpoints
" false)

(defn fe [s] (if false (str "[fe] | " s) s))
