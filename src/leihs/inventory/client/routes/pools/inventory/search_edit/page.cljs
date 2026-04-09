(ns leihs.inventory.client.routes.pools.inventory.search-edit.page
  (:require
   ["@@/button" :refer [Button]]
   ["@@/card" :refer [Card CardContent CardHeader CardTitle CardFooter]]
   ["@@/form" :refer [Form]]
   ["@hookform/resolvers/zod" :refer [zodResolver]]
   ["react-hook-form" :refer [useForm useWatch]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :refer [useFetcher useLoaderData useParams
                               useSearchParams]]
   [leihs.inventory.client.components.pagination :as pagination]
   [leihs.inventory.client.components.typo :refer [Typo]]
   [leihs.inventory.client.lib.dynamic-form :as dynamic-form]
   [leihs.inventory.client.lib.hooks :as hooks]
   [leihs.inventory.client.lib.utils :refer [jc cj]]
   [leihs.inventory.client.routes.pools.inventory.search-edit.components.and-filters :refer [AndFilters]]
   [leihs.inventory.client.routes.pools.inventory.search-edit.components.edit-dialog :refer [EditDialog]]
   [leihs.inventory.client.routes.pools.inventory.search-edit.components.items-table :refer [ItemsTable]]
   [leihs.inventory.client.routes.pools.inventory.search-edit.components.or-filters :refer [OrFilters]]
   [leihs.inventory.client.routes.pools.inventory.search-edit.defaults :as defaults]
   [leihs.inventory.client.routes.pools.inventory.search-edit.schema :refer [search-edit-schema]]
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
        query (jc (js/JSON.parse (.get search-params "filter_d")))

        form-ref (uix/use-ref nil)

        {:keys [pool-id]} (jc (useParams))
        [selected-items set-selected-items!] (uix/use-state #{})
        [edit-open? set-edit-open!] (uix/use-state false)
        fields (:fields data)
        structure (dynamic-form/fields->structure fields {:group-order groups})
        defaults (dynamic-form/fields->defaults fields)

        item-list (:data items)
        pagination (:pagination items)

        blocks (->> structure
                    (mapcat :blocks)
                    ;; overwrite default values for specific fields, otherwise use the default from structure
                    (map #(merge {:value (case (:component %)
                                           "autocomplete" nil
                                           "autocomplete-search" nil
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
                                   "input"
                                   {:allowed-operators ["$ilike"]}
                                   "autocomplete"
                                   {:allowed-operators ["$eq"]}
                                   "autocomplete-search"
                                   {:allowed-operators ["$eq"]}
                                   {:allowed-operators ["$eq"]})
                                 %))

                    ;; Remove fields that are not relevant for search (e.g., building_id and attachments)
                    (remove #(#{"building_id" "attachments"} (:name %)))
                    vec)

        form (useForm #js {:resolver (zodResolver search-edit-schema)
                           :defaultValues (if query
                                            (fn [] (defaults/query->form-structure query blocks pool-id))
                                            nil)})

        control (.-control form)

        watch (useWatch #js {:control control
                             :name "$or"})

        debounced-watch (hooks/use-debounce watch 300)

        handle-submit (.. form -handleSubmit)

        on-submit (uix/use-callback
                   (fn [data]
                     ;; Clear existing query params and reset to page 1 on new filter
                     (let [prev-query (js/JSON.stringify (js/JSON.parse (.get search-params "filter_d")))
                           next-query (js/JSON.stringify data)]

                       (when (= (count ^js (.-$or data)) 0)
                         (.delete search-params "filter_d")
                         (set-search-params! search-params))

                        ;; Only update search params if the query has actually changed, to not interfere with pagination
                       (when (not= prev-query next-query)
                         (.set search-params "page" "1")
                         (.set search-params "size" "50")

                         ;; Add the stringified query
                         (.set search-params "filter_d" next-query)
                         (set-search-params! search-params))))

                   [set-search-params! search-params])

        on-invalid (uix/use-callback
                    (fn [errors]
                      (js/console.warn "Form validation failed with errors:" (clj->js errors)))
                    [])]

    (uix/use-effect
     (fn []
       (handle-submit on-submit on-invalid))
     [debounced-watch on-submit on-invalid handle-submit])

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

             ($ Typo {:variant "caption"
                      :as-child true
                      :class-name "w-80"}
                ($ :p (t "pool.models.search_edit.page.description")))))

       ;; Search Results Section
       (when items
         ($ Card {:class-name "mt-4"}
            ($ CardHeader {:class-name "flex flex-row items-center justify-between"}
               ($ CardTitle (t "pool.models.search_edit.page.search_results"))

               ;; Bulk action buttons - show when items selected
               ($ :div {:class-name "flex gap-2"}
                  ($ Button {:disabled (empty? selected-items)
                             :on-click #(set-edit-open! true)
                             :class-name "disabled:hover:bg-primary"}
                     (t "pool.models.search_edit.page.edit_items" #js {:count (count selected-items)}))
                  ($ Button {:disabled (empty? selected-items)
                             :class-name "disabled:hover:bg-primary"}
                     (t "pool.models.search_edit.page.export_items" #js {:count (count selected-items)}))))

            ($ CardContent
               ($ ItemsTable {:items item-list
                              :on-selection-change (fn [selected-ids]
                                                     (set-selected-items! selected-ids))}))

            ($ CardFooter {:class-name "sticky bottom-0 bg-background z-10 rounded-b-xl  pt-6"
                           :style {:background "linear-gradient(to top, hsl(var(--background)) 80%, transparent 100%)"}}
               ($ pagination/main {:pagination pagination
                                   :class-name "justify-start w-full"}))))

       ($ EditDialog {:open? edit-open?
                      :on-open-change set-edit-open!
                      :selected-items selected-items
                      :blocks blocks}))))
