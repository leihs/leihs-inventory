#!/usr/bin/env bash
set -euo pipefail

FEATURE='./spec/features/views/inventory_spec.rb'
PG15PORT=5415
PG15USER=leihs

#echo "# FEATURE_NAME: $FEATURE_NAME"
echo "# FEATURE: $FEATURE"
mkdir -p log
unset PGPORT; unset PGUSER
PGPORT=${PG15PORT} PGUSER=${PG15USER} \
  ./bin/rspec "${FEATURE}"