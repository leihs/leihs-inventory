(ns leihs.inventory.client.lib.tree)

(defn current-or-any-descendent? [pred category]
  (or (pred category)
      (some (partial current-or-any-descendent? pred)
            (:children category))))

(defn deep-filter [pred tree]
  (->> tree
       (filter (partial current-or-any-descendent? pred))
       (map #(update % :children (partial deep-filter pred)))))

(comment
  (require '[clojure.test :as t])

  (let [tree [{:name "foo"
               :children [{:name "bar"
                           :children [{:name "baz"
                                       :children []}]}]}
              {:name "qux"
               :children [{:name "quux"
                           :children [{:name "foo"
                                       :children []}]}]}]
        term "oo"
        result (deep-filter #(re-matches (re-pattern (str "(?i).*" term ".*"))
                                         (:name %))
                            tree)]
    ; result
    (t/is (= result [{:name "foo",
                      :children []}
                     {:name "qux",
                      :children [{:name "quux",
                                  :children [{:name "foo",
                                              :children []}]}]}]))))

(defn convert-tree-path
  "Converts a tree path represented as a map into a tree path
  represented as a vector (list of ancestors incl. self).
  Example:
  ;; (convert-tree-path {:id 1 :children [{:id 2 :children [{:id 3}]}]})
  ;; => [{:id 1} {:id 2} {:id 3}]"
  [node]
  (letfn [(tree-path-h [node result]
            (if (empty? (:children node))
              (conj result node)
              (tree-path-h (first (:children node))
                           (conj result (dissoc node :children)))))]
    (tree-path-h node [])))

(defn get-path [id tree]
  (let [parent-paths (deep-filter #(= (:category_id %) id)
                                  (:children tree))]
    (mapv convert-tree-path parent-paths)))
