(ns leihs.inventory.server.constants)

(def ACTIVATE-DEV-MODE-REDIRECT true)
(def ACTIVATE-CSRF true)
(def ACTIVATE-SET-CSRF true)

(def APPLY_ENDPOINTS_NOT_YET_USED_BY_FE true)
(def APPLY_DEV_ENDPOINTS false)
(def HIDE_BASIC_ENDPOINTS
  "- sign-in / sign-out (html/endpoints)
- csrf-token / test-endpoints
- session-endpoints
- token-endpoints
" true)

(defn fe [s] (if false (str "[fe] | " s) s))
