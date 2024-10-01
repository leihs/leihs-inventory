import { z } from "zod"

const fileSchema = z.object({
  name: z.string(),
  size: z.number().max(5 * 1024 * 1024, "File size should not exceed 5MB"), // Example: max 5MB
  type: z
    .string()
    .regex(/^image\/(jpeg|png)$/, "Only JPEG and PNG files are allowed"),
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
  entitlements: z.string().array(),
  categories: z.string().array(),
  images: z.array(fileSchema).nonempty("Bitte mindestens ein Bild hochladen"),
  attachments: z.string().array(),
  accessories: z.string().array(),
  model_links: z.string().array(),
  properties: z.string().array(),
})

export const structure = [
  {
    title: "Produkt",
    fields: [
      {
        name: "is_package",
        label: "dies ist ein Paket?",
        input: {
          component: "checkbox",
          props: { defaultChecked: false },
        },
      },
      {
        name: "product",
        label: "Produkt",
        description: "Wie soll das Produkt heissen?",
        input: {
          component: "input",
          props: {
            placeholder: "Produktnamen eingeben",
            "auto-complete": "off",
          },
        },
      },
      {
        name: "version",
        label: "Version",
        description: "Welche Version hat das Produkt?",
        input: {
          component: "input",
          props: {
            type: "number",
            placeholder: "Version eingeben",
            "auto-complete": "off",
          },
        },
      },
      {
        name: "manufacturer",
        label: "Hersteller",
        description: "Wer ist der Hersteller?",
        input: {
          component: "input",
          props: {
            placeholder: "Hersteller eingeben",
            "auto-complete": "off",
          },
        },
      },
      {
        name: "description",
        label: "Beschreibung",
        description: "Beschreiben Sie das Produkt",
        input: {
          component: "textarea",
          props: {
            placeholder: "Beschreibung eingeben",
            "auto-complete": "off",
          },
        },
      },
      {
        name: "technical_detail",
        label: "Technische Details",
        description: "Geben Sie technische Details an",
        input: {
          component: "textarea",
          porps: {
            placeholder: "Technische Details eingeben",
            "auto-complete": "off",
          },
        },
      },
      {
        name: "internal_description",
        label: "Interne Beschreibung",
        description: "Geben Sie eine interne Beschreibung an",
        input: {
          component: "textarea",
          props: {
            placeholder: "Interne Beschreibung eingeben",
            "auto-complete": "off",
          },
        },
      },
      {
        name: "hand_over_note",
        label: "Übergabevermerk",
        description: "Geben Sie einen Übergabevermerk an",
        input: {
          component: "textarea",
          props: {
            placeholder: "Übergabevermerk eingeben",
            "auto-complete": "off",
          },
        },
      },
    ],
  },
  {
    title: "Zuteilungen",
    fields: [
      {
        name: "entitlements",
        label: "Berechtigungen",
        description: "Listen Sie die Berechtigungen auf",
        input: {
          component: "combobox",
          props: {
            placeholder: "Berechtigungen eingeben",
            "auto-complete": "off",
          },
        },
      },
    ],
  },
  {
    title: "Kategorien",
    fields: [
      {
        name: "categories",
        label: "Kategorien",
        description: "Listen Sie die Kategorien auf",
        input: {
          component: "combobox",
          props: { placeholder: "Kategorien eingeben", "auto-complete": "off" },
        },
      },
    ],
  },
  {
    title: "Bilder",
    fields: [
      {
        name: "images",
        label: "Bilder",
        description: "Listen Sie die Bild-URLs auf",
        input: {
          component: "dropzone",
          props: {
            sortable: true,
            multiple: true,
            filetypes: "jpeg,png",
          },
        },
      },
    ],
  },
  {
    title: "Anhänge",
    fields: [
      {
        name: "attachments",
        label: "Anhänge",
        description: "Listen Sie die Anhang-URLs auf",
        input: {
          component: "dropzone",
          props: {
            sortable: false,
            multiple: true,
            filetypes: "pdf",
          },
        },
      },
    ],
  },
  {
    title: "Zubehör",
    fields: [
      {
        name: "accessories",
        label: "Zubehör",
        description: "Listen Sie das Zubehör auf",
        input: {
          component: "input",
          props: {
            type: "file",
            placeholder: "Zubehör eingeben",
            "auto-complete": "off",
          },
        },
      },
    ],
  },
  {
    title: "Ergänzende Modelle",
    fields: [
      {
        name: "model_links",
        label: "Modell-Links",
        description: "Listen Sie die Modell-Links auf",
        input: {
          component: "input",
          props: {
            type: "file",
            placeholder: "Modell-Links eingeben",
            "auto-complete": "off",
          },
        },
      },
    ],
  },
  {
    title: "Eigenschaften",
    fields: [
      {
        name: "properties",
        label: "Eigenschaften",
        description: "Listen Sie die Eigenschaften auf",
        input: {
          component: "input",
          props: {
            type: "file",
            placeholder: "Eigenschaften eingeben",
            "auto-complete": "off",
          },
        },
      },
    ],
  },
]
