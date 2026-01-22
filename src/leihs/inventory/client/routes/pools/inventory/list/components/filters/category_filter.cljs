(ns leihs.inventory.client.routes.pools.inventory.list.components.filters.category-filter
  (:require
   ["@@/button" :refer [Button]]
   ["@@/collapsible" :refer [Collapsible CollapsibleContent CollapsibleTrigger]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuContent
                               DropdownMenuSeparator DropdownMenuTrigger]]
   ["@@/radio-group" :refer [RadioGroup RadioGroupItem]]
   ["lucide-react" :refer [ChevronDown ChevronRight List Search]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router]
   [clojure.string :as str]
   [uix.core :as uix :refer [$ defui]]))

(defn find-category-name [tree-list category-id]
  (when tree-list
    (some (fn [node]
            (if (= (:category_id node) category-id)
              (:name node)
              (find-category-name (:children node) category-id)))
          tree-list)))

(defui file-tree-item [{:keys [name category_id searching on-select children]}]
  (let [[search-params _] (router/useSearchParams)]

    (if (not (seq children))
      ($ :div {:class-name "relative h-full"}
         ($ :div {:class-name "absolute right-[-25px] flex items-center h-full"}
            ($ RadioGroupItem {:data-test-id category_id
                               :checked (= category_id (.. search-params (get "category_id")))
                               :value category_id
                               :name category_id
                               :on-click on-select}))

         ($ :span {:class-name (str "flex text-sm items-center py-1 " (if searching "pl-4" "pl-10"))} name))

      ($ Collapsible {:class-name "pl-4"}
         ($ :div {:class-name "relative h-full"}
            ($ :div {:class-name "absolute right-[-25px] flex items-center h-full"}
               ($ RadioGroupItem {:data-test-id category_id
                                  :checked (= category_id (.. search-params (get "category_id")))
                                  :value category_id
                                  :name category_id
                                  :on-click on-select}))
            ($ CollapsibleTrigger {:class-name "text-sm w-full group flex items-center gap-2 py-1"}
               ($ ChevronRight {:class-name "h-4 w-4 group-data-[state=open]:rotate-90 transition-transform"})
               ($ :span {:class-name "flex items-center gap-2 text-left"} name)))

         ($ CollapsibleContent
            (for [child children]
              ($ file-tree-item (merge {:key (:category_id child)
                                        :on-select on-select}
                                       child))))))))

(defn distinct-by [key-fn coll]
  (let [seen (atom #{})]
    (remove (fn [item]
              (let [k (key-fn item)]
                (if (contains? @seen k)
                  true
                  (do (swap! seen conj k) false))))
            coll)))

(defn filter-categories [categories names]
  (letfn [(filter-helper [nodes]
            (reduce
             (fn [acc {:keys [name children _] :as node}]
               (let [matched (if (some #(str/includes? (str/lower-case name)
                                                       (str/lower-case %))
                                       names)
                               [(assoc node :children [])]
                               [])]
                 (concat acc matched (filter-helper children))))
             []
             nodes))]
    (->> (filter-helper categories)
         (distinct-by :category_id))))

(defui main [{:keys [className]}]
  (let [categories (:children (:categories (router/useRouteLoaderData "models-page")))
        [search set-search!] (uix/use-state categories)
        [search-params set-search-params!] (router/useSearchParams)
        category-id (.. search-params (get "category_id"))
        category-name (find-category-name categories category-id)
        [open set-open!] (uix/use-state false)
        type (.. search-params (get "type"))
        [is-searching? set-is-searching!] (uix/use-state false)
        handle-search (fn [e]
                        (let [value (.. e -target -value)
                              filtered (filter-categories categories [value])]
                          (if (= value "")
                            (do
                              (set-is-searching! false)
                              (set-search! categories))
                            (do
                              (set-is-searching! true)
                              (set-search! filtered)))))

        handle-select (fn [e]
                        (set-open! false)
                        (let [id (.. e -currentTarget -value)]
                          (if (= id (.. search-params (get "category_id")))
                            (.delete search-params "category_id")
                            (.set search-params "category_id" id))

                          (set-is-searching! false)
                          (set-search! categories)
                          (.set search-params "page" "1")
                          (set-search-params! search-params)))

        [t] (useTranslation)]

    ($ RadioGroup
       ($ DropdownMenu {:open open
                        :on-open-change set-open!}
          ($ DropdownMenuTrigger {:asChild "true"}
             ($ Button {:variant "outline"
                        :data-test-id "category-filter-button"
                        :on-click #(set-open! (not open))
                        :disabled (or (= type "option")
                                      (= type "software"))
                        :class-name (str "min-w-48 max-w-48" className)}
                ($ List {:className "h-4 w-4 "})

                (if category-name
                  ($ :span {:class-name "truncate" :title category-name}
                     category-name)
                  (t "pool.models.filters.categories.title"))
                ($ ChevronDown {:className "ml-auto h-4 w-4 opacity-50"})))

          ($ DropdownMenuContent {:class-name "max-h-[300px]"
                                  :data-test-id "category-filter-dropdown"}
             ($ :div {:class-name "flex items-center space-x-2"}
                ($ Search {:class-name "mx-2 h-4 w-4 shrink-0 opacity-50"})
                ($ :input {:data-test-id "category-filter"
                           :on-change handle-search
                           :auto-complete "off"
                           :auto-correct "off"
                           :class-name "flex h-8 w-full rounded-md 
                        bg-transparent py-3 text-sm outline-none 
                        placeholder:text-muted-foreground 
                        disabled:cursor-not-allowed disabled:opacity-50"}))
             ($ DropdownMenuSeparator)
             ($ :div {:class-name "w-[300px] pb-4 px-4 rounded-lg"}
                ($ :div {:class-name "w-full -ml-4"}
                   (for [category search]
                     ($ file-tree-item (merge {:key (:category_id category)
                                               :on-select handle-select
                                               :searching is-searching?}
                                              category))))))))))

(def CategoryFilter
  (uix/as-react
   (fn [props]
     (main props))))
