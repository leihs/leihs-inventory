Models
--

### Create model

1. Load form
    1. https://test.leihs.zhdk.ch/manage/8bd16d45-056d-5590-bc7f-12849f034351/models?search_term=
    2. https://test.leihs.zhdk.ch/manage/8bd16d45-056d-5590-bc7f-12849f034351/categories?search_term= (
       entitlement_groups)
    3. https://test.leihs.zhdk.ch/manage/8bd16d45-056d-5590-bc7f-12849f034351/groups?search_term=
2. Save data
   ```json
   {
       "model": {
           "type": "model",
           "product": "abc",
           "version": "vers",
           "manufacturer": "3M",
           "description": "beschr",
           "technical_detail": "techDat",
           "internal_description": "internBez",
           "hand_over_note": "wichtigNotiz",
           "category_ids": [
               "87053e34-db44-5bbb-9ac1-27d44aa484cd"
           ],
           "compatible_ids": [
               "621e9d2e-87e9-4a32-a403-4c516cd56712"
           ],
           "properties_attributes": [
               {
                   "key": "eigen",
                   "value": "schaft"
               }
           ],
           "partitions_attributes": {
               "rails_dummy_id_0": {
                   "group_id": "8c3a0fdd-9dc1-5d3b-ae22-29ca3d9bced5",
                   "quantity": "1"
               }
           },
           "accessories_attributes": {
               "535972aa-4932-4943-9493-af4c59292871": {
                   "id": "535972aa-4932-4943-9493-af4c59292871",
                   "inventory_pool_toggle": "1,8bd16d45-056d-5590-bc7f-12849f034351",
                   "_destroy": null
               }
           },
           "images_attributes": {
               "ca99833b-3b8e-4476-8af5-333eef951c46": {
                   "id": "ca99833b-3b8e-4476-8af5-333eef951c46",
                   "_destroy": null,
                   "is_cover": false
               }
           },
           "attachments_attributes": {
               "e827bfd0-ee87-4433-99ef-eb71bee2dae8": {
                   "id": "e827bfd0-ee87-4433-99ef-eb71bee2dae8",
                   "_destroy": null
               }
           }
       }
   }
   ```
3. Save image
   - https://test.leihs.zhdk.ch/manage/8bd16d45-056d-5590-bc7f-12849f034351/models/store_image_react
4. Save attachment
   https://test.leihs.zhdk.ch/manage/8bd16d45-056d-5590-bc7f-12849f034351/models/store_attachment_react
