(ns leihs.inventory.server.resources.pool.list.export
  (:require
   [honey.sql.helpers :as sql]))

(def select-model-fields
  [:models.product
   :models.version
   :models.manufacturer
   :models.technical_detail
   :models.internal_description
   :models.hand_over_note

   ; categories
   [(-> (sql/select [[:string_agg
                      [:distinct
                       [:coalesce :model_group_links.label
                        :model_groups.name]]
                      "; "]])
        (sql/from :model_groups)
        (sql/join :model_group_links
                  [:= :model_groups.id :model_group_links.child_id])
        (sql/join :model_links
                  [:= :model_groups.id :model_links.model_group_id])
        (sql/where [:= :model_groups.type "Category"])
        (sql/where [:= :model_links.model_id :models.id])
        (sql/group-by :model_links.model_id)) :categories]

   ; accessories
   [(-> (sql/select [[:string_agg :accessories.name "; "]])
        (sql/from :accessories)
        (sql/where [:= :accessories.model_id :models.id])
        (sql/group-by :accessories.model_id)) :accessories]

   ; models-compatibles
   [(-> (sql/select [[:string_agg :compatibles.name "; "]])
        (sql/from :models_compatibles)
        (sql/join [:models :compatibles]
                  [:= :models_compatibles.compatible_id :compatibles.id])
        (sql/where [:= :models_compatibles.model_id :models.id])
        (sql/group-by :models_compatibles.model_id)) :compatibles]

   ; properties
   [(-> (sql/select [[:string_agg
                      [:concat_ws ": " :properties.key
                       :properties.value]
                      "; "]])
        (sql/from :properties)
        (sql/where [:= :properties.model_id :models.id])
        (sql/group-by :properties.model_id)) :properties]

   ]
  )
