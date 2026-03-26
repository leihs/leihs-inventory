(ns leihs.inventory.client.provider.visibility-provider
  (:require
   ["react" :refer [createContext useContext useState useEffect useCallback useMemo]]
   ["react-hook-form" :refer [useWatch]]
   [uix.core :refer [defui defhook $]]))

;; Context definition
(def visibility-context (createContext nil))

(defn- has-value?
  "Check if a value is considered 'truthy' for dependency purposes.
   Returns false for: nil, empty string, empty array, empty object, false"
  [val]
  (cond
    (nil? val) false
    (boolean? val) val
    (string? val) (not= val "")
    (array? val) (pos? (.-length val))
    (object? val) (if (js/Object.hasOwn val "value")
                    ;; For objects like {:value "..." :label "..."}, check the value property
                    (has-value? (.-value val))
                    ;; For plain objects without value property, check if they have keys
                    (pos? (count (js/Object.keys val))))
    :else true))

(defui VisibilityProvider
  "Provider component that manages field visibility and registration.
   Wraps the form to provide visibility checking for all field dispatchers.
   
   Usage:
     ($ VisibilityProvider {:form form}
        ($ :form
           ($ FieldDispatcher {:block block1})
           ($ FieldDispatcher {:block block2})))"
  [{:keys [form children]}]
  (let [;; Pure React state - set of field names currently rendered
        [rendered-fields set-rendered-fields] (useState #{})

        ;; Function to register a field
        register-field (useCallback
                        (fn [field-name]
                          (set-rendered-fields
                           (fn [prev] (conj prev field-name))))
                        #js [])

        ;; Function to unregister a field
        unregister-field (useCallback
                          (fn [field-name]
                            (set-rendered-fields
                             (fn [prev] (disj prev field-name))))
                          #js [])

        ctx-value {:form form
                   :rendered-fields rendered-fields
                   :register-field register-field
                   :unregister-field unregister-field}]

    ($ (.-Provider visibility-context)
       {:value ctx-value}
       children)))

(defn use-field-visibility
  "Centralized visibility checking for a field block.
   Handles all visibility logic including:
   - Visibility dependencies (value-based and presence-based)
   - Values dependencies (has-value check)
   - Field registration for presence-based visibility
   
   Takes: block - Field configuration with :visibility-dependency and :values-dependency
   
   Returns: {:is-visible boolean
             :values-dependency map or nil
             :watched-dependency-value any (for values-url construction)}"
  [block]
  (let [ctx (useContext visibility-context)
        _ (when-not ctx
            (js/console.warn "use-field-visibility must be used within VisibilityProvider"))
        {:keys [form rendered-fields register-field unregister-field]} ctx

        control (.-control form)
        visibility (:visibility-dependency block)
        values-dep (:values-dependency block)

        ;; Always call useWatch (hooks must be called unconditionally)
        watched-visibility (useWatch #js {:control control
                                          :name (:field visibility)})

        watched-dependency (useWatch #js {:control control
                                          :name (:field values-dep)})

        ;; Check visibility
        is-visible-check (cond
                          ;; Pattern 1: Check if value matches
                           (contains? visibility :value)
                           (= (str watched-visibility) (str (:value visibility)))

                          ;; Pattern 2: Check if field is rendered (via registry)
                           (and (contains? visibility :field)
                                (not (contains? visibility :value)))
                           (contains? rendered-fields (:field visibility))

                          ;; Default: always visible
                           :else
                           true)

        ;; Check if field should show based on values dependency
        has-dependency-value (if values-dep
                               (has-value? watched-dependency)
                               true)

        ;; Final visibility combines both checks
        is-visible (and is-visible-check has-dependency-value)]

    ;; Register this field when visible
    (useEffect
     (fn []
       (when is-visible
         (register-field (:name block)))

       ;; Cleanup: unregister when unmounting or becoming invisible
       (fn []
         (unregister-field (:name block))))
     #js [is-visible (:name block)])

    {:is-visible is-visible
     :values-dependency values-dep
     :watched-dependency-value watched-dependency}))
