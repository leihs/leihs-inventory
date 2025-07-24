import { z } from "zod"

export const schema = z.object({
  is_package: z.boolean().optional(),
  product: z.string().min(1),
  version: z.string().optional(),
  manufacturer: z.string().optional(),
  description: z.string().optional(),
  technical_detail: z.string().optional(),
  internal_description: z.string().optional(),
  hand_over_note: z.string().optional(),
  entitlements: z
    .array(
      z.object({
        id: z.string().nullish(),
        group_id: z.string(),
        quantity: z.coerce.number(),
        name: z.string(),
      }),
    )
    .optional(),
  categories: z
    .array(z.object({ id: z.string(), name: z.string() }))
    .optional(),
  images: z
    .array(
      z.object({
        id: z.string().nullish(),
        file: z.instanceof(File).optional(),
        is_cover: z.boolean(),
      }),
    )
    .optional(),
  attachments: z
    .array(
      z.object({
        id: z.string().nullish(),
        file: z.instanceof(File).optional(),
      }),
    )
    .optional(),
  accessories: z.array(z.object({ name: z.string() })).optional(),
  compatibles: z
    .array(
      z.object({
        product: z.string(),
        id: z.string(),
      }),
    )
    .optional(),
  properties: z
    .array(
      z.object({
        id: z.string().nullish(),
        key: z.string(),
        value: z.string(),
      }),
    )
    .optional(),
})

export const structure = [
  {
    title: "pool.model.product.title",
    blocks: [
      {
        name: "is_package",
        label: "pool.model.product.blocks.package.label",
        component: "checkbox",
        props: {
          defaultChecked: false,
          "data-id": "is-package",
        },
      },
      {
        name: "product",
        label: "pool.model.product.blocks.product.label",
        component: "input",
        props: {
          "auto-complete": "off",
        },
      },
      {
        name: "version",
        label: "pool.model.product.blocks.version.label",
        component: "input",
        props: {
          type: "text",
          "auto-complete": "off",
        },
      },
      {
        name: "manufacturers",
        label: "pool.model.product.blocks.manufacturer.label",
        component: "manufacturers",
      },
      {
        name: "description",
        label: "pool.model.product.blocks.description.label",
        component: "textarea",
        props: {
          "auto-complete": "off",
        },
      },
      {
        name: "technical_detail",
        label: "pool.model.product.blocks.technical_detail.label",
        component: "textarea",
        props: {
          "auto-complete": "off",
        },
      },
      {
        name: "internal_description",
        label: "pool.model.product.blocks.internal_description.label",
        component: "textarea",
        props: {
          "auto-complete": "off",
        },
      },
      {
        name: "hand_over_note",
        label: "pool.model.product.blocks.hand_over_note.label",
        component: "textarea",
        props: {
          "auto-complete": "off",
        },
      },
    ],
  },
  {
    title: "pool.model.entitlements.title",
    blocks: [
      {
        name: "entitlements",
        label: "pool.model.entitlements.blocks.entitlements.label",
        description: "Listen Sie die Berechtigungen auf",
        component: "entitlement-allocations",
      },
    ],
  },
  {
    title: "pool.model.categories.title",
    blocks: [
      {
        name: "categories",
        label: "pool.model.categories.blocks.categories.label",
        component: "category-assignment",
      },
    ],
  },
  {
    title: "pool.model.images.title",
    blocks: [
      {
        name: "images",
        label: "pool.model.images.blocks.images.label",
        component: "image-dropzone",
        props: {
          sortable: false,
          multiple: true,
          filetypes: "jpeg,png",
        },
      },
    ],
  },
  {
    title: "pool.model.attachments.title",
    blocks: [
      {
        name: "attachments",
        label: "pool.model.attachments.blocks.attachments.label",
        component: "attachments",
        props: {
          multiple: true,
          sortable: false,
        },
      },
    ],
  },
  {
    title: "pool.model.accessories.title",
    blocks: [
      {
        name: "accessories",
        component: "accessory-list",
      },
    ],
  },
  {
    title: "pool.model.compatible_models.title",
    blocks: [
      {
        name: "compatibles",
        label: "pool.model.fields.blocks.compatibles.label",
        component: "compatible-models",
      },
    ],
  },
  {
    title: "pool.model.model_properties.title",
    blocks: [
      {
        name: "properties",
        component: "model-properties",
        props: {
          inputs: [
            {
              name: "key",
              label: "pool.model.fields.blocks.properties.label",
              component: "textarea",
              props: {
                autoscale: true,
                resize: false,
                placeholder: "Eigenschaft eingeben",
                "auto-complete": "off",
              },
            },
            {
              name: "value",
              label: "pool.model.fields.blocks.value.label",
              component: "textarea",
              props: {
                autoscale: true,
                resize: false,
                placeholder: "Wert eingeben",
                "auto-complete": "off",
              },
            },
          ],
        },
      },
    ],
  },
]
