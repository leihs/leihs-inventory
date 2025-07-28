(ns leihs.inventory.server.utils.auth.roles)

(def min-role-inventory-manager
  "Roles that have at least inventory manager permissions.
  - inventory-manager"
  #{:inventory_manager})

(def min-role-lending-manager
  "Roles that have at least lending manager permissions.
  - inventory-manager
  - lending-manager"
  #{:inventory_manager :lending_manager})

(def min-role-group-manager
  "Roles that have at least lending manager permissions.
  - inventory-manager
  - lending-manager
  - group-manager"
  #{:inventory_manager :lending_manager :group_manager})

(def min-role-customer
  "Roles that have at least lending manager permissions.
  - inventory-manager
  - lending-manager
  - group-manager
  - customer"
  #{:inventory_manager :lending_manager :group_manager :customer})
