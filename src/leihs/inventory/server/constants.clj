(ns leihs.inventory.server.constants)

(def ACTIVATE-DEV-MODE-REDIRECT true)
(def ACTIVATE-CSRF true)
(def ACTIVATE-SET-CSRF true)

(def HIDE_BASIC_ENDPOINTS
  "- sign-in / sign-out (html/endpoints)
- csrf-token / test-endpoints
- session-endpoints
- token-endpoints
" false)

(def HIDE_DEV_ENDPOINTS
  "- update login
- login / logout
- set password
" false)