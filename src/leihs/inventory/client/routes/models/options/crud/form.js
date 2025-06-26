import { z } from "zod"

export const schema = z.object({
  product: z.string().min(1),
  inventory_code: z.string().regex(/^[a-zA-Z0-9]*$/),
  version: z.string().optional(),
  price: z.number().optional(),
})

export const structure = [
  {
    title: "pool.option.option.title",
    blocks: [
      {
        name: "product",
        label: "pool.option.option.blocks.product.label",
        component: "input",
        props: {
          required: true,
          "auto-complete": "off",
        },
      },
      {
        name: "inventory_code",
        label: "pool.option.option.blocks.inventory_code.label",
        component: "input",
        props: {
          required: true,
          type: "text",
          "auto-complete": "off",
        },
      },
      {
        name: "version",
        label: "pool.option.option.blocks.version.label",
        component: "input",
        props: {
          type: "text",
          "auto-complete": "off",
        },
      },
      {
        name: "price",
        label: "pool.option.option.blocks.price.label",
        component: "input",
        props: {
          "auto-complete": "off",
        },
      },
    ],
  },
]
