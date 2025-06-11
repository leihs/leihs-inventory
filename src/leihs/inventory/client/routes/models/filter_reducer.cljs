(ns leihs.inventory.client.routes.models.filter-reducer
  (:require
   ["react-router-dom" :as router]
   [uix.core :as uix :refer [$ defui]]))

(def ctx (uix/create-context))
(def dispatcher (uix/create-context))

(defui FilterProvider [{:keys [children]}]
  (let [[search-params set-search-params!] (router/useSearchParams)

        update-search-params (fn [enabled-filter last-filter value]
                               (doseq [filter enabled-filter]
                                 (when (not= (name filter) last-filter)
                                   (.delete search-params (name filter))))

                               (if (not= value nil)
                                 (.set search-params last-filter value)
                                 (.delete search-params last-filter))

                               (.set search-params "page" "1")
                               (set-search-params! search-params))

        filter-reducer (fn [state action]
                         (let [filter (:filter action)
                               value (:value action)]

                           (cond
                           ;; reset filters
                             #_(= (:delete action) nil)
                             #_(let [new-state []]

                                 (update-search-params new-state filter value)
                                 new-state)

                             (= (:filter action) "type")
                             (when (= (:value action) "option")
                               (let [disabled-filter [:inventory_pool_id
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
                                                 (do
                                                   (js/console.debug "deleting" action state disabled-filter)
                                                   (into [] (remove (set disabled-filter) state)))
                                                 (into state disabled-filter))]

                                 (js/console.debug new-state)

                                 (update-search-params new-state filter value)
                                 new-state))

                             (= (:value action) "model")
                             (let [new-state []]

                               (update-search-params new-state filter value)
                               new-state)

                             (= (:value action) "package")
                             (let [new-state []]

                               (update-search-params new-state filter value)
                               new-state)

                             (= (:value action) "software")
                             (let [new-state [:broken
                                              :incomplete
                                              :category_id
                                              :before_last_check]]

                               (update-search-params new-state filter value)
                               new-state)

                             (or (= (:filter action) "incomplete")
                                 (= (:filter action) "broken"))
                             (let [new-state [:option
                                              :model]]

                               (update-search-params new-state filter value)
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

                                   (or (= broken "true")
                                       (= incomplete "true")
                                       (= type "software"))
                                   (conj :model)

                                   (or (= broken "true")
                                       (= incomplete "true")
                                       inventory_pool_id
                                       category_id
                                       before_last_check)
                                   (conj :option)

                                   (or category_id
                                       before_last_check)
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
