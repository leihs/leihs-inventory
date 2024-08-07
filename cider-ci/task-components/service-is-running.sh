#!/usr/bin/env bash
set -euo pipefail
until curl --silent --fail -I -X GET \
  -H 'Accept: application/json' \
  "http://localhost:${LEIHS_INVENTORY_HTTP_PORT}/inventory/status";
  do sleep 1;
done