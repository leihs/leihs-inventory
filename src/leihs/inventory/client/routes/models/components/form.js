import { z } from "zod"

const modelProperties = z.object({
  key: z.string(),
  value: z.string(),
})

export const schema = z.object({
  is_package: z.boolean().optional(),
  product: z
    .string()
    .min(5, "Produktname muss mindestens 5 Zeichen lang sein")
    .max(255),
  version: z.string().optional(),
  manufacturer: z.string().optional(),
  description: z.string().optional(),
  technical_detail: z.string().optional(),
  internal_description: z.string().optional(),
  hand_over_note: z.string().optional(),
  entitlements: z
    .array(
      z.object({
        entitlement_group_id: z.string(),
        quantity: z.coerce.number(),
      }),
    )
    .optional(),
  categories: z.array(z.object({ id: z.string() })).optional(),
  images: z
    .array(
      z
        .instanceof(File)
        .refine(
          (file) =>
            ["image/png", "image/jpeg", "image/jpg"].includes(file.type),
          { message: "Invalid image file type" },
        ),
    )
    .optional(),
  image_attributes: z
    .array(
      z.object({
        is_cover: z.boolean(),
        checksum: z.string(),
        to_delete: z.boolean(),
      }),
    )
    .optional(),
  attachments: z.array(z.instanceof(File)).optional(),
  accessories: z.array(z.object({ name: z.string() })).optional(),
  compatibles: z.array(z.object({ product: z.string() })).optional(),
  properties: z.array(modelProperties).optional(),
})

export const structure = [
  {
    title: "Produkt",
    blocks: [
      {
        name: "is_package",
        label: "dies ist ein Paket?",
        input: "checkbox",
        props: { defaultChecked: false },
      },
      {
        name: "product",
        label: "Produkt",
        input: "input",
        props: {
          "auto-complete": "off",
        },
      },
      {
        name: "version",
        label: "Version",
        input: "input",
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
        input: "textarea",
        props: {
          "auto-complete": "off",
        },
      },
      {
        name: "technical_detail",
        label: "Technische Details",
        input: "textarea",
        props: {
          "auto-complete": "off",
        },
      },
      {
        name: "internal_description",
        label: "Interne Beschreibung",
        input: "textarea",
        props: {
          "auto-complete": "off",
        },
      },
      {
        name: "hand_over_note",
        label: "Übergabevermerk",
        input: "textarea",
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
        // input: {
        //   component: "combobox",
        //   props: {
        //     placeholder: "Berechtigungen eingeben",
        //     "auto-complete": "off",
        //   },
        // },
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
        // description: "Listen Sie die Bild-URLs auf",
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
        // description: "Listen Sie die Anhang-URLs auf",
        input: "dropzone",
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
        props: {
          inputs: [
            {
              name: "name",
              label: "Artikel",
              // description: "Listen Sie das Zubehör auf",
              component: "input",
              props: {
                placeholder: "Zubehör eingeben",
                "auto-complete": "on",
              },
            },
          ],
        },
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
        props: {},
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
