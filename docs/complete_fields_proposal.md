# Leihs GET/POST Fields/Item Data Structure - Complete Specification

## GET Response Format

The endpoint returns an object containing field definitions and default values for creating/editing items and licenses.

### Response Structure

```json
{
  "data": {
    "inventory_pool_id": "uuid-of-default-pool",
    "responsible_department": "uuid-of-default-dept",
    "inventory_code": "P-AUS2008037"
  },
  "fields": [
    // Array of field definitions (see below)
  ]
}
```

### Complete Fields Array

All 58 fields from the `fields` table, ordered by position:

#### Core Fields (position 1-3)

```json
{
  "id": "inventory_code",
  "label": "Inventory Code",
  "group": null,
  "position": 1,
  "type": "text",
  "required": true
}
```

```json
{
  "id": "model_id",
  "label": "Model",
  "group": null,
  "position": 2,
  "type": "autocomplete-search",
  "required": true,
  "form_name": "model_id",
  "value_attr": "id",
  "search_attr": "search_term",
  "search_path": "/inventory/:pool-id/models"
}
```

```json
{
  "id": "license_version",
  "label": "License Version",
  "group": null,
  "position": 3,
  "type": "text"
}
```

```json
{
  "id": "software_model_id",
  "label": "Software",
  "group": null,
  "position": 3,
  "type": "autocomplete-search",
  "required": true,
  "form_name": "model_id",
  "value_attr": "id",
  "search_attr": "search_term",
  "search_path": "/inventory/:pool-id/software"
}
```

#### General Information Group (positions 4-8, 32-39, 45)

```json
{
  "id": "serial_number",
  "label": "Serial Number",
  "group": "General Information",
  "position": 4,
  "type": "text"
}
```

```json
{
  "id": "properties_mac_address",
  "label": "MAC-Address",
  "group": "General Information",
  "position": 5,
  "type": "text"
}
```

```json
{
  "id": "properties_imei_number",
  "label": "IMEI-Number",
  "group": "General Information",
  "position": 6,
  "type": "text"
}
```

```json
{
  "id": "name",
  "label": "Name",
  "group": "General Information",
  "position": 7,
  "type": "text"
}
```

```json
{
  "id": "note",
  "label": "Note",
  "group": "General Information",
  "position": 8,
  "type": "textarea"
}
```

```json
{
  "id": "properties_activation_type",
  "label": "Activation Type",
  "group": "General Information",
  "position": 32,
  "type": "select",
  "values": [
    {"label": "None", "value": "none"},
    {"label": "Dongle", "value": "dongle"},
    {"label": "Serial Number", "value": "serial_number"},
    {"label": "License Server", "value": "license_server"},
    {"label": "Challenge Response/System ID", "value": "challenge_response"}
  ],
  "default": "none"
}
```

```json
{
  "id": "properties_dongle_id",
  "label": "Dongle ID",
  "group": "General Information",
  "position": 33,
  "type": "text",
  "required": true,
  "visibility_dependency_field_id": "properties_activation_type",
  "visibility_dependency_value": "dongle"
}
```

```json
{
  "id": "properties_license_type",
  "label": "License Type",
  "group": "General Information",
  "position": 34,
  "type": "select",
  "values": [
    {"label": "Free", "value": "free"},
    {"label": "Single Workplace", "value": "single_workplace"},
    {"label": "Multiple Workplace", "value": "multiple_workplace"},
    {"label": "Site License", "value": "site_license"},
    {"label": "Concurrent", "value": "concurrent"}
  ],
  "default": "free"
}
```

```json
{
  "id": "properties_total_quantity",
  "label": "Total quantity",
  "group": "General Information",
  "position": 35,
  "type": "text"
}
```

```json
{
  "id": "properties_quantity_allocations",
  "label": "Quantity allocations",
  "group": "General Information",
  "position": 36,
  "type": "composite",
  "visibility_dependency_field_id": "properties_total_quantity",
  "data_dependency_field_id": "properties_total_quantity"
}
```

```json
{
  "id": "properties_operating_system",
  "label": "Operating System",
  "group": "General Information",
  "position": 37,
  "type": "checkbox",
  "values": [
    {"label": "Windows", "value": "windows"},
    {"label": "Mac OS X", "value": "mac_os_x"},
    {"label": "Linux", "value": "linux"},
    {"label": "iOS", "value": "ios"}
  ]
}
```

```json
{
  "id": "properties_installation",
  "label": "Installation",
  "group": "General Information",
  "position": 38,
  "type": "checkbox",
  "values": [
    {"label": "Citrix", "value": "citrix"},
    {"label": "Local", "value": "local"},
    {"label": "Web", "value": "web"}
  ]
}
```

```json
{
  "id": "properties_license_expiration",
  "label": "License expiration",
  "group": "General Information",
  "position": 39,
  "type": "date"
}
```

```json
{
  "id": "attachments",
  "label": "Attachments",
  "group": "General Information",
  "position": 45,
  "type": "attachment"
}
```

#### Status Group (positions 9-14)

```json
{
  "id": "retired",
  "label": "Retirement",
  "group": "Status",
  "position": 9,
  "type": "select",
  "values": [
    {"label": "No", "value": false},
    {"label": "Yes", "value": true}
  ],
  "default": false
}
```

```json
{
  "id": "retired_reason",
  "label": "Reason for Retirement",
  "group": "Status",
  "position": 10,
  "type": "textarea",
  "required": true,
  "visibility_dependency_field_id": "retired",
  "visibility_dependency_value": "true"
}
```

```json
{
  "id": "is_broken",
  "label": "Working order",
  "group": "Status",
  "position": 11,
  "type": "radio",
  "values": [
    {"label": "OK", "value": false},
    {"label": "Broken", "value": true}
  ],
  "default": false
}
```

```json
{
  "id": "is_incomplete",
  "label": "Completeness",
  "group": "Status",
  "position": 12,
  "type": "radio",
  "values": [
    {"label": "OK", "value": false},
    {"label": "Incomplete", "value": true}
  ],
  "default": false
}
```

```json
{
  "id": "is_borrowable",
  "label": "Borrowable",
  "group": "Status",
  "position": 13,
  "type": "radio",
  "values": [
    {"label": "OK", "value": true},
    {"label": "Unborrowable", "value": false}
  ],
  "default": false
}
```

```json
{
  "id": "status_note",
  "label": "Status note",
  "group": "Status",
  "position": 14,
  "type": "textarea"
}
```

#### Location Group (positions 15-17)

```json
{
  "id": "building_id",
  "label": "Building",
  "group": "Location",
  "position": 15,
  "type": "autocomplete",
  "required": true,
  "values": [
    {"label": "Main Building", "value": "550e8400-e29b-41d4-a716-446655440001"},
    {"label": "Science Wing", "value": "550e8400-e29b-41d4-a716-446655440002"},
    {"label": "Arts Center", "value": "550e8400-e29b-41d4-a716-446655440003"}
  ]
}
```

```json
{
  "id": "room_id",
  "label": "Room",
  "group": "Location",
  "position": 16,
  "type": "autocomplete",
  "required": true,
  "values_url": "/inventory/:pool-id/rooms",
  "values_dependency_field_id": "building_id"
}
```

```json
{
  "id": "shelf",
  "label": "Shelf",
  "group": "Location",
  "position": 17,
  "type": "text"
}
```

#### Inventory Group (positions 18-23, 33)

```json
{
  "id": "is_inventory_relevant",
  "label": "Relevant for inventory",
  "group": "Inventory",
  "position": 18,
  "type": "select",
  "values": [
    {"label": "No", "value": false},
    {"label": "Yes", "value": true}
  ],
  "default": true
}
```

```json
{
  "id": "owner_id",
  "label": "Owner",
  "group": "Inventory",
  "position": 19,
  "type": "autocomplete",
  "values": [
    {"label": "IT Department", "value": "550e8400-e29b-41d4-a716-446655440010"},
    {"label": "Media Lab", "value": "550e8400-e29b-41d4-a716-446655440011"},
    {"label": "Library", "value": "550e8400-e29b-41d4-a716-446655440012"}
  ]
}
```

```json
{
  "id": "last_check",
  "label": "Last Checked",
  "group": "Inventory",
  "position": 20,
  "type": "date",
  "default": "today"
}
```

```json
{
  "id": "inventory_pool_id",
  "label": "Responsible department",
  "group": "Inventory",
  "position": 21,
  "type": "autocomplete",
  "values": [
    {"label": "IT Department", "value": "550e8400-e29b-41d4-a716-446655440010"},
    {"label": "Media Lab", "value": "550e8400-e29b-41d4-a716-446655440011"},
    {"label": "Library", "value": "550e8400-e29b-41d4-a716-446655440012"}
  ]
}
```

```json
{
  "id": "responsible",
  "label": "Responsible person",
  "group": "Inventory",
  "position": 22,
  "type": "text"
}
```

```json
{
  "id": "user_name",
  "label": "User/Typical usage",
  "group": "Inventory",
  "position": 23,
  "type": "text"
}
```

```json
{
  "id": "properties_anschaffungskategorie",
  "label": "Beschaffungsgruppe",
  "group": "Inventory",
  "position": 33,
  "type": "select",
  "value_label": ["properties", "anschaffungskategorie"],
  "values": [
    {"label": "", "value": null},
    {"label": "Werkstatt-Technik", "value": "Werkstatt-Technik"},
    {"label": "Produktionstechnik", "value": "Produktionstechnik"},
    {"label": "AV-Technik", "value": "AV-Technik"},
    {"label": "Musikinstrumente", "value": "Musikinstrumente"},
    {"label": "Facility Management", "value": "Facility Management"},
    {"label": "IC-Technik/Software", "value": "IC-Technik/Software"}
  ],
  "default": null,
  "required": false,
  "visibility_dependency_field_id": "is_inventory_relevant",
  "visibility_dependency_value": "true"
}
```

#### Invoice Information Group (positions 0, 24-31, 44)

```json
{
  "id": "properties_p4u",
  "label": "P4U-Nummer",
  "group": "Invoice Information",
  "position": 0,
  "type": "text"
}
```

```json
{
  "id": "properties_invoice_number",
  "label": "Gruppe Zugehörigkeit",
  "group": "Invoice Information",
  "position": 0,
  "type": "select",
  "values": [
    {"label": "SUSO", "value": "SUSO"},
    {"label": "PROPO", "value": null}
  ],
  "default": "SUSO"
}
```

```json
{
  "id": "properties_reference",
  "label": "Reference",
  "group": "Invoice Information",
  "position": 24,
  "type": "radio",
  "values": [
    {"label": "Running Account", "value": "invoice"},
    {"label": "Investment", "value": "investment"}
  ],
  "default": "invoice",
  "required": true
}
```

```json
{
  "id": "properties_project_number",
  "label": "Project Number",
  "group": "Invoice Information",
  "position": 25,
  "type": "text",
  "required": true,
  "visibility_dependency_field_id": "properties_reference",
  "visibility_dependency_value": "investment"
}
```

```json
{
  "id": "invoice_number",
  "label": "Invoice Number",
  "group": "Invoice Information",
  "position": 26,
  "type": "text"
}
```

```json
{
  "id": "invoice_date",
  "label": "Invoice Date",
  "group": "Invoice Information",
  "position": 27,
  "type": "date"
}
```

```json
{
  "id": "price",
  "label": "Initial Price",
  "group": "Invoice Information",
  "position": 28,
  "type": "text",
  "currency": true
}
```

```json
{
  "id": "supplier_id",
  "label": "Supplier",
  "group": "Invoice Information",
  "position": 29,
  "type": "autocomplete",
  "extended_key": ["supplier", "name"],
  "values": [
    {"label": "Dell Inc.", "value": "550e8400-e29b-41d4-a716-446655440020"},
    {"label": "Apple Inc.", "value": "550e8400-e29b-41d4-a716-446655440021"},
    {"label": "Adobe Systems", "value": "550e8400-e29b-41d4-a716-446655440022"}
  ],
  "extensible": true
}
```

```json
{
  "id": "properties_warranty_expiration",
  "label": "Warranty expiration",
  "group": "Invoice Information",
  "position": 30,
  "type": "date"
}
```

```json
{
  "id": "properties_contract_expiration",
  "label": "Contract expiration",
  "group": "Invoice Information",
  "position": 31,
  "type": "date"
}
```

```json
{
  "id": "properties_procured_by",
  "label": "Procured by",
  "group": "Invoice Information",
  "position": 44,
  "type": "text"
}
```

#### Maintenance Group (positions 40-43)

```json
{
  "id": "properties_maintenance_contract",
  "label": "Maintenance contract",
  "group": "Maintenance",
  "position": 40,
  "type": "select",
  "values": [
    {"label": "No", "value": "false"},
    {"label": "Yes", "value": "true"}
  ],
  "default": "false"
}
```

```json
{
  "id": "properties_maintenance_expiration",
  "label": "Maintenance expiration",
  "group": "Maintenance",
  "position": 41,
  "type": "date",
  "visibility_dependency_field_id": "properties_maintenance_contract",
  "visibility_dependency_value": "true"
}
```

```json
{
  "id": "properties_maintenance_currency",
  "label": "Currency",
  "group": "Maintenance",
  "position": 42,
  "type": "select",
  "values": [
    {"label": "CHF", "value": "CHF"},
    {"label": "EUR", "value": "EUR"},
    {"label": "USD", "value": "USD"}
  ],
  "default": "CHF",
  "visibility_dependency_field_id": "properties_maintenance_expiration"
}
```

```json
{
  "id": "properties_maintenance_price",
  "label": "Price",
  "group": "Maintenance",
  "position": 43,
  "type": "text",
  "currency": true,
  "visibility_dependency_field_id": "properties_maintenance_currency"
}
```

#### Eigenschaften Group (positions 0)

```json
{
  "id": "properties_ampere",
  "label": "Ampère",
  "group": "Eigenschaften",
  "position": 0,
  "type": "text"
}
```

```json
{
  "id": "properties_installation_status",
  "label": "Installation Status",
  "group": "Eigenschaften",
  "position": 0,
  "type": "radio",
  "values": [
    {"label": "Im Lager", "value": "inStorage"},
    {"label": "Verbaut", "value": "installed"}
  ],
  "default": "inStorage"
}
```

```json
{
  "id": "properties_electrical_power",
  "label": "Verbrauch in kw/h",
  "group": "Eigenschaften",
  "position": 0,
  "type": "text"
}
```

#### Umzug Group (positions 43-44)

```json
{
  "id": "properties_umzug",
  "label": "Umzug",
  "group": "Umzug",
  "position": 43,
  "type": "select",
  "values": [
    {"label": "zügeln", "value": "zügeln"},
    {"label": "sofort entsorgen", "value": "sofort entsorgen"},
    {"label": "bei Umzug entsorgen", "value": "bei Umzug entsorgen"},
    {"label": "bei Umzug verkaufen", "value": "bei Umzug verkaufen"}
  ],
  "default": "zügeln"
}
```

```json
{
  "id": "properties_zielraum",
  "label": "Zielraum",
  "group": "Umzug",
  "position": 44,
  "type": "text"
}
```

#### Toni Ankunftskontrolle Group (positions 45-47)

```json
{
  "id": "properties_ankunftsdatum",
  "label": "Ankunftsdatum",
  "group": "Toni Ankunftskontrolle",
  "position": 45,
  "type": "date"
}
```

```json
{
  "id": "properties_ankunftszustand",
  "label": "Ankunftszustand",
  "group": "Toni Ankunftskontrolle",
  "position": 46,
  "type": "select",
  "values": [
    {"label": "intakt", "value": "intakt"},
    {"label": "transportschaden", "value": "transportschaden"}
  ],
  "default": "intakt"
}
```

```json
{
  "id": "properties_ankunftsnotiz",
  "label": "Ankunftsnotiz",
  "group": "Toni Ankunftskontrolle",
  "position": 47,
  "type": "textarea"
}
```

#### Test Fields (position 0)

```json
{
  "id": "properties_test",
  "label": "test",
  "group": null,
  "position": 0,
  "type": "textarea"
}
```

---

## POST Request Format

When submitting item/license data, use a simplified flat structure with only field values:

```json
{
  "inventory_code": "P-AUS2008037",
  "model_id": "550e8400-e29b-41d4-a716-446655440000",
  "serial_number": "SN123456",
  "inventory_pool_id": "550e8400-e29b-41d4-a716-446655440001",
  "owner_id": "550e8400-e29b-41d4-a716-446655440001",
  "room_id": "550e8400-e29b-41d4-a716-446655440002",
  "shelf": "A3",
  "is_borrowable": true,
  "is_broken": false,
  "is_incomplete": false,
  "is_inventory_relevant": true,
  "retired": false,
  "note": "Sample note",
  "price": "1500.00",
  "invoice_date": "2025-01-15",
  "invoice_number": "INV-2025-001",
  "last_check": "2025-10-20",
  "properties_anschaffungskategorie": "AV-Technik",
  "properties_reference": "investment",
  "properties_project_number": "PRJ-2025-001",
  "properties_imei_number": "123456789012345",
  "properties_warranty_expiration": "2027-01-15",
  "properties_electrical_power": "150",
  "properties_installation_status": "installed"
}
```

### Field Naming Convention for POST

- Core fields: Use the field `id` directly (e.g., `inventory_code`, `serial_number`)
- Property fields: Use the field `id` with `properties_` prefix (e.g., `properties_anschaffungskategorie`)
- Nested attributes: For fields like `supplier_id`, use the `form_name` if available, otherwise use the field `id`

---

## Field Type Descriptions

### Field Types

1. **text**: Single-line text input
2. **textarea**: Multi-line text input
3. **select**: Dropdown with predefined options
4. **radio**: Radio button group
5. **checkbox**: Multiple selection checkboxes
6. **date**: Date picker
7. **autocomplete**: Autocomplete with predefined values list
8. **autocomplete-search**: Autocomplete with server-side search
9. **attachment**: File upload field
10. **composite**: Complex nested structure (e.g., quantity allocations)

### Key Field Attributes

- **id**: Unique field identifier
- **label**: Display name for the field
- **group**: Grouping category (e.g., "Inventory", "Status", "General Information")
- **position**: Display order within forms
- **type**: Input type (see Field Types above)
- **required**: Boolean indicating if field is mandatory
- **default**: Default value for the field
- **currency**: Boolean indicating if field represents currency value
- **extensible**: Boolean allowing creation of new values (for autocomplete fields)
- **form_name**: Alternative name to use in form submission

### Dependency Attributes

- **visibility_dependency_field_id**: Field ID that controls visibility
- **visibility_dependency_value**: Value that makes field visible
- **values_dependency_field_id**: Field ID that provides values
- **data_dependency_field_id**: Field ID that provides data context

### Values Attributes

- **values**: Array of `{label, value}` objects for static options
- **values_url**: Dynamic URL for fetching dependent values
- **value_label**: Path to the value label in item object

### Autocomplete-Search Specific Attributes

- **search_path**: API endpoint path for search
- **search_attr**: Query parameter name for search term
- **value_attr**: Attribute to use as the submitted value

---

## Notes

1. **Position 0 fields**: Many dynamic/custom fields have position 0, indicating they may be added to forms in flexible positions or grouped separately
2. **Dependencies**: Multiple types of dependencies control visibility, values, and data context between fields
3. **Special values**: Some autocomplete fields use special string values like "all_buildings" to indicate the data source
4. **Extended keys**: The `extended_key` attribute allows creating new entries in autocomplete fields (when `extensible: true`)
