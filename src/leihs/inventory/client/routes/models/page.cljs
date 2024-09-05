(ns leihs.inventory.client.routes.models.page
  (:require
   ["@@/button" :refer [Button]]
   ["@@/tabs" :refer [Tabs TabsContent TabsList TabsTrigger]]
   ["@tanstack/react-query" :as react-query :refer [useMutation useQuery]]
   ["lucide-react" :refer [CirclePlus]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router :refer [Link useParams]]
   [leihs.inventory.client.routes.models.components.tabs.inventory-list :as inventory-list]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defn jc [js]
  (js->clj js {:keywordize-keys true}))

(defn cj [clj]
  (clj->js clj))

(defn fetch-inventory []
  (.. (js/fetch "/inventory/models"
                (cj {:headers {"Accept" "application/json"}}))
      (then #(.json %))
      (then #(jc %))))

(defui page []
  (let [models (jc (useQuery (cj {:queryKey ["inventory"]
                                  :queryFn fetch-inventory})))
        tab-route (jc (useParams))
        [tab-value set-tab-value!] (uix/use-state "list")
        [t] (useTranslation)]

    (uix/use-effect
     (fn []
       (set-tab-value! (or (:tab tab-route) "list")))
     [tab-route])

    (cond
      (:isLoading models)
      ($ :div "Loading...") ;; or a spinner

      (:isError models)
      ($ :div "Error!") ;; or an error message

      (:isSuccess models)
      ($ :article
         ($ :h1 {:className "text-2xl font-bold mt-12 mb-6"}
            (t "models.title", "Inventarliste - Ausleihe Toni Areal Localized"))
         ($ Tabs {:defaultValue tab-value}
            ($ :div {:className "flex w-full"}
               ($ TabsList
                  ($ TabsTrigger {:value "list"}
                     ($ Link {:to "/inventory/models"} "Inventarliste"))
                  ($ TabsTrigger {:value "borrow"}
                     ($ Link {:to "/inventory/borrow"} "Ausleihe"))
                  ($ TabsTrigger {:value "statistik"}
                     ($ Link {:to "/inventory/statistics"} "Statistik")))
               ($ Button {:className "ml-auto"}
                  ($ :<>
                     ($ CirclePlus {:className "mr-2 h-4 w-4"})
                     ($ :<> "Inventar hinzufügen"))))
            ($ TabsContent {:value "list"}
               ($ inventory-list/main {:data (:data models)})))))))


