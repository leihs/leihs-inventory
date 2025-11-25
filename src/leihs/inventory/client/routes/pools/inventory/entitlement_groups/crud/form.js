import { z } from "zod"

export const schema = z.object({
  name: z.string().trim().min(1).trim(),
  is_verification_required: z.boolean(),
  models: z.array(
    z.object({
      id: z.string(),
      quantity: z.coerce.number().min(1).default(1),
    }),
  ),
  users: z.array(
    z.object({
      id: z.string(),
    }),
  ),
  groups: z.array(
    z.object({
      id: z.string(),
    }),
  ),
})

export const structure = [
  [
    {
      blocks: [
        {
          name: "name",
          label:
            "pool.entitlement_groups.entitlement_group.top_section.blocks.name.label",
          component: "input",
          props: {
            required: true,
            "auto-complete": "off",
          },
        },
        {
          name: "is_verification_required",
          label:
            "pool.entitlement_groups.entitlement_group.top_section.blocks.is_verification_required.label",
          component: "radio-group",
          props: {
            required: true,
            options: [
              {
                value: true,
                label:
                  "pool.entitlement_groups.entitlement_group.top_section.blocks.is_verification_required.yes",
              },
              {
                value: false,
                label:
                  "pool.entitlement_groups.entitlement_group.top_section.blocks.is_verification_required.no",
              },
            ],
          },
        },
      ],
    },
    {
      title: "pool.entitlement_groups.entitlement_group.models.title",
      blocks: [
        {
          name: "models",
          label:
            "pool.entitlement_groups.entitlement_group.models.blocks.models.label",
          component: "models",
          props: {
            text: {
              select:
                "pool.entitlement_groups.entitlement_group.models.blocks.models.select",
              placeholder:
                "pool.entitlement_groups.entitlement_group.models.blocks.models.placeholder",
              not_found:
                "pool.entitlement_groups.entitlement_group.models.blocks.models.not_found",
              searching:
                "pool.entitlement_groups.entitlement_group.models.blocks.models.searching",
              search_empty:
                "pool.entitlement_groups.entitlement_group.models.blocks.models.search_empty",
            },
            attributes: ["quantity", "available"],
          },
        },
      ],
    },
  ],
  [
    {
      title: "pool.entitlement_groups.entitlement_group.groups.title",
      blocks: [
        {
          name: "groups",
          label: undefined,
          component: "groups",
          props: {
            text: {
              select:
                "pool.entitlement_groups.entitlement_group.groups.blocks.groups.select",
              placeholder:
                "pool.entitlement_groups.entitlement_group.groups.blocks.groups.placeholder",
              not_found:
                "pool.entitlement_groups.entitlement_group.groups.blocks.groups.not_found",
              searching:
                "pool.entitlement_groups.entitlement_group.groups.blocks.groups.searching",
              search_empty:
                "pool.entitlement_groups.entitlement_group.groups.blocks.groups.search_empty",
            },
            attributes: ["quantity", "available"],
          },
        },
      ],
    },
    {
      title: "pool.entitlement_groups.entitlement_group.users.title",
      blocks: [
        {
          name: "users",
          label: undefined,
          component: "users",
          props: {
            text: {
              select:
                "pool.entitlement_groups.entitlement_group.users.blocks.users.select",
              placeholder:
                "pool.entitlement_groups.entitlement_group.users.blocks.users.placeholder",
              not_found:
                "pool.entitlement_groups.entitlement_group.users.blocks.users.not_found",
              searching:
                "pool.entitlement_groups.entitlement_group.users.blocks.users.searching",
              search_empty:
                "pool.entitlement_groups.entitlement_group.users.blocks.users.search_empty",
            },
            attributes: ["quantity", "available"],
          },
        },
      ],
    },
  ],
]
