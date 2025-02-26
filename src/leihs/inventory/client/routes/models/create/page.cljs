(ns leihs.inventory.client.routes.models.create.page
  (:require
   ["@/components/react/scrollspy/scrollspy" :refer [Scrollspy ScrollspyItem
                                                     ScrollspyMenu]]
   ["@/routes/models/create/form" :refer [schema structure]]
   ["@@/button" :refer [Button]]
   ["@@/card" :refer [Card CardContent]]
   ["@@/form" :refer [Form]]
   ["@hookform/resolvers/zod" :refer [zodResolver]]
   ["@tanstack/react-query" :as react-query :refer [useMutation useQuery]]
   ["react-hook-form" :refer [useForm]]
   ["react-router-dom" :as router :refer [Link]]
   [leihs.inventory.client.lib.utils :refer [cj jc]]
   [leihs.inventory.client.routes.models.create.context :refer [state-context]]
   [leihs.inventory.client.routes.models.create.fields :as form-fields]
   [uix.core :as uix :refer [$ defui]]
   [uix.dom]))

(defn on-submit [data event]
  (let [form-data (js/FormData.)]
    (.. event (preventDefault))
    (js/console.debug "is valid " data)

    (doseq [[k v] (js/Object.entries data)]
      (cond
        ;; add images as binary data
        (= k "images")
        (if (js/Array.isArray v)
          (doseq [v v]
            (.. form-data (append "images" v)))
          (.. form-data (append "images" v)))

        ;; add attachments as binary data
        (= k "attachments")
        (if (js/Array.isArray v)
          (doseq [v v]
            (.. form-data (append "attachments" v)))
          (.. form-data (append "attachments" v)))

        ;; add fields as text data
        :else (.. form-data (append k v))))

    #_(js/fetch "http://localhost:5002/api/sample"
                (cj {:method "POST"
                     :body form-data}))

    (js/fetch "/inventory/8bd16d45-056d-5590-bc7f-12849f034351/model"
              (cj {:method "POST"
                   :headers {"Accept" "application/json"}
                   :body form-data}))))

(defn- on-invalid [data event]
  (js/console.debug "is invalid: " data))

(defn fetch-entitlement-groups [params]
  (let [path (router/generatePath "/inventory/:pool-id/entitlement-groups" params)]
    (.. (js/fetch path (cj {:headers {"Accept" "application/json"}}))
        (then #(.json %))
        (then #(jc %)))))

(defn fetch-categories []
  (.. (js/fetch "/inventory/tree" (cj {:headers {"Accept" "application/json"}}))
      (then #(.json %))
      (then #(jc %))))

(defn fetch-models [params]
  (.. (js/fetch "/inventory/models-compatibles" (cj {:headers {"Accept" "application/json"}}))
      (then #(.json %))
      (then #(jc %))))

(defui page []
  (let [form (useForm (cj {:resolver (zodResolver schema)
                           :defaultValues {:product ""
                                           :isPackage false
                                           :manufacturer ""
                                           :description ""
                                           :internalDescription ""
                                           :technicalDetails ""
                                           :handOverNote ""
                                           :version ""
                                           :categories []
                                           :entitlements []
                                           :properties []
                                           :accessories []}}))
        params (router/useParams)
        handleSubmit (:handleSubmit (jc form))
        control (:control (jc form))
        entitlement-groups (jc (useQuery (cj {:queryKey ["entitlement-groups"]
                                              :queryFn #(fetch-entitlement-groups params)})))

        models (jc (useQuery (cj {:queryKey ["models"]
                                  :queryFn #(fetch-models params)})))

        categories (jc (useQuery (cj {:queryKey ["categorories"]
                                      :queryFn #(fetch-categories)})))]

    ;; without this, form data is stale.
    ;; But this also means the form is evaluated every render
    (.. form (watch))

    (cond
      (and (:isLoading entitlement-groups) (:isLoading categories))
      ($ :div "Loading...")

      (or (:isError entitlement-groups) (:isError categories))
      ($ :div "Error!")

      (and (:isSuccess entitlement-groups) (:isSuccess categories))
      ($ (.-Provider state-context) {:value {:models (:data models)
                                             :entitlements (:data entitlement-groups)
                                             :categories (:data categories)}}
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
                                                        :form form
                                                        :block block}))))))

                     ($ :div {:className "h-max flex space-x-6 sticky top-[43vh] ml-auto"}

                        ($ Link {:to (router/generatePath "/inventory/:pool-id/models" params)
                                 :className "self-center hover:underline"}
                           "Abbrechen")

                        ($ Button {:type "submit"
                                   :form "create-model"
                                   :className "self-center"}
                           "Submit"))))))))))


