(ns leihs.inventory.client.routes.pools.inventory.search-edit.components.field-dispatcher
  (:require
   ["react-i18next" :refer [useTranslation]]
   [clojure.string :as str]
   [leihs.inventory.client.components.form.fields.attachments-field :refer [AttachmentsField]]
   [leihs.inventory.client.components.form.fields.autocomplete-field :refer [AutocompleteField]]
   [leihs.inventory.client.components.form.fields.calendar-field :refer [CalendarField]]
   [leihs.inventory.client.components.form.fields.common-field :refer [CommonField]]
   [leihs.inventory.client.components.form.fields.radio-group-field :refer [RadioGroupField]]
   [leihs.inventory.client.components.form.fields.select-field :refer [SelectField]]
   [uix.core :refer [$ defui]]))

(def translations
  {:text
   {:select "pool.items.item.fields.autocomplete.select"
    :search "pool.items.item.fields.autocomplete.search"
    :empty "pool.items.item.fields.autocomplete.empty"}})

(defui FieldDispatcher [{:keys [form block]}]
  (let [[t] (useTranslation)
        translated-block (cond-> block
                           (and (:label block)
                                (not (str/starts-with? (:name block) "properties_")))
                           (update :label t))

        label-inactive (fn [props]
                         (let [without-options (dissoc props :options)
                               text (t "pool.items.item.fields.inactive")
                               annotated (map #(if (and (boolean? (:is_active %))
                                                        (false? (:is_active %)))
                                                 (assoc % :label (str (:label %) " ( " text " )"))
                                                 %)
                                              (-> props :options))]
                           (assoc without-options :options annotated)))]

    (cond
      (-> block :component (= "attachments"))
      ($ AttachmentsField {:form form
                           :label (:label translated-block)
                           :name (:name translated-block)
                           :props (:props translated-block)})

      ;; instant search via values-url
      (-> block :component (= "autocomplete-search"))
      ($ AutocompleteField {:form form
                            :name (:name translated-block)
                            :label (:label translated-block)
                            :props (merge
                                    translations
                                    {:remap (fn [item] {:value (str (:id item))
                                                        :label (:name item)})}
                                    (:props translated-block))
                            :class-name "mt-0 flex-1"})

      (-> block :component (= "autocomplete"))
      ($ AutocompleteField {:form form
                            :name (:name translated-block)
                            :label (:label translated-block)
                            :props (merge translations
                                          {:remap (fn [item] {:value (str (:id item))
                                                              :label (:name item)})}
                                          (label-inactive (:props translated-block)))
                            :class-name "mt-0 flex-1"})

      (-> block :component (= "radio-group"))
      ($ RadioGroupField {:form form
                          :block translated-block
                          :class-name "mt-0 flex-1"})

      (-> block :component (= "select"))
      ($ SelectField {:form form
                      :block translated-block
                      :class-name "mt-0 flex-1"})

      (-> block :component (= "calendar"))
      ($ CalendarField {:form form
                        :block translated-block
                        :class-name "mt-0 flex-1"})

      ;; default case - renders a component from the component map
      :else
      ($ CommonField {:form form
                      :block translated-block
                      :class-name "mt-0 flex-1"}))))
