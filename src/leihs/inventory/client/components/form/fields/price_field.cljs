(ns leihs.inventory.client.components.form.fields.price-field
  (:require
   ["@@/form" :refer [FormControl FormField FormItem FormLabel FormMessage]]
   ["@@/input" :refer [Input]]
   ["react-i18next" :refer [useTranslation]]
   [uix.core :as uix :refer [$ defui]]))

(defn- format-price [value language]
  (let [n (js/parseFloat (str value))]
    (when-not (js/isNaN n)
      (.format (js/Intl.NumberFormat. language
                                      #js {:minimumFractionDigits 2
                                           :maximumFractionDigits 2})
               n))))

(defui ^:private PriceInput [{:keys [field block language class-name]}]
  (let [raw-value (.-value field)
        [display-val set-display-val!] (uix/use-state
                                        (or (format-price raw-value language)
                                            (str raw-value)
                                            ""))]

    ;; Reformat when raw value or language changes externally
    (uix/use-effect
     (fn []
       (set-display-val! (or (format-price raw-value language) (str raw-value) "")))
     [raw-value language])

    ($ FormItem {:class-name (str "mt-6 " class-name)}
       (when (:label block)
         ($ FormLabel (:label block)
            (when (-> block :props :required) "*")))
       ($ FormControl
          ($ Input (merge
                    (dissoc (:props block) :type)
                    {:type "text"
                     :value display-val
                     :on-focus (fn [_]
                                 (set-display-val! (str raw-value)))
                     :on-change #(set-display-val! (.. % -target -value))
                     :on-blur (fn [e]
                                (let [n (js/parseFloat display-val)
                                      raw (if (js/isNaN n) display-val (str n))
                                      formatted (or (format-price raw language) raw)]
                                  ^js (.onChange field raw)
                                  ^js (.onBlur field e)
                                  (set-display-val! formatted)))
                     :name (.-name field)
                     :ref ^js (.-ref field)})))
       ($ FormMessage))))

(defui PriceField [{:keys [form block class-name]}]
  (let [[_ i18n] (useTranslation)
        language (.-language i18n)]
    ($ FormField {:control (.-control form)
                  :name (:name block)
                  :render #($ PriceInput {:field (aget % "field")
                                          :block block
                                          :language language
                                          :class-name class-name})})))
