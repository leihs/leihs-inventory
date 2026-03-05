(ns leihs.inventory.client.routes.pools.inventory.search-edit.defaults
  (:require
   [leihs.inventory.client.lib.client :refer [http-client]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [promesa.core :as p]))

(defn- fetch-label
  "Fetches the display label for an autocomplete-search field value via API.
   Returns a Promise resolving to a string label, or nil on failure."
  [pool-id field-name value]
  (case field-name
    "model_id"
    (-> http-client
        (.get (str "/inventory/" pool-id "/models/" value))
        (.then (fn [res] (:product (jc (.-data res)))))
        (.catch (fn [_] nil)))

    "room_id"
    (-> http-client
        (.get (str "/inventory/" pool-id "/rooms/" value))
        (.then (fn [res] (:name (jc (.-data res)))))
        (.catch (fn [_] nil)))

    (p/resolved nil)))

(defn- get-labels
  "Returns enriched field map (or a Promise of one) for a given value and field-name."
  [value field-name blocks pool-id]
  (let [value (str value)
        field (dissoc
               (into {} (filter #(= (:name %) field-name) blocks))
               :label)

        ;; For select and autocomplete, find the label for the current value from the field's options
        label (->> blocks
                   (some #(when (= (:name %) field-name) %))
                   :props
                   :options
                   (some #(when (= (:value %) value) %))
                   :label)]

    (case (:component field)
      "select"
      (assoc field :value value)

      "autocomplete"
      (assoc field :value {:value value :label label})

      ;; For autocomplete-search, we need to fetch the label from the API based on the value (ID)
      "autocomplete-search"
      (-> (fetch-label pool-id field-name value)
          (.then (fn [resolved-label]
                   (assoc field :value {:value value :label resolved-label}))))

      (assoc field :value value))))

(defn- resolve-structure
  "Resolves all potentially-Promise entries in the query->form-structure output.
   Returns a Promise of the fully resolved structure as a ClojureScript map."
  [structure]
  (p/let [or-groups
          (p/all
           (mapv (fn [or-group]
                   (p/let [and-entries (p/all (:$and or-group))]
                     (assoc or-group :$and and-entries)))
                 (:$or structure)))]
    {:$or or-groups}))

(defn query->form-structure
  "Converts simplified query format to React Hook Form structure.

   Input:  {:$or [{:$and [{:model_id {:$eq 'uuid'}}]}]}
   Output: {:$or [{:id 'uuid' :$and [{:id 'uuid' :name 'model_id' :operator '$eq' :value 'uuid'}]}]}"
  [query blocks pool-id]
  (let [transform (fn [input]
                    (let [[field-kw condition] (first input)
                          field-name (name field-kw)
                          value (-> condition vals first)
                          base-cond {:id (str (random-uuid))
                                     :operator (-> condition keys first name)}
                          labels (get-labels value field-name blocks pool-id)]
                      (if (instance? js/Promise labels)
                        (.then labels #(merge base-cond %))
                        (merge base-cond labels))))]

    (-> {:$or (mapv (fn [group]
                      {:id (str (random-uuid))
                       :$and (mapv transform (:$and group))})
                    (:$or query))}
        resolve-structure
        (.then cj))))
