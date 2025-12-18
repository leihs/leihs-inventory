import { z } from "zod"

export const schema = z.object({
  product: z.string().min(1),
  version: z.string().optional(),
  manufacturer: z
    .union([
      z.string(),
      z
        .object({ value: z.string(), label: z.string() })
        .transform((val) => val.value),
    ])
    .optional(),
  technical_detail: z.string().optional(),
  attachments: z
    .array(
      z.object({
        id: z.string().nullish(),
        file: z.instanceof(File).optional(),
      }),
    )
    .optional(),
})

export const structure = [
  {
    title: "pool.software.software.title",
    blocks: [
      {
        name: "product",
        label: "pool.software.software.blocks.product.label",
        component: "input",
        props: {
          "auto-complete": "off",
        },
      },
      {
        name: "version",
        label: "pool.software.software.blocks.version.label",
        component: "input",
        props: {
          type: "text",
          "auto-complete": "off",
        },
      },
      {
        name: "manufacturer",
        label: "pool.software.software.blocks.manufacturer.label",
        component: "autocomplete",
        props: {
          instant: true,
          extendable: true,
          interpolate: true,
          "auto-complete": "off",
          "values-url":
            "/inventory/:pool-id/manufacturers/?type=Software&search=",

          text: {
            search: "pool.software.software.blocks.manufacturer.search",
            select: "pool.software.software.blocks.manufacturer.select",
            add_new: "pool.software.software.blocks.manufacturer.add_new",
            empty: "pool.software.software.blocks.manufacturer.empty",
          },
        },
      },
      {
        name: "technical_detail",
        label: "pool.software.software.blocks.software_information.label",
        component: "textarea",
        props: {
          "auto-complete": "off",
        },
      },
    ],
  },
  {
    title: "pool.software.attachments.title",
    blocks: [
      {
        name: "attachments",
        label: "pool.software.attachments.blocks.attachments.label",
        component: "attachments",
        props: {
          text: {
            description:
              "pool.software.attachments.blocks.attachments.table.description",
          },
          multiple: true,
          sortable: false,
        },
      },
    ],
  },
]
