import { z } from "zod"

export const schema = z.object({
  name: z.string().trim().min(1).trim(),
  models: z
    .array(
      z.object({
        id: z.string(),
        quantity: z.coerce.number().min(0).default(0),
      }),
    )
    .min(1),
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
        props: {
          text: {
            select: "pool.templates.template.models.blocks.models.select",
            placeholder:
              "pool.templates.template.models.blocks.models.placeholder",
            not_found: "pool.templates.template.models.blocks.models.not_found",
          },
          attributes: ["quantity", "available"],
        },
      },
    ],
  },
]
