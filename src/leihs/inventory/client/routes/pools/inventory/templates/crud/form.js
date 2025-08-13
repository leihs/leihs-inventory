import { z } from "zod"

export const schema = z.object({
  name: z.string().min(1),
  entitlements: z
    .array(
      z.object({
        product: z.string(),
        quantitiy: z.coerce.number().optional(),
        id: z.string(),
      }),
    )
    .optional(),
})

export const structure = [
  {
    title: "pool.template.name.title",
    blocks: [
      {
        name: "name",
        label: "pool.template.name.blocks.name.label",
        component: "input",
        props: {
          "auto-complete": "off",
        },
      },
    ],
  },
  {
    title: "pool.model.entitlements.title",
    blocks: [
      {
        name: "entitlements",
        label: "pool.template.fields.blocks.entitlements.label",
        component: "entitlements",
      },
    ],
  },
]
