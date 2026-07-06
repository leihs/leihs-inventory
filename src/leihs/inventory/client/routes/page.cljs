(ns leihs.inventory.client.routes.page
  (:require
   ["@@/card" :refer [Card CardContent CardFooter CardHeader CardTitle]]
   ["@@/table" :refer [Table TableBody TableCell TableHead TableHeader
                       TableRow]]
   ["lucide-react" :refer [Pencil Eye]]
   ["react-i18next" :refer [useTranslation]]
   ["react-router-dom" :as router :refer [Link]]
   [leihs.inventory.client.components.typo :refer [Typo]]
   [uix.core :as uix :refer [defui $]]
   [uix.dom]))

(defui page []
  (let [[t] (useTranslation)
        {:keys [profile]} (router/useRouteLoaderData "root")
        pools (->> (get profile :available_inventory_pools [])
                   (sort-by :name))]

    ($ Card {:class-name "mt-12 mb-6 overflow-hidden"}
       ($ CardHeader
          ($ CardTitle (t "root.welcome")))
       ($ CardContent

          ($ :div {:class-name "border rounded-md"}
             ($ Table
                ($ TableHeader
                   ($ TableRow
                      ($ TableHead (t "root.pools.name"))
                      ($ TableHead (t "root.pools.access_rights"))))

                ($ TableBody
                   (for [pool pools]
                     ($ TableRow {:key (:id pool)
                                  :class-name "even:bg-muted"}
                        ($ TableCell
                           ($ Typo {:variant "link"}
                              ($ Link {:class-name "underline"
                                       :to (str (:id pool))}
                                 (:name pool))))

                        ($ TableCell
                           (case (:permission pool)
                             "edit" ($ Pencil {:class-name "w-4 h-4"})
                             "read" ($ Eye {:class-name "w-4 h-4"})))))))))

       ($ CardFooter {:class-name "py-3 px-6 border-t-[1px] bg-muted/50 flex items-center justify-end"}
          ($ Typo {:variant "link"}
             ($ :a {:target "_blank"
                    :href "/inventory/swagger-ui/"}
                (t "root.api_browser")))))))

