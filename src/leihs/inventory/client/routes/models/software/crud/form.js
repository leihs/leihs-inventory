import { z } from "zod"

export const schema = z.object({
  product: z.string().min(1),
  version: z.string().optional(),
  manufacturer: z.string().optional(),
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
        component: "instant-search",
        props: {
          "auto-complete": "off",
          resource: "/inventory/:pool-id/manufacturers/?type=Software&search=",
          "not-found": "pool.software.software.blocks.manufacturer.not_found",
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
