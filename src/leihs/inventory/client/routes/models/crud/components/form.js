import { z } from "zod"

const modelProperties = z.object({
  key: z.string(),
  value: z.string(),
})

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
    .array(z.object({ id: z.string(), name: z.string(), type: z.string() }))
    .optional(),
  images: z
    .array(
      z.object({
        id: z.string().nullish(),
        file: z
          .instanceof(File)
          .refine((file) =>
            ["image/png", "image/jpeg", "image/jpg"].includes(file.type),
          ),
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
        cover_image_id: z.string().nullish(),
        cover_image_url: z.string().nullish(),
      }),
    )
    .optional(),
  properties: z.array(modelProperties).optional(),
})

export const structure = [
  {
    title: "Produkt",
    blocks: [
      {
        name: "is-package",
        label: "dies ist ein Paket?",
        component: "checkbox",
        props: {
          defaultChecked: false,
          "data-id": "is-package",
        },
      },
      {
        name: "product",
        label: "Produkt",
        component: "input",
        props: {
          "auto-complete": "off",
        },
      },
      {
        name: "version",
        label: "Version",
        component: "input",
        props: {
          type: "text",
          "auto-complete": "off",
        },
      },
      {
        name: "manufacturers",
        label: "Hersteller",
        component: "manufacturers",
      },
      {
        name: "description",
        label: "Beschreibung",
        component: "textarea",
        props: {
          "auto-complete": "off",
        },
      },
      {
        name: "technical_detail",
        label: "Technische Details",
        component: "textarea",
        props: {
          "auto-complete": "off",
        },
      },
      {
        name: "internal_description",
        label: "Interne Beschreibung",
        component: "textarea",
        props: {
          "auto-complete": "off",
        },
      },
      {
        name: "hand_over_note",
        label: "Übergabevermerk",
        component: "textarea",
        props: {
          "auto-complete": "off",
        },
      },
    ],
  },
  {
    title: "Zuteilungen",
    blocks: [
      {
        name: "entitlements",
        label: "Berechtigungen",
        description: "Listen Sie die Berechtigungen auf",
        component: "entitlement-allocations",
      },
    ],
  },
  {
    title: "Kategorien",
    blocks: [
      {
        name: "categories",
        label: "Kategorien",
        component: "category-assignment",
      },
    ],
  },
  {
    title: "Bilder",
    blocks: [
      {
        name: "images",
        label: "Bilder",
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
    title: "Anhänge",
    blocks: [
      {
        name: "attachments",
        label: "Anhänge",
        component: "attachments",
        props: {
          multiple: true,
          sortable: false,
        },
      },
    ],
  },
  {
    title: "Zubehör",
    blocks: [
      {
        name: "accessories",
        component: "accessory-list",
      },
    ],
  },
  {
    title: "Ergänzende Modelle",
    blocks: [
      {
        name: "compatibles",
        label: "Modell-Links",
        component: "compatible-models",
      },
    ],
  },
  {
    title: "Eigenschaften",
    blocks: [
      {
        name: "properties",
        component: "model-properties",
        props: {
          inputs: [
            {
              name: "key",
              label: "Eigenschaft",
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
              label: "Wert",
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
