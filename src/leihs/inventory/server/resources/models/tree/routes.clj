(ns leihs.inventory.server.resources.models.tree.routes
  (:require
   [clojure.spec.alpha :as sa]
   [leihs.core.core :refer [presence]]
   [leihs.core.resources.categories.filter :as filter]
   [leihs.core.resources.categories.tree :refer [tree]]
   [leihs.inventory.server.utils.auth.roles :as roles]
   [reitit.coercion.schema]
   [reitit.coercion.spec :as spec]
   [reitit.ring.middleware.multipart :as multipart]
   [ring.middleware.accept]
   [ring.util.response :as response]
   [schema.core :as s]))

(defn term-filter [tree request]
  (if-let [term (-> request :query-params-raw :term presence)]
    (filter/deep-filter #(re-matches (re-pattern (str "(?i).*" term ".*"))
                                     (:name %))
                        tree)
    tree))

(sa/def ::with-metadata boolean?)

(defn get-tree-route []
  ;["/:pool_id"
  ;
  ; {:swagger {:conflicting true
  ;            :tags ["Tree by pool"] :security []}}

  ["/tree"
   {:swagger {:conflicting true
              :tags ["tree"]}}

   [""
    {:get {:accept "application/json"
           :summary "Dynamic-Tree-Handler [fe]"
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
           :parameters {;:path {:pool_id uuid?}
                        :query (sa/keys :opt-un [::with-metadata])}

           :handler (fn [{{:keys [pool_id]} :path-params :as request}]
                      (let [;; TODO: reduce to provide :with-metadata=false only
                    ;; https://github.com/leihs/leihs-admin/blob/6ac7465731610563ad1986bd29e4cdd2c8a5ea79/src/leihs/admin/resources/categories/main.clj

                            with-metadata (or (-> request :parameters :query :with-metadata) false)
                            tx (:tx request)
                            res {:body {:name "categories"
                                        :children (-> (tree tx {:with-metadata with-metadata})
                                                      (term-filter request))}}]
                        res))
           :responses {200 {:description "OK"
                            :body {:name string?
                                   :children [any?]}}
                       404 {:description "Not Found"}
                       500 {:description "Internal Server Error"}}}}]])
