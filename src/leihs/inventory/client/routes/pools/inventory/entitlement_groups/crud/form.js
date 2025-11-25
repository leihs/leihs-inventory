import { z } from "zod"

export const schema = z.object({
  name: z.string().trim().min(1).trim(),
  is_verification_required: z.string(),
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
        label:
          "pool.entitlement-groups.entitlement-group.name.blocks.name.label",
        component: "input",
        props: {
          required: true,
          "auto-complete": "off",
        },
      },
      {
        name: "is_verification_required",
        label: "aguga",
        component: "radio-group",
        props: {
          options: [
            { value: "ok", label: "OK" },
            { value: "not-ok", label: "Defekt" },
          ],
        },
      },
    ],
  },
  {
    title: "pool.entitlement-groups.entitlement-group.models.title",
    blocks: [
      {
        name: "models",
        label:
          "pool.entitlement-groups.entitlement-group.models.blocks.models.label",
        component: "models",
        props: {
          required: true,
          text: {
            select:
              "pool.entitlement-groups.entitlement-group.models.blocks.models.select",
            placeholder:
              "pool.entitlement-groups.entitlement-group.models.blocks.models.placeholder",
            not_found:
              "pool.entitlement-groups.entitlement-group.models.blocks.models.not_found",
            searching:
              "pool.entitlement-groups.entitlement-group.models.blocks.models.searching",
            search_empty:
              "pool.entitlement-groups.entitlement-group.models.blocks.models.search_empty",
          },
          attributes: ["quantity", "available"],
        },
      },
    ],
  },
]
