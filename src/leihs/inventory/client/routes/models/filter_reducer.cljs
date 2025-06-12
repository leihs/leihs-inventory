(ns leihs.inventory.client.routes.models.filter-reducer
  (:require
   ["react-router-dom" :as router]
   [uix.core :as uix :refer [$ defui]]))

(def ctx (uix/create-context))
(def dispatcher (uix/create-context))

(defui FilterProvider [{:keys [children]}]
  (let [[search-params set-search-params!] (router/useSearchParams)

        update-search-params (fn [disabled current-filter value & [delete?]]
                               (doseq [filter disabled]
                                 (.delete search-params (name filter)))

                               (if delete?
                                 (.delete search-params current-filter)
                                 (.set search-params current-filter value))

                               (.set search-params "page" "1")
                               (set-search-params! search-params))

        filter-reducer (fn [state action]
                         (let [filter (:filter action)
                               value (:value action)]

                           (js/console.debug filter value)
                           (cond
                           ;; reset filters
                             (:reset action)
                             (let [remove-filter [:type
                                                  :inventory_pool_id
                                                  :category_id
                                                  :before_last_check
                                                  :borrowable
                                                  :retired
                                                  :broken
                                                  :incomplete
                                                  :owned
                                                  :in_stock]]

                               (update-search-params remove-filter "with_items" true)
                               [:status])

                             (= (:filter action) "with_items")
                             (let [disable [:status]

                                   new-state (if (:delete action)
                                               (into [] (remove (set disable) state))
                                               disable)]

                               (update-search-params new-state filter value (:delete action))
                               new-state)

                             (= (:filter action) "type")
                             (cond
                               (= (:value action) "option")
                               (let [disable [:inventory_pool_id
                                              :category_id
                                              :before_last_check
                                              :borrowable
                                              :retired
                                              :with_items
                                              :status
                                              :broken
                                              :incomplete
                                              :owned
                                              :in_stock]

                                     new-state (if (:delete action)
                                                 (into [] (remove (set disable) state))
                                                 disable)]

                                 (update-search-params new-state filter value (:delete action))
                                 new-state)

                               (= (:value action) "model")
                               (let [disable []
                                     new-state (if (:delete action)
                                                 (into [] (remove (set disable) state))
                                                 disable)]

                                 (update-search-params new-state filter value (boolean (:delete action)))
                                 new-state)

                               (= (:value action) "package")
                               (let [disable []
                                     new-state (if (:delete action)
                                                 (into [] (remove (set disable) state))
                                                 disable)]

                                 (update-search-params new-state filter value (boolean (:delete action)))
                                 new-state)

                               (= (:value action) "software")
                               (let [disable [:broken
                                              :incomplete
                                              :category_id
                                              :before_last_check]

                                     new-state (if (:delete action)
                                                 (into [] (remove (set disable) state))
                                                 disable)]

                                 (update-search-params new-state filter value (boolean (:delete action)))
                                 new-state))

                             :else
                             (let [disable []
                                   new-state (if (:delete action)
                                               (into [] (remove (set disable) state))
                                               (into state filter))]

                               (update-search-params new-state filter value (boolean (:delete action)))
                               new-state))))

        create-initial-state (fn []
                               (let [type (.get search-params "type")
                                     owned (.get search-params "owned")
                                     in_stock (.get search-params "in_stock")
                                     incomplete (.get search-params "incomplete")
                                     broken (.get search-params "broken")
                                     with_items (.get search-params "with_items")
                                     retired (.get search-params "retired")
                                     inventory_pool_id (.get search-params "inventory_pool_id")
                                     category_id (.get search-params "category_id")
                                     before_last_check (.get search-params "before_last_check")
                                     borrowable (.get search-params "borrowable")]

                                 (cond-> []
                                   false
                                   (conj :type)

                                   false
                                   (conj :model)

                                   false
                                   (conj :option)

                                   false
                                   (conj :software)

                                   false
                                   (conj :package)

                                   (= type "option")
                                   (conj :inventory_pool_id)

                                   (or (= type "option")
                                       (= type "software"))
                                   (conj :before_last_check)

                                   (or (= type "option")
                                       (= type "software"))
                                   (conj :category_id)

                                   (= type "option")
                                   (conj :borrowable)

                                   (= type "option")
                                   (conj :retired)

                                   (= type "option")
                                   (conj :with_items)

                                   (= type "option")
                                   (conj :status)

                                   false
                                   (conj :owned)

                                   false
                                   (conj :in_stock)

                                   (= type "software")
                                   (conj :incomplete)

                                   (= type "software")
                                   (conj :broken))))

        [state dispatch] (uix/use-reducer filter-reducer [] create-initial-state)]

    ($ ctx
       {:value state}
       ($ dispatcher
          {:value dispatch}
          children))))

(defn use-filter-state []
  (uix/use-context ctx))

(defn use-filter-dispatcher []
  (uix/use-context dispatcher))
