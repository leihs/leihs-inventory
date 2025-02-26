import { z } from "zod"

const modelProperties = z.object({
  key: z.string(),
  value: z.string(),
})

export const schema = z.object({
  isPackage: z.boolean().optional(),
  product: z
    .string()
    .min(5, "Produktname muss mindestens 5 Zeichen lang sein")
    .max(255),
  version: z.string().optional(),
  manufacturer: z.string().optional(),
  description: z.string().optional(),
  technicalDetails: z.string().optional(),
  internalDescription: z.string().optional(),
  handOverNote: z.string().optional(),
  entitlements: z
    .array(z.object({ entitlement_group_id: z.string(), quantity: z.string() }))
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
        name: "isPackage",
        label: "dies ist ein Paket?",
        input: "checkbox",
        props: { defaultChecked: false },
      },
      {
        name: "product",
        label: "Produkt",
        // description: "Wie soll das Produkt heissen?",
        input: "input",
        props: {
          placeholder: "Produktnamen eingeben",
          "auto-complete": "off",
        },
      },
      {
        name: "version",
        label: "Version",
        // description: "Welche Version hat das Produkt?",
        input: "input",
        props: {
          type: "text",
          placeholder: "Version eingeben",
          "auto-complete": "off",
        },
      },
      {
        name: "manufacturer",
        label: "Hersteller",
        // description: "Wer ist der Hersteller?",
        input: "input",
        props: {
          placeholder: "Hersteller eingeben",
          "auto-complete": "off",
        },
      },
      {
        name: "description",
        label: "Beschreibung",
        // description: "Beschreiben Sie das Produkt",
        input: "textarea",
        props: {
          placeholder: "Beschreibung eingeben",
          "auto-complete": "off",
        },
      },
      {
        name: "technicalDetails",
        label: "Technische Details",
        // description: "Geben Sie technische Details an",
        input: "textarea",
        porps: {
          placeholder: "Technische Details eingeben",
          "auto-complete": "off",
        },
      },
      {
        name: "internalDescription",
        label: "Interne Beschreibung",
        // description: "Geben Sie eine interne Beschreibung an",
        input: "textarea",
        props: {
          placeholder: "Interne Beschreibung eingeben",
          "auto-complete": "off",
        },
      },
      {
        name: "handOverNote",
        label: "Übergabevermerk",
        // description: "Geben Sie einen Übergabevermerk an",
        input: "textarea",
        props: {
          placeholder: "Übergabevermerk eingeben",
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
