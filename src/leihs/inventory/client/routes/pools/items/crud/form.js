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
      {
        name: "models",
        label: "Modell *",
        component: "models",
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
        props: {
          "data-id": "retire-reason",
        },
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
      {
        name: "last-inventory-date",
        label: "Letzte Inventur",
        component: "calendar",
        props: {
          mode: "single",
        },
      },
    ],
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
          text: {
            description: "Anhänge zu diesem Gegenstand",
          },
          multiple: true,
          sortable: false,
        },
      },
    ],
  },
  {
    title: "Ort",
    blocks: [
      {
        name: "shelf",
        label: "Regal",
        component: "input",
        props: {
          type: "text",
          autoComplete: "off",
        },
      },
    ],
  },
  {
    title: "Rechnungsinformationen",
    blocks: [
      {
        name: "p4u-number",
        label: "P4U Nummer",
        component: "input",
        props: {
          type: "text",
          autoComplete: "off",
        },
      },
      {
        name: "properties_reference",
        label: "Bezug",
        component: "radio-group",
        props: {
          options: [
            {
              value: "properties_reference_invoice",
              label: "laufende Rechnung",
            },
            { value: "properties_reference_investment", label: "Investition" },
          ],
        },
      },
      {
        name: "invoice-number",
        label: "Rechnungsnummer",
        component: "input",
        props: {
          type: "text",
          autoComplete: "off",
          "data-id": "invoice-number",
        },
      },
      {
        name: "invoice-date",
        label: "Rechnungsdatum",
        component: "calendar",
        props: {
          mode: "single",
        },
      },
      {
        name: "item-price",
        label: "Anschaffungswert",
        component: "input",
        props: {
          type: "text",
          autoComplete: "off",
        },
      },
      {
        name: "gurantee-end-date",
        label: "Garantieablaufdatum",
        component: "calendar",
        props: {
          mode: "single",
        },
      },
    ],
  },
]
