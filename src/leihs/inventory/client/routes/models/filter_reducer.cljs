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
                                               (into [] (remove (set disable) state))
                                               (into state filter))]

                               (update-search-params {:update-param {:param filter
                                                                     :value value
                                                                     :delete (:delete action)}})
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

        [state dispatch] (uix/use-reducer filter-reducer nil create-initial-state)
        prev-state (uix/use-ref nil)]

    ;; (uix/use-effect
    ;;  (fn []
    ;;    (let [prev-state @prev-state]
    ;;      (js/console.debug prev-state state)
    ;;      ;; Check if `state` changed
    ;;      (when (not= prev-state state)
    ;;        (doseq [[filter is-hidden] (:hidden state)]
    ;;          (when (and is-hidden
    ;;                     (not= (name filter) (:filter state)))
    ;;            (.delete search-params (name filter))))
    ;;
    ;;        (if (not= (:value state) nil)
    ;;          (.set search-params (:filter state) (:value state))
    ;;          (.delete search-params (:filter state)))
    ;;
    ;;        (.set search-params "page" "1")
    ;;        (set-search-params! search-params)))
    ;;
    ;;    ;; Update the ref with the current dependencies
    ;;    (reset! prev-state state))
    ;;  [state search-params set-search-params!])

;; Provide the state and dispatcher to the context
    ;; (uix/use-effect
    ;;  (fn []
    ;;    (update-search-params {:remove-params [:page]
    ;;                           :update-param {:param "type"
    ;;                                          :value (.get search-params "type")
    ;;                                          :delete true}}))
    ;;  [])

    ($ ctx
       {:value state}
       ($ dispatcher
          {:value dispatch}
          children))))

(defn use-filter-state []
  (uix/use-context ctx))

(defn use-filter-dispatcher []
  (uix/use-context dispatcher))
