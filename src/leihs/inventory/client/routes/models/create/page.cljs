(ns leihs.inventory.client.routes.models.create.page
  (:require
   ["@/components/react/scrollspy/scrollspy" :refer [Scrollspy ScrollspyItem
                                                     ScrollspyMenu]]
   ["@/routes/models/create/form" :refer [schema structure]]
   ["@@/button" :refer [Button]]
   ["@@/card" :refer [Card CardContent]]
   ["@@/form" :refer [Form]]
   ["@hookform/resolvers/zod" :refer [zodResolver]]
   ["react-hook-form" :refer [useForm]]
   ["react-router-dom" :as router :refer [Link]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [leihs.inventory.client.routes.models.create.fields :as form-fields]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defn on-submit [data]
  (js/console.debug "is valid: " data))

(defn- on-invalid [data]
  (js/console.debug "is invalid: " data))

(defui page []
  (let [form (useForm (cj {:resolver (zodResolver schema)
                           :defaultValues {:product ""
                                           :version ""}}))
        params (jc (router/useParams))
        handleSubmit (:handleSubmit (jc form))
        control (:control (jc form))]

    ($ :article
       ($ :h1 {:className "text-2xl bold font-bold mt-12 mb-6"}
          "Inventarliste - Ausleihe Toni Areal")

       ($ :h3 {:className "text-sm mt-12 mb-6 text-gray-500"}
          "Nehmen Sie Änderungen vor und speichern Sie anschliessend")

       ($ Card {:className "py-8 mb-12"}
          ($ CardContent
             ($ Scrollspy {:className "flex gap-4"}
                ($ ScrollspyMenu)

                ($ Form (merge form)
                   ($ :form {:id "create-model"
                             :className "space-y-12 w-3/5"
                             :on-submit (handleSubmit on-submit on-invalid)}

                      (for [section (jc structure)]
                        ($ ScrollspyItem {:className "scroll-mt-[10vh]"
                                          :key (:title section)
                                          :id (:title section)
                                          :name (:title section)}

                           ($ :h2 {:className "text-lg"} (:title section))
                           ($ :hr {:className "mb-4"})

                           (for [block (:blocks section)]
                             ($ form-fields/field {:key (:name block)
                                                   :control control
                                                   :block block}))))))

                ($ :div {:className "h-max flex space-x-6 sticky top-[43vh] ml-auto"}

                   ($ Link {:to (router/generatePath
                                 "/inventory/:pool-id/models"
                                 (cj params))
                            :className "self-center hover:underline"}
                      "Abbrechen")

                   ($ Button {:type "submit"
                              :form "create-model"
                              :className "self-center"}
                      "Submit"))))))))


