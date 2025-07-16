(ns leihs.inventory.server.resources.pool.category-tree.routes
  (:require
   [clojure.spec.alpha :as sa]
   [leihs.inventory.server.constants :refer [fe]]
   [leihs.inventory.server.resources.pool.category-tree.main :as category-tree]
   [leihs.inventory.server.resources.pool.category-tree.types :as types]
   [reitit.coercion.schema]
   [reitit.coercion.spec :as spec]
   [ring.middleware.accept]))

(defn routes []

  ["/category-tree/"
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
           :parameters {:query (sa/keys :opt-un [::types/with-metadata])
                        :path {:pool_id uuid?}}
           :handler category-tree/index-resources
           :responses {200 {:description "OK"
                            :body ::types/response-body}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}]])
