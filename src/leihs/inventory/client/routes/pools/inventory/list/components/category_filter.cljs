(ns leihs.inventory.client.routes.pools.inventory.list.components.category-filter
  (:require
   ["@@/button" :refer [Button]]
   ["@@/checkbox" :refer [Checkbox]]
   ["@@/collapsible" :refer [Collapsible CollapsibleContent CollapsibleTrigger]]
   ["@@/dropdown-menu" :refer [DropdownMenu DropdownMenuContent
                               DropdownMenuTrigger DropdownMenuSeparator]]
   ["lucide-react" :refer [ChevronDown ChevronRight List Search]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router]
   [clojure.string :as str]
   [uix.core :as uix :refer [$ defui]]))

(defui file-tree-item [{:keys [name category_id searching children]}]
  (let [[search-params set-search-params!] (router/useSearchParams)
        handle-select (fn [e]
                        (let [id (.. e -currentTarget -value)]
                          (if (= id (.. search-params (get "category_id")))
                            (.delete search-params "category_id")
                            (.set search-params "category_id" id))
                          (.set search-params "page" "1")
                          (set-search-params! search-params)))]

    (if (not (seq children))
      ($ :div {:class-name "relative"}
         ($ Checkbox {:data-test-id category_id
                      :checked (= category_id (.. search-params (get "category_id")))
                      :value category_id
                      :name category_id
                      :on-click handle-select
                      :class-name "absolute right-[-25px] top-[6px]"})
         ($ :span {:class-name (str "flex text-sm items-center py-1 " (if searching "pl-4" "pl-10"))} name))

      ($ Collapsible {:class-name "pl-4"}
         ($ :div {:class-name "relative"}
            ($ Checkbox {:data-test-id category_id
                         :checked (= category_id (.. search-params (get "category_id")))
                         :value category_id
                         :name category_id
                         :on-click handle-select
                         :class-name "absolute right-[-25px] top-[6px]"})
            ($ CollapsibleTrigger {:class-name "text-sm w-full group flex items-center gap-2 py-1"}
               ($ ChevronRight {:class-name "h-4 w-4 group-data-[state=open]:rotate-90 transition-transform"})
               ($ :span {:class-name "flex items-center gap-2"} name)))

         ($ CollapsibleContent
            (for [child children]
              ($ file-tree-item (merge {:key (:category_id child)}
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
               (let [matched (if (some #(str/starts-with? name %) names)
                               [(assoc node :children [])]
                               [])]
                 (concat acc matched (filter-helper children))))
             []
             nodes))]
    (->> (filter-helper categories)
         (distinct-by :category_id))))

(defui main [{:keys [class-name]}]
  (let [categories (:children (:categories (router/useRouteLoaderData "models-page")))
        [search set-search!] (uix/use-state categories)
        [search-params _] (router/useSearchParams)
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
        [t] (useTranslation)]

    ($ DropdownMenu
       ($ DropdownMenuTrigger {:asChild "true"}
          ($ Button {:variant "outline"
                     :disabled (or (= type "option")
                                   (= type "software"))
                     :class-name class-name}
             ($ List {:className "h-4 w-4 mr-2 "})
             (t "pool.models.filters.categories.title")
             ($ ChevronDown {:className "ml-auto h-4 w-4 opacity-50"})))
       ($ DropdownMenuContent {:class-name "h-[300px]"}
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
                                            :searching is-searching?}
                                           category)))))))))

(def CategoryFilter
  (uix/as-react
   (fn [props]
     (main props))))
