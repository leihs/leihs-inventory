(ns leihs.inventory.server.resources.pool.category-tree.routes
  (:require
   [clojure.spec.alpha :as sa]
   [leihs.core.core :refer [presence]]
   [leihs.core.resources.categories.filter :as filter]
   [leihs.core.resources.categories.tree :refer [tree]]
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.pool.category-tree.main :as category-tree]
   [leihs.inventory.server.resources.pool.category-tree.types :as t]
   [leihs.inventory.server.utils.auth.roles :as roles]
   [reitit.coercion.schema]
   [reitit.coercion.spec :as spec]
   [reitit.ring.middleware.multipart :as multipart]
   [ring.middleware.accept]
   [ring.util.response :as response]
   [schema.core :as s]))

(defn get-category-tree-route []

  ["/:pool_id/category-tree/"
   {:swagger {:tags [""]}}

   [""
    {:get {:accept "application/json"
           :summary (fe "")
           :description "Fetch tree

- `with-metadata` provides additional metadata, including a base64-encoded image URL.
- default: `with-metadata=false`

Example Metadata:
```json
{
  \"metadata\": {
    \"id\": \"1e435dbc-b25e-58a4-8d95-41ef94b000a9\",
    \"name\": \"Verstärker\",
    \"label\": \"Verstärker\",
    \"models_count\": 71,
    \"is_deletable\": false,
    \"image_url\": null,
    \"thumbnail_url\": null
  }
}
```"
           :coercion spec/coercion
           :parameters {:query (sa/keys :opt-un [::t/with-metadata])
                        :path {:pool_id uuid?}}
           :handler category-tree/index-resources
           :responses {200 {:description "OK"
                            :body ::t/response-body}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}]])
