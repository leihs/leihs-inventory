(ns leihs.inventory.client.routes.pools.inventory.entitlement-groups.crud.components.field-dispatcher
  (:require
   [leihs.inventory.client.components.form.fields.common-field :refer [CommonField]]
   [leihs.inventory.client.components.form.fields.radio-group-field :refer [RadioGroupField]]
   [leihs.inventory.client.routes.pools.inventory.entitlement-groups.crud.components.fields.groups-field :refer [GroupsField]]
   [leihs.inventory.client.routes.pools.inventory.entitlement-groups.crud.components.fields.models-field :refer [ModelsField]]
   [leihs.inventory.client.routes.pools.inventory.entitlement-groups.crud.components.fields.users-field :refer [UsersField]]
   [uix.core :refer [$ defui]]))

(defui FieldDispatcher [{:keys [form block]}]
  (cond
    (-> block :component (= "models"))
    ($ ModelsField {:form form
                    :block block})

    (-> block :component (= "users"))
    ($ UsersField {:form form
                   :block block})

    (-> block :component (= "groups"))
    ($ GroupsField {:form form
                    :block block})

    (-> block :component (= "radio-group"))
    ($ RadioGroupField {:form form
                        :block block})

    ;; default case - renders a component from the component map
    :else
    ($ CommonField {:form form
                    :block block})))
