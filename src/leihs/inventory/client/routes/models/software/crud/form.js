import { z } from "zod"

export const schema = z.object({
  product: z.string().min(1),
  version: z.string().optional(),
  manufacturer: z.string().optional(),
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
        name: "manufacturers",
        label: "pool.software.software.blocks.manufacturer.label",
        component: "manufacturers",
        props: {
          text: {
            label: "pool.software.software.blocks.manufacturer.label",
          },
        },
      },
      {
        name: "software_information",
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
