(ns leihs.inventory.server.utils.auth.roles)

(def min-role-lending-manager
  "Roles that have at least lending manager permissions.
  - inventory-manager
  - lending-manager"
  #{:inventory_manager :lending_manager})
