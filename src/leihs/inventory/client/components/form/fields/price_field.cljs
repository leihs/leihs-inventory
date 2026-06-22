(ns leihs.inventory.client.components.form.fields.price-field
  (:require
   ["@@/form" :refer [FormControl FormField FormItem FormLabel FormMessage]]
   ["@@/input" :refer [Input]]
   ["react-i18next" :refer [useTranslation]]
   [clojure.string :as str]
   [uix.core :as uix :refer [$ defui]]))

(defui PriceField [{:keys [form block class-name]}]
  (let [[_ i18n] (useTranslation)
        get-values (aget form "getValues")
        set-value! (aget form "setValue")
        field-name (:name block)
        field-value (get-values field-name)
        language (.-language i18n)

        parse-price (fn [string]
                      (let [seperator (if (str/starts-with? language "es") "," ".")
                            seperator-pos (str/index-of string seperator)
                            whole (-> (if seperator-pos (subs string 0 seperator-pos) string)
                                      (str/replace #"[^\d]" ""))
                            frac (when seperator-pos
                                   (let [digits (-> (subs string (inc seperator-pos))
                                                    (str/replace #"[^\d]" ""))]
                                     (subs digits 0 (min 2 (count digits)))))
                            parsed (js/parseFloat (str whole "." (or frac "00")))]
                        (when-not (js/isNaN parsed) parsed)))

        format-price (uix/use-callback
                      (fn [value]
                        (let [as-float (js/parseFloat (str value))]
                          (when-not (js/isNaN as-float)
                            (.. ^js i18n -services -formatter
                                (format as-float "price" language
                                        #js {:minimumFractionDigits 2
                                             :maximumFractionDigits 2})))))
                      [language i18n])

        [display-val set-display-val!] (uix/use-state "0.00")

        handle-change (fn [ev]
                        (set-display-val! (.. ev -target -value)))

        handle-blur (fn []
                      (let [parsed (parse-price display-val)
                            raw (if parsed (str parsed) "0.00")
                            formatted (or (format-price raw) "0.00")]

                        (set-value! field-name raw #js {:shouldValidate true
                                                        :shouldDirty true})
                        (set-display-val! formatted)))]

    (uix/use-effect
     (fn []
       (set-display-val! (or (format-price field-value) "0.00")))
     [language format-price field-value])

    ($ FormField {:control (.-control form)
                  :name (:name block)
                  :render #($ FormItem {:class-name (str "mt-6 " class-name)}
                              (when (:label block)
                                ($ FormLabel (:label block)
                                   (when (-> block :props :required) "*")))
                              ($ FormControl
                                 ($ Input {:type "text"
                                           :value display-val
                                           :on-change handle-change
                                           :on-blur handle-blur
                                           :name (.. ^js % -field -name)
                                           :ref (.. ^js % -field -ref)}))
                              ($ FormMessage))})))
