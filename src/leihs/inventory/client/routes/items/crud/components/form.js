import { z } from "zod"

export const schema = z.object({})

export const structure = [
  {
    title: "Gegenstand",
    blocks: [
      {
        name: "number-items",
        label: "Anzahl Gegenstände erstellen",
        component: "input",
        props: {
          type: "number",
          defaultValue: 1,
          "data-id": "number-items",
        },
      },
    ],
  },
]
