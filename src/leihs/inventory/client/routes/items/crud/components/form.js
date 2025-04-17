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
        },
      },
      {
        name: "inventory-code",
        label: "Inventarcode *",
        component: "inventory-code",
      },
    ],
  },
  {
    title: "Zustand",
    blocks: [
      {
        name: "retired",
        label: "Ausmusterung",
        component: "select",
        props: {
          options: [
            { value: "yes", label: "Ja" },
            { value: "no", label: "Nein" },
          ],
        },
      },
      {
        name: "retire-reason",
        label: "Grund der Ausmusterung *",
        component: "textarea",
      },
      {
        name: "working",
        label: "Zustand",
        component: "radio-group",
        props: {
          options: [
            { value: "ok", label: "OK" },
            { value: "not-ok", label: "Defekt" },
          ],
        },
      },
      {
        name: "availability",
        label: "Verfügbarkeit",
        component: "radio-group",
        props: {
          options: [
            { value: "ok", label: "OK" },
            { value: "no-ok", label: "Unvollstaendig" },
          ],
        },
      },
      {
        name: "lendable",
        label: "Ausleihbar",
        component: "radio-group",
        props: {
          options: [
            { value: "ok", label: "OK" },
            { value: "not-ok", label: "Nicht ausleihbar" },
          ],
        },
      },
      {
        name: "status-note",
        label: "Statusnotiz",
        component: "textarea",
      },
    ],
  },
  {
    title: "Inventar",
    blocks: [
      {
        name: "relevance",
        label: "Inventarrevelanz",
        component: "select",
        props: {
          options: [
            { value: "yes", label: "Ja" },
            { value: "no", label: "Nein" },
          ],
        },
      },
    ]
  },
  {
    title: "Allgemeine Informationen",
    blocks: [
      {
        name: "serial-number",
        label: "Seriennummer",
        component: "input",
        props: {
          type: "text",
          placeholder: "Seriennummer",
          autoComplete: "off",
        },
      },
      {
        name: "imei-number",
        label: "IMEI-Nummer",
        component: "input",
        props: {
          type: "text",
          placeholder: "Seriennummer",
          autoComplete: "off",
        },
      },
      {
        name: "note",
        label: "Notiz",
        component: "textarea",
      },
      {
        name: "attachments",
        component: "attachments",
        props: {
          multiple: true,
          sortable: false,
        },
      },
    ]
  },
]
