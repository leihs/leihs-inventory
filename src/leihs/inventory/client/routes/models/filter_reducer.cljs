(ns leihs.inventory.client.routes.models.filter-reducer
  (:require
   ["react-router-dom" :as router]
   [uix.core :as uix :refer [$ defui]]))

(def ctx (uix/create-context))
(def dispatcher (uix/create-context))

(defui main [{:keys [children]}]
  (let [search-params (js/URLSearchParams. (.. js/window -location -search))

        update-search-params (fn [{:keys [remove-params update-param]
                                   :or {remove-params []
                                        update-param {:param nil
                                                      :value nil
                                                      :delete true}}}]

                               (doseq [param remove-params]
                                 (.delete search-params (name param)))

                               (if (:delete update-param)
                                 (.delete search-params (:param update-param))
                                 (.set search-params
                                       (:param update-param)
                                       (:value update-param)))

                               (.set search-params "page" "1")

                               (let [url (str (.-pathname js/window.location)
                                              "?"
                                              (str search-params))]
                                 (.pushState js/history #js {} "" url)))

        filter-reducer (fn [state action]
                         (let [filter (:filter action)
                               value (:value action)]

                           (cond
                             ;; reset filters
                             (:reset action)
                             (let [remove-params [:type
                                                  :inventory_pool_id
                                                  :category_id
                                                  :before_last_check
                                                  :borrowable
                                                  :retired
                                                  :broken
                                                  :incomplete
                                                  :owned
                                                  :in_stock]]

                               (update-search-params {:remove-params remove-params
                                                      :update-param {:param "with_items"
                                                                     :delete true}})
                               [:status])

                             (= (:filter action) "with_items")
                             (let [disable [(when (= value true) :status)]]

                               (update-search-params {:update-param {:param filter
                                                                     :value value
                                                                     :delete (:delete action)}})

                               disable)

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

                                 (update-search-params {:update-param {:param filter
                                                                       :value value
                                                                       :delete (:delete action)}})
                                 new-state)

                               (= (:value action) "model")
                               (let [disable []
                                     new-state (if (:delete action)
                                                 (into [] (remove (set disable) state))
                                                 disable)]

                                 (update-search-params {:update-param {:param filter
                                                                       :value value
                                                                       :delete (:delete action)}})
                                 new-state)

                               (= (:value action) "package")
                               (let [disable []
                                     new-state (if (:delete action)
                                                 (into [] (remove (set disable) state))
                                                 disable)]

                                 (update-search-params {:update-param {:param filter
                                                                       :value value
                                                                       :delete (:delete action)}})
                                 new-state)

                               (= (:value action) "software")
                               (let [disable [:broken
                                              :incomplete
                                              :category_id
                                              :before_last_check]

                                     new-state (if (:delete action)
                                                 (into [] (remove (set disable) state))
                                                 disable)]

                                 (update-search-params {:update-param {:param filter
                                                                       :value value
                                                                       :delete (:delete action)}})
                                 new-state))

                             :else
                             (let [disable []
                                   new-state (if (:delete action)
                                               (into [] (remove filter state))
                                               (into state [filter]))]

                               (update-search-params {:update-param {:param filter
                                                                     :value value
                                                                     :delete (:delete action)}})
                               (js/console.debug "new state" new-state filter)
                               new-state))))

        create-initial-state (fn []
                               (let [type (.get search-params "type")
                                     with_items (.get search-params "with_items")]
                                 (cond-> []
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

                                   (or (= type "option")
                                       (= with_items "true"))
                                   (conj :status)

                                   (= type "software")
                                   (conj :incomplete)

                                   (= type "software")
                                   (conj :broken))))

        [state dispatch] (uix/use-reducer filter-reducer nil create-initial-state)]

    ($ ctx
       {:value state}
       ($ dispatcher
          {:value dispatch}
          children))))

(defn use-filter-state []
  (uix/use-context ctx))

(defn use-filter-dispatcher []
  (uix/use-context dispatcher))
