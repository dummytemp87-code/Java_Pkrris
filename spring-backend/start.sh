#!/usr/bin/env bash
# Loads spring-backend/.env into the environment and starts the backend.
set -euo pipefail
cd "$(dirname "$0")"
if [ ! -f .env ]; then
  echo ".env not found. Copy .env.example to .env and fill in real values." >&2
  exit 1
fi
set -a
source .env
set +a
mvn -q clean spring-boot:run
