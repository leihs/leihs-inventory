(ns leihs.inventory.client.routes.pools.inventory.search-edit.page
  (:require
   ["@@/badge" :refer [Badge]]
   ["@@/button" :refer [Button]]
   ["@@/card" :refer [Card CardContent CardFooter CardHeader CardTitle]]
   ["@@/form" :refer [Form]]
   ["@@/spinner" :refer [Spinner]]
   ["@hookform/resolvers/zod" :refer [zodResolver]]
   ["lucide-react" :refer [SquarePen]]
   ["react-hook-form" :refer [useForm useWatch]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :refer [useLoaderData useParams useSearchParams]]
   ["sonner" :refer [toast]]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [leihs.inventory.client.components.export :refer [Export]]
   [leihs.inventory.client.components.items-table :refer [ItemsTable]]
   [leihs.inventory.client.components.pagination :as pagination]
   [leihs.inventory.client.components.typo :refer [Typo]]
   [leihs.inventory.client.lib.client :refer [http-client]]
   [leihs.inventory.client.lib.dynamic-form :as dynamic-form]
   [leihs.inventory.client.lib.hooks :as hooks]
   [leihs.inventory.client.lib.utils :refer [jc cj]]
   [leihs.inventory.client.routes.pools.inventory.search-edit.components.edit-dialog :refer [EditDialog]]
   [leihs.inventory.client.routes.pools.inventory.search-edit.components.filters.and-filters :refer [AndFilters]]
   [leihs.inventory.client.routes.pools.inventory.search-edit.components.filters.or-filters :refer [OrFilters]]
   [leihs.inventory.client.routes.pools.inventory.search-edit.defaults :as defaults]
   [leihs.inventory.client.routes.pools.inventory.search-edit.schema :refer [search-edit-schema]]
   [promesa.core :as p]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(def groups ["Mandatory data"
             "Status"
             "Inventory"
             "Eigenschaften"
             "General Information"
             "Location"
             "Invoice Information"])

(defui page []
  (let [[t] (useTranslation)
        {:keys [data items]} (useLoaderData)
        [search-params set-search-params!] (useSearchParams)
        query (jc (edn/read-string (.get search-params "filter_q")))

        form-ref (uix/use-ref nil)
        prev-filter-ref (uix/use-ref
                         (str (edn/read-string (.get search-params "filter_q"))))

        {:keys [pool-id]} (jc (useParams))
        [selected-items set-selected-items!] (uix/use-state #{})
        [edit-open? set-edit-open!] (uix/use-state false)
        [edit-loading? set-edit-loading!] (uix/use-state false)
        [protected-fields set-protected-fields!] (uix/use-state #{})
        fields (:fields data)
        structure (-> (dynamic-form/fields->structure fields {:group-order groups})
                      (dynamic-form/patch "price" {:component "price"}))
        defaults (dynamic-form/fields->defaults fields)

        item-list (:data items)
        pagination (:pagination items)

        blocks (->> structure
                    (mapcat :blocks)
                    (map #(if (and (:label %) (not (str/starts-with? (:name %) "properties_")))
                            (update % :label t)
                            %))
                    ;; overwrite default values for specific fields, otherwise use the default from structure
                    (sort #(.localeCompare (:label %1) (:label %2)))
                    (map #(merge {:value (case (:component %)
                                           "autocomplete" nil
                                           "autocomplete-search" nil
                                           "price" ""
                                           "input" ""
                                           "calendar" (js/Date.)
                                           ((keyword (:name %)) defaults))}
                                 %))

                    ;; Add operator and allowed-operators based on component type
                    (map #(merge (case (:component %)
                                   "calendar"
                                   {:allowed-operators ["$eq" "$gte" "$lte"]}
                                   "checkbox"
                                   {:allowed-operators ["$eq"]}
                                   "radio-group"
                                   {:allowed-operators ["$eq"]}
                                   "select"
                                   {:allowed-operators ["$eq"]}
                                   "textarea"
                                   {:allowed-operators ["$ilike"]}
                                   "price"
                                   {:allowed-operators ["$eq" "$gte" "$lte"]}
                                   "input"
                                   {:allowed-operators ["$ilike"]}
                                   "autocomplete"
                                   {:allowed-operators ["$eq"]}
                                   "autocomplete-search"
                                   {:allowed-operators ["$eq"]}
                                   {:allowed-operators ["$eq"]})
                                 %))

                    ;; Remove fields that are not relevant for search (e.g., building_id and attachments)
                    (remove #(#{"attachments"} (:name %)))
                    vec)

        form (useForm #js {:resolver (zodResolver search-edit-schema)
                           :defaultValues (if query
                                            (fn [] (defaults/query->form-structure query blocks pool-id))
                                            nil)})

        control (.-control form)

        watch (useWatch #js {:control control
                             :name "$or"})

        debounced-watch (hooks/use-debounce watch 300)

        selected-item-objects (filter #(contains? selected-items (:id %)) item-list)

         ;; One representative item per unique non-owned pool
        non-owned-items
        (->> selected-item-objects
             (filter #(not= (:owner_id %) pool-id))
             (group-by :owner_id)
             (vals)
             (map first))

        editable-blocks (remove #(contains? protected-fields (:name %)) blocks)

        export-url (fn []
                     (let [base-url (str "/inventory/" pool-id "/items/")]
                       (if (empty? selected-items)
                         (str base-url "?" search-params)
                         (str base-url "?" (->> selected-items
                                                (map #(str "ids=" %))
                                                (str/join "&"))))))

        handle-edit
        (fn []
          (if (empty? non-owned-items)
            (do (set-protected-fields! #{})
                (set-edit-open! true))
            (do
              (set-edit-loading! true)
              (let [fetches (map (fn [item]
                                   (-> http-client
                                       (.get (str "/inventory/" pool-id "/fields/")
                                             (clj->js {:params {:target_type "item"
                                                                :resource_id (str (:id item))}}))))
                                 non-owned-items)]
                (-> (p/let [responses (p/all fetches)
                            all-fields (->> responses
                                            (mapcat #(jc (.. ^js % -data -fields)))
                                            (filter :protected)
                                            (map :id)
                                            set)]
                      (set-protected-fields! all-fields)
                      (set-edit-loading! false)
                      (set-edit-open! true))
                    (p/catch (fn [_err]
                               (set-edit-loading! false)
                               (.. toast (error (t "pool.models.search_edit.dialog.fields_fetch_error"))))))))))

        handle-submit (.. form -handleSubmit)

        on-submit (uix/use-callback
                   (fn [data]
                     (let [next-query (str (jc data))
                           no-filters? (= (count ^js (.-$or data)) 0)]

                       (set-selected-items! #{})

                       (when (not= @prev-filter-ref next-query)
                         (reset! prev-filter-ref next-query)
                         (set-search-params!
                          (fn [search-params]
                            (if no-filters?
                              (do (.delete search-params "filter_q") search-params)
                              (do (.set search-params "page" "1")
                                  (.set search-params "size" "50")
                                  (.set search-params "filter_q" (str (jc data)))
                                  search-params)))))))
                   [set-search-params!])

        on-invalid (uix/use-callback
                    (fn [errors]
                      (js/console.warn "Form validation failed with errors:" (clj->js errors)))
                    [])]

    (uix/use-effect
     (fn []
       (when (.. form -formState -isReady)
         (handle-submit on-submit on-invalid)))
     [debounced-watch on-submit on-invalid handle-submit form])

    ($ :<>
       ($ Card {:class-name "my-4"}
          ($ CardHeader
             ($ CardTitle (t "pool.models.search_edit.page.title")))
          ($ CardContent {:class-name "flex flex-col space-y-4 lg:flex-row lg:space-y-0"}

             ($ Form (merge form)
                ($ :form {:id "search-edit-form"
                          :ref form-ref
                          :className " space-y-2 w-full mr-12"
                          :no-validate true
                          :on-submit (handle-submit on-submit on-invalid)}

                   ($ OrFilters {:blocks blocks
                                 :form form}
                      ($ AndFilters))))

             ($ :div {:class-name "flex flex-col"}
                ($ Typo {:variant "caption"
                         :as-child true
                         :class-name "w-80"}
                   ($ :p (t "pool.models.search_edit.page.description")))

                ($ Typo {:variant "caption"
                         :as-child true
                         :class-name "w-80 mt-4"}
                   ($ :p (t "pool.models.search_edit.page.selection_info"))))))

       ;; Search Results Section
       (when items
         ($ Card {:class-name "mt-4"}
            ($ CardHeader {:class-name "flex flex-row items-center justify-between"}
               ($ CardTitle (t "pool.models.search_edit.page.search_results"))

               ;; Bulk action buttons - show when items selected
               ($ :div {:class-name "flex gap-2"}
                  ($ Export {:url (export-url)
                             :count (str (if (empty? selected-items)
                                           (:total_rows pagination)
                                           (count selected-items)))})

                  ($ Button {:data-test-id "edit-button"
                             :disabled (or (empty? selected-items) edit-loading?)
                             :on-click handle-edit
                             :class-name "disabled:hover:bg-primary"}
                     (if edit-loading?
                       ($ Spinner {:class-name "w-4 h-4"})
                       ($ SquarePen {:class-name "w-4 h-4"}))
                     (t "pool.models.search_edit.page.edit_items")
                     ($ Badge {:variant "primary"
                               :class-name "ml-2 rounded-full"}
                        (str (count selected-items))))))

            ($ CardContent
               ($ ItemsTable {:items item-list
                              :selected selected-items
                              :on-selection-change (fn [selected-ids]
                                                     (set-selected-items! selected-ids))}))

            ($ CardFooter {:class-name "sticky bottom-0 bg-background z-10 rounded-b-xl pt-6"
                           :style {:background "linear-gradient(to top, hsl(var(--background)) 80%, transparent 100%)"}}
               ($ pagination/main {:pagination pagination
                                   :class-name "justify-start w-full"}))))

       ($ EditDialog {:open? edit-open?
                      :on-open-change set-edit-open!
                      :selected-items selected-item-objects
                      :blocks editable-blocks
                      :some-fields-restricted? (boolean (seq protected-fields))}))))
