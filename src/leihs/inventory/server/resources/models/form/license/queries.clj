(ns leihs.inventory.server.resources.models.form.license.queries
  ;(:require
  ; [honey.sql :refer [format]
  ;  :rename {format sql-format}]
  ; [honey.sql.helpers :as sql]
  ; [leihs.inventory.server.resources.models.helper :refer [str-to-bool]]
  ; [clojure.string :as str]
  ; [leihs.inventory.server.resources.utils.request :refer [path-params query-params]]
  ; [leihs.inventory.server.utils.converter :refer [to-uuid]]
  ; [leihs.inventory.server.utils.helper :refer [convert-map-if-exist]]
  ; [leihs.inventory.server.utils.pagination :refer [create-paginated-response fetch-pagination-params]]
  ; [next.jdbc :as jdbc]
  ; [ring.util.response :refer [bad-request response status]]
  ; [taoensso.timbre :refer [error]])
  ;(:import [java.net URL JarURLConnection]
  ;         (java.time LocalDateTime)
  ;         [java.util.jar JarFile])
  )


;-- inventoryManager 30 - ok
(ns leihs.inventory.server.resources.models.form.license.queries)

(def fields-query-inventory-manager "
  SELECT *
  FROM (
    SELECT f.id,
           f.data ->> 'label' AS label,
           f.active,
           f.position,
           f.data ->> 'group' AS group,
           f.data -> 'target_type' AS target,
           COALESCE(f.data -> 'permissions' -> 'role', '\"\"') AS role,
           f.data -> 'permissions' -> 'owner' AS owner
    FROM fields f
    WHERE f.active = true
  ) AS ff
  WHERE (ff.target = '\"license\"' OR ff.target IS NULL)
    AND (
      ff.role IN ('\"inventory_manager\"', '\"\"')
      OR (ff.target IS NULL OR owner IS NULL)
    )
  ORDER BY ff.group, ff.position;
")
