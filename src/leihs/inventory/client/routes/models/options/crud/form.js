import { z } from "zod"

export const schema = z.object({
  product: z.string().min(1),
  inventory_code: z
    .string()
    .regex(/^[a-zA-Z0-9]*$/)
    .optional(),
  version: z.string().optional(),
  price: z.string().optional(),
})

export const structure = [
  {
    title: "pool.model.product.title",
    blocks: [
      {
        name: "product",
        label: "pool.model.product.blocks.product.label",
        component: "input",
        props: {
          required: true,
          "auto-complete": "off",
        },
      },
      {
        name: "inventory_code",
        label: "pool.model.product.blocks.version.label",
        component: "input",
        props: {
          required: true,
          type: "text",
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
        name: "price",
        label: "pool.model.product.blocks.hand_over_note.label",
        component: "input",
        props: {
          "auto-complete": "off",
        },
      },
    ],
  },
]
