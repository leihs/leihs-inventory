(ns leihs.inventory.server.resources.pool.category-tree.main
  (:require
   [leihs.core.core :refer [presence]]
   [leihs.core.resources.categories.filter :as filter]
   [leihs.core.resources.categories.tree :refer [tree]]
   [reitit.coercion.schema]
   [ring.middleware.accept]))

(defn- term-filter [tree request]
  (if-let [term (-> request :query-params-raw :term presence)]
    (filter/deep-filter #(re-matches (re-pattern (str "(?i).*" term ".*"))
                                     (:name %))
                        tree)
    tree))

(defn index-resources [{{:keys [pool_id]} :path-params :as request}]
  (let [tx (:tx request)
        with-metadata (or (-> request :parameters :query :with-metadata) false)
        res {:body {:name "categories"
                    :children (-> (tree tx {:with-metadata with-metadata})
                                  (term-filter request))}}]
    res))
