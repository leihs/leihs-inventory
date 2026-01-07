(ns leihs.inventory.client.components.form.users
  (:require
   ["@/components/ui/command" :refer [Command CommandEmpty CommandInput
                                      CommandItem CommandList]]
   ["@/components/ui/popover" :refer [Popover PopoverContent PopoverTrigger]]
   ["@@/button" :refer [Button]]
   ["@@/form" :refer [FormDescription FormField FormItem FormMessage]]
   ["@@/label" :refer [Label]]
   ["@@/table" :refer [Table TableBody TableCell TableRow]]
   ["lucide-react" :refer [Check ChevronsUpDown Loader2Icon Trash]]
   ["react-hook-form" :as hook-form]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router :refer [useParams]]
   [leihs.core.url.query-params :as query-params]
   [leihs.inventory.client.lib.client :refer [http-client]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defn check-path-existing [id items]
  (some (fn [item]
          (= id (:id item)))
        items))

(defn find-index-from-path [id items]
  (some (fn [[idx item]]
          (when (= id (:id item))
            idx))
        (map-indexed vector items)))

(defui main [{:keys [form name props label children]}]
  (let [[t] (useTranslation)

        user-label (fn [u]
                     (str (:firstname u) " " (:lastname u)
                          (some->> (:email u) not-empty (str " - "))
                          (when-not (:account_enabled u) (str " (" (t (-> props :text :account_disabled)) ")"))))

        control (cj (.-control form))

        params (useParams)
        path (router/generatePath "/inventory/:pool-id/users" params)

        [open set-open!] (uix/use-state false)
        [width set-width!] (uix/use-state nil)
        [search set-search!] (uix/use-state "")
        [data set-data!] (uix/use-state [])

        [loading set-loading!] (uix/use-state false)

        buttonRef (uix/use-ref nil)

        {:keys [fields append remove update]} (jc (hook-form/useFieldArray
                                                   (cj {:control control
                                                        :keyName (str name "-id")
                                                        :name name})))]

    (uix/use-effect
     (fn []
       (if (< (count search) 2)
         (set-data! [])
         (let [debounce (js/setTimeout
                         (fn []
                           (set-loading! true)
                           ;; Fetch result based on the search term
                           (-> http-client
                               (.get (str path "/" "?account_enabled=true&search=" (js/encodeURIComponent search)))
                               (.then (fn [res]
                                        (let [data (jc (.-data res))]
                                          (set-loading! false)
                                          (set-data! data))))
                               (.catch
                                (fn [err]
                                  (js/console.error "Error fetching result" err)))))
                         200)]

           (set-loading! false)
           (fn [] (js/clearTimeout debounce)))))
     [search props path name])

    (uix/use-effect
     (fn []
       (when (.. buttonRef -current)
         (set-width! (.. buttonRef -current -offsetWidth))))
     [])

    ($ :div {:class-name "flex flex-col gap-2"}
       ($ FormField {:control control
                     :name name
                     :render #($ FormItem
                                 ($ Label (t label) (when (:required props) "*"))
                                 ($ Popover {:open open
                                             :on-open-change (fn [val]
                                                               (set-search! "")
                                                               (set-data! [])
                                                               (set-open! val))}

                                    ($ PopoverTrigger {:as-child true}
                                       ($ Button {:variant "outline"
                                                  :role "combobox"
                                                  :ref buttonRef
                                                  :on-click (fn [] (set-open! (not open)))
                                                  :class-name "w-full justify-between"}
                                          (t (-> props :text :select))
                                          ($ ChevronsUpDown {:class-name "ml-2 h-4 w-4 shrink-0 opacity-50"})))

                                    ($ PopoverContent {:class-name "p-0"
                                                       :style {:width (str width "px")}}

                                       ($ Command {:should-filter false
                                                   :on-change (fn [event] (set-search! (.. event -target -value)))}
                                          ($ :div
                                             ($ CommandInput
                                                {:placeholder (t (-> props :text :placeholder))})
                                             (when loading
                                               ($ Loader2Icon {:className "absolute right-0 top-0 h-4 w-4 m-3 animate-spin opacity-50"})))
                                          ($ CommandList {:data-test-id "users-list"}

                                             ($ CommandEmpty (cond
                                                               loading
                                                               (t (-> props :text :searching))

                                                               (< (count search) 2)
                                                               (t (-> props :text :search_empty))

                                                               :else
                                                               (t (-> props :text :not_found))))

                                             (for [element data]
                                               ($ CommandItem {:key (:id element)
                                                               :value (user-label element)
                                                               :on-select (fn []
                                                                            (set-open! false)
                                                                            (if
                                                                             (not (check-path-existing (:id element) fields))
                                                                              (append (cj (merge {:firstname (:firstname element)
                                                                                                  :lastname (:lastname element)
                                                                                                  :email (:email element)
                                                                                                  :url (:url element)
                                                                                                  :id (:id element)}
                                                                                                 (into {}
                                                                                                       (map (fn [attr]
                                                                                                              (when-let [value (get element (keyword attr))]
                                                                                                                [(keyword attr) value]))
                                                                                                            (:attributes props))))))
                                                                              (remove (find-index-from-path (:id element) fields))))}

                                                  ($ Check
                                                     {:class-name (str "mr-2 h-4 w-4 "
                                                                       (if (check-path-existing (:id element) fields)
                                                                         "visible"
                                                                         "invisible"))})
                                                  ($ :span
                                                     {:class-name (str (when (= 1 (:level element)) " font-bold ")
                                                                       (when (= 2 (:level element)) " font-medium ")
                                                                       " truncate")}
                                                     (user-label element))))))))
                                 ($ FormDescription)
                                 ($ FormMessage))})

       (when (not-empty fields)
         ($ :div {:class-name "rounded-md border overflow-hidden"}
            ($ Table {:class-name "w-full"}

               ($ TableBody
                  (doall
                   (map-indexed
                    (fn [index field]
                      ($ TableRow {:class-name ""
                                   :key index}

                         ($ TableCell {:class-name "pl-4"}
                            (user-label field))

                         (when children ($ :<> (children update index field)))

                         ($ TableCell {:class-name "text-right w-0"}
                            ($ Button {:variant "outline"
                                       :type "button"
                                       :on-click #(remove index)
                                       :size "icon"}
                               ($ Trash {:class-name "h-4 w-4"})))))
                    fields)))))))))

(def Users
  (uix/as-react
   (fn [props]
     (main props))))
