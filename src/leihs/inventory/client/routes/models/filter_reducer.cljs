(ns leihs.inventory.client.routes.models.filter-reducer
  (:require
   ["react-router-dom" :as router]
   [uix.core :as uix :refer [$ defui]]))

(def ctx (uix/create-context))
(def dispatcher (uix/create-context))

(defui FilterProvider [{:keys [children]}]
  (let [[search-params set-search-params!] (router/useSearchParams)

        update-search-params (fn [state current-filter value]
                               (doseq [filter state]
                                 (when (not= (name filter) current-filter)
                                   (.delete search-params (name filter))))

                               (if (not= value nil)
                                 (.set search-params current-filter value)
                                 (.delete search-params current-filter))

                               (.set search-params "page" "1")
                               (set-search-params! search-params))

        filter-reducer (fn [state action]
                         (let [filter (:filter action)
                               value (:value action)]

                           (cond
                           ;; reset filters
                             (= (:value action) nil)
                             (let [new-state []]

                               (update-search-params new-state filter value)
                               new-state)

                             (= (:value action) "option")
                             (let [new-state [:inventory_pool_id
                                              :category_id
                                              :before_last_check
                                              :borrowable
                                              :retired
                                              :with_items
                                              :status
                                              :broken
                                              :incomplete
                                              :owned
                                              :in_stock]]

                               (update-search-params new-state filter value)
                               new-state)

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

                                 (cond -> []
                                  true 
                                  (conj :type )

                                  (when
                                   (or (= broken "true")
                                       (= incomplete "true")))
                                  (conj :model )

                                  (when
                                   (or (= broken "true")
                                       (= incomplete "true")
                                       inventory_pool_id
                                       category_id
                                       before_last_check))
                                    (conj :option)

                                  (when
                                   (or category_id
                                       before_last_check)
                                    :software)

                                  :package
                                  (when
                                   (= type "option")
                                    :inventory_pool_id)

                                  (when
                                   (or (= type "option")
                                       (= type "software"))
                                    :before_last_check)

                                  (when
                                   (or (= type "option")
                                       (= type "software"))
                                    :category_id)

                                  (when
                                   (= type "option")
                                    :borrowable)

                                  (when
                                   (= type "option")
                                    :retired)

                                  (when
                                   (= type "option")
                                    :with_items)

                                  (when
                                   (= type "option")
                                    :status)

                                  :owned

                                  :in_stock

                                  (when
                                   (= type "software")
                                    :imcomplete)

                                  (when
                                   (= type "software")
                                    :broken)]
                                       )))

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
