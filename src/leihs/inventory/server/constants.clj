(ns leihs.inventory.server.constants
  ;(:refer-clojure :exclude
  ; [keyword replace])
  ;(:require
  ; [cheshire.core :as json]
  ; [clojure.string :as str]
  ; [clojure.java.io :as io]
  ; [leihs.core.anti-csrf.back :refer [anti-csrf-props anti-csrf-token]]
  ; [leihs.core.auth.session :refer [wrap-authenticate]]
  ; [leihs.core.constants :as constants]
  ; [leihs.core.constants]
  ; [leihs.core.sign-in.back :as be]
  ; [leihs.core.sign-in.simple-login :refer [sign-in-view]]
  ; [leihs.core.sign-out.back :as so]
  ; [leihs.core.status :as status]
  ; [leihs.inventory.server.resources.auth.auth-routes :refer [authenticate-handler
  ;                                                            logout-handler
  ;                                                            set-password-handler
  ;                                                            token-routes]]
  ; [leihs.inventory.server.resources.auth.session :as ab]
  ; [leihs.inventory.server.resources.categories.routes :refer [get-categories-routes]]
  ; [leihs.inventory.server.resources.fields.routes :refer [get-fields-routes]]
  ; [leihs.inventory.server.resources.images.routes :refer [get-images-routes]]
  ; [leihs.inventory.server.resources.items.routes :refer [get-items-routes]]
  ; [leihs.inventory.server.resources.models.main]
  ; [leihs.inventory.server.resources.models.routes :refer [get-model-by-pool-route get-model-route]]
  ; [leihs.inventory.server.resources.owner-department.routes :refer [get-owner-department-routes]]
  ; [leihs.inventory.server.resources.pools.routes :refer [get-pools-routes]]
  ; [leihs.inventory.server.resources.properties.routes :refer [get-properties-routes]]
  ; [leihs.inventory.server.resources.supplier.routes :refer [get-supplier-routes]]
  ; [leihs.inventory.server.resources.user.routes :refer [get-user-routes]]
  ; [leihs.inventory.server.utils.html-utils :refer [add-csrf-tags add-csrf-tags2]]
  ; [muuntaja.core :as m]
  ; [reitit.coercion.schema]
  ; [reitit.coercion.spec]
  ; [reitit.openapi :as openapi]
  ; [reitit.ring.middleware.muuntaja :as muuntaja]
  ; [reitit.swagger :as swagger]
  ; [ring.middleware.accept]
  ; [ring.middleware.accept]
  ; [ring.util.response :refer [bad-request redirect response status]]
  ; [schema.core :as s]
  ; )
  )

(def ACTIVATE-DEV-MODE-REDIRECT true)
(def ACTIVATE-CSRF true)
(def ACTIVATE-SET-CSRF true)
