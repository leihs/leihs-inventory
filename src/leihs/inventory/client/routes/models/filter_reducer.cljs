(ns leihs.inventory.client.routes.models.filter-reducer
  (:require
   ["react-router-dom" :as router]
   [uix.core :as uix :refer [$ defui]]))

(def initial-state
  {:filter nil
   :hidden {:type false
            :inventory-pool false
            :before-last-checked false
            :categories false
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

(defn filter-reducer [state action]
  (cond
    ;; reset filters
    (= (:value action) nil)
    (let [new-state (assoc initial-state
                           :filter (:filter action)
                           :value nil)]
      new-state)

    (= (:value action) "option")
    (let [new-state (assoc initial-state
                           :filter (:filter action)
                           :value (:value action)
                           :hidden {:inventory-pool true
                                    :categories true
                                    :before-last-checked true
                                    :borrowable true
                                    :retired true
                                    :with_items true
                                    :status true
                                    :broken true
                                    :incomplete true
                                    :owned true
                                    :in_stock true})]
      new-state)

    (= (:value action) "model")
    (let [new-state (assoc initial-state
                           :filter (:filter action)
                           :value (:value action))]
      new-state)

    (= (:value action) "package")
    (let [new-state (assoc initial-state
                           :filter (:filter action)
                           :value (:value action))]
      new-state)

    (= (:value action) "software")
    (let [new-state (assoc initial-state
                           :filter (:filter action)
                           :value (:value action)
                           :hidden {:broken true
                                    :incomplete true
                                    :categories true
                                    :before-last-checked true})]

      new-state)))

(defui FilterProvider [{:keys [children]}]
  (let [[state dispatch] (uix/use-reducer filter-reducer initial-state)
        [search-params set-search-params!] (router/useSearchParams)
        prev-state (uix/use-ref nil)]

    (uix/use-effect
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

    ($ ctx
       {:value state}
       ($ dispatcher
          {:value dispatch}
          children))))

(defn use-filter-state []
  (uix/use-context ctx))

(defn use-filter-dispatcher []
  (uix/use-context dispatcher))
