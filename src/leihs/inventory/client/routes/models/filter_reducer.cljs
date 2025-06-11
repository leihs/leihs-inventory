(ns leihs.inventory.client.routes.models.filter-reducer
  (:require
   ["react-router-dom" :as router]
   [uix.core :as uix :refer [$ defui]]))

(def initial-state
  {:filter nil
   :value nil
   :hidden {:type false
            :model false
            :option false
            :software false
            :package false
            :inventory_pool_id false
            :before_last_check false
            :category_id false
            :borrowable false
            :retired false
            :with_items false
            :status false
            :owned false
            :in_stock false
            :incomplete false
            :broken false}})

(def ctx (uix/create-context))
(def dispatcher (uix/create-context))

(defui FilterProvider [{:keys [children]}]
  (let [[search-params set-search-params!] (router/useSearchParams)

        update-search-params (fn [state]
                               (doseq [[filter is-hidden] (:hidden state)]
                                 (when (and is-hidden
                                            (not= (name filter) (:filter state)))
                                   (.delete search-params (name filter))))

                               (if (not= (:value state) nil)
                                 (.set search-params (:filter state) (:value state))
                                 (.delete search-params (:filter state)))

                               (.set search-params "page" "1")
                               (set-search-params! search-params))

        filter-reducer (fn [state action]
                         (cond
                           ;; reset filters
                           (= (:value action) nil)
                           (let [new-state (assoc initial-state
                                                  :filter (:filter action)
                                                  :value nil)]
                             (update-search-params new-state)
                             new-state)

                           (= (:value action) "option")
                           (let [new-state (assoc initial-state
                                                  :filter (:filter action)
                                                  :value (:value action)
                                                  :hidden {:inventory_pool_id true
                                                           :category_id true
                                                           :before_last_check true
                                                           :borrowable true
                                                           :retired true
                                                           :with_items true
                                                           :status true
                                                           :broken true
                                                           :incomplete true
                                                           :owned true
                                                           :in_stock true})]

                             (update-search-params new-state)
                             new-state)

                           (= (:value action) "model")
                           (let [new-state (assoc initial-state
                                                  :filter (:filter action)
                                                  :value (:value action))]

                             (update-search-params new-state)
                             new-state)

                           (= (:value action) "package")
                           (let [new-state (assoc initial-state
                                                  :filter (:filter action)
                                                  :value (:value action))]

                             (update-search-params new-state)
                             new-state)

                           (= (:value action) "software")
                           (let [new-state (assoc initial-state
                                                  :filter (:filter action)
                                                  :value (:value action)
                                                  :hidden {:broken true
                                                           :incomplete true
                                                           :category_id true
                                                           :before_last_check true})]

                             (update-search-params new-state)
                             new-state)))

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

                                 (js/console.debug (boolean inventory_pool_id))

                                 {:filter nil
                                  :value nil
                                  :hidden {:type false
                                           :model (cond
                                                    (or (= broken "true")
                                                        (= incomplete "true"))
                                                    true

                                                    :else
                                                    false)
                                           :option (cond
                                                     (or (= broken "true")
                                                         (= incomplete "true")
                                                         inventory_pool_id
                                                         category_id
                                                         before_last_check)
                                                     true

                                                     :else
                                                     false)
                                           :software (cond
                                                       (or category_id
                                                           before_last_check)
                                                       true

                                                       :else
                                                       false)
                                           :package false
                                           :inventory_pool_id (cond
                                                                (= type "option")
                                                                true

                                                                :else
                                                                false)
                                           :before_last_check (cond
                                                                (or (= type "option")
                                                                    (= type "software"))
                                                                true

                                                                :else
                                                                false)
                                           :category_id (cond
                                                          (or (= type "option")
                                                              (= type "software"))
                                                          true

                                                          :else
                                                          false)
                                           :borrowable (cond
                                                         (= type "option")
                                                         true

                                                         :else
                                                         false)
                                           :retired (cond
                                                      (= type "option")
                                                      true

                                                      :else
                                                      false)
                                           :with_items (cond
                                                         (= type "option")
                                                         true

                                                         :else
                                                         false)
                                           :status (cond
                                                     (= type "option")
                                                     true

                                                     :else
                                                     false)
                                           :owned (cond
                                                    :else
                                                    false)
                                           :in_stock (cond
                                                       :else
                                                       false)
                                           :incomplete (cond
                                                         (= type "software")
                                                         true

                                                         :else
                                                         false)
                                           :broken (cond
                                                     (= type "software")
                                                     true

                                                     :else
                                                     false)}}))

        [state dispatch] (uix/use-reducer filter-reducer nil create-initial-state)
        prev-state (uix/use-ref nil)]

    #_(uix/use-effect
       (fn []
         (let [prev-state @prev-state]
           (js/console.debug prev-state state)

         ;; Check if `state` changed
           (when (not= prev-state state)
             (doseq [[filter is-hidden] (:hidden state)]
               (when (and is-hidden
                          (not= (name filter) (:filter state)))
                 (.delete search-params (name filter))))

             (if (not= (:value state) nil)
               (.set search-params (:filter state) (:value state))
               (.delete search-params (:filter state)))

             (.set search-params "page" "1")
             (set-search-params! search-params)))

       ;; Update the ref with the current dependencies
         (reset! prev-state state))
       [state search-params set-search-params!])

    #_(uix/use-effect
       (fn []
       ;; Reset the state when the component unmounts
         (js/console.debug "Initalizing filter state")
         #_(dispatch {:filter nil
                      :value nil
                      :hidden {}}))
       [search-params])

    ($ ctx
       {:value state}
       ($ dispatcher
          {:value dispatch}
          children))))

(defn use-filter-state []
  (uix/use-context ctx))

(defn use-filter-dispatcher []
  (uix/use-context dispatcher))
