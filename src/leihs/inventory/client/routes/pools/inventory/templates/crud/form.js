import { z } from "zod"

export const schema = z.object({
  name: z.string().min(1),
  models: z
    .array(
      z.object({
        id: z.string(),
        quantitiy: z.coerce.number().optional(),
      }),
    )
    .optional(),
})

export const structure = [
  {
    blocks: [
      {
        name: "name",
        label: "pool.templates.template.name.blocks.name.label",
        component: "input",
        props: {
          "auto-complete": "off",
        },
      },
    ],
  },
  {
    title: "pool.templates.template.models.title",
    blocks: [
      {
        name: "models",
        label: "pool.templates.template.models.blocks.models.label",
        component: "models",
      },
    ],
  },
]
