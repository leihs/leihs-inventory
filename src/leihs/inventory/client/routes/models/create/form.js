import { z } from "zod"

const fileSchema = z.object({
  name: z.string(),
  size: z.number().max(5 * 1024 * 1024, "File size should not exceed 5MB"), // Example: max 5MB
  type: z
    .string()
    .regex(/^image\/(jpeg|png)$/, "Only JPEG and PNG files are allowed"),
  is_cover: z.boolean().default(false),
})

const modelProperties = z.object({
  name: z.string().min(5, "Eigenschaft muss mindestens 5 Zeichen lang sein"),
  value: z.string().min(5, "Wert muss mindestens 5 Zeichen lang sein"),
})

export const schema = z.object({
  is_package: z.boolean().default(false),
  product: z.string().min(5, "Produktname muss mindestens 5 Zeichen lang sein"),
  version: z.coerce.number().positive("Version musss positiv sein"),
  manufacturer: z.string(),
  description: z.string(),
  technical_detail: z.string(),
  internal_description: z.string(),
  hand_over_note: z.string(),
  entitlements: z.array(
    z.object({ entitlement_group_id: z.string(), quantity: z.number() }),
  ),
  // categories: z.string().array(),
  images: z.array(fileSchema).nonempty("Bitte mindestens ein Bild hochladen"),
  attachments: z.string().array(),
  accessories: z.array(z.object({ accessory: z.string() })),
  model_links: z.string().array(),
  properties: z.array(modelProperties),
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
          type: "number",
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
        name: "technical_detail",
        label: "Technische Details",
        // description: "Geben Sie technische Details an",
        input: "textarea",
        porps: {
          placeholder: "Technische Details eingeben",
          "auto-complete": "off",
        },
      },
      {
        name: "internal_description",
        label: "Interne Beschreibung",
        // description: "Geben Sie eine interne Beschreibung an",
        input: "textarea",
        props: {
          placeholder: "Interne Beschreibung eingeben",
          "auto-complete": "off",
        },
      },
      {
        name: "hand_over_note",
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
  // {
  //   title: "Kategorien",
  //   blocks: [
  //     {
  //       name: "categories",
  //       label: "Kategorien",
  //       description: "Listen Sie die Kategorien auf",
  //       input: {
  //         component: "combobox",
  //         props: { placeholder: "Kategorien eingeben", "auto-complete": "off" },
  //       },
  //     },
  //   ],
  // },
  {
    title: "Bilder",
    blocks: [
      {
        name: "image-dropzone",
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
        name: "accessory-list",
        component: "accessory-list",
        props: {
          inputs: [
            {
              name: "accessory",
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
        name: "model_links",
        label: "Modell-Links",
        // description: "Listen Sie die Modell-Links auf",
        input: "input",
        props: {
          type: "file",
          placeholder: "Modell-Links eingeben",
          "auto-complete": "off",
        },
      },
    ],
  },
  {
    title: "Eigenschaften",
    blocks: [
      {
        name: "model-properties",
        component: "model-properties",
        props: {
          inputs: [
            {
              name: "name",
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
