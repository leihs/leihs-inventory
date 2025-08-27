import { z } from "zod"

export const schema = z.object({
  name: z.string().trim().min(1).trim(),
  models: z
    .array(
      z.object({
        id: z.string(),
        quantity: z.coerce.number().min(0).default(1),
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
          required: true,
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
          required: true,
          text: {
            select: "pool.templates.template.models.blocks.models.select",
            placeholder:
              "pool.templates.template.models.blocks.models.placeholder",
            not_found: "pool.templates.template.models.blocks.models.not_found",
            searching: "pool.templates.template.models.blocks.models.searching",
            search_empty:
              "pool.templates.template.models.blocks.models.search_empty",
          },
          attributes: ["quantity", "available"],
        },
      },
    ],
  },
]
