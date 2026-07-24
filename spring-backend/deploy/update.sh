#!/usr/bin/env bash
# Pulls latest code, rebuilds, and restarts the backend service.
set -euo pipefail
cd "$HOME/Java_Pkrris"
git pull
cd spring-backend
mvn -q -DskipTests clean package
sudo systemctl restart studyhub-backend
echo "==> Restarted. Tail logs with: sudo journalctl -u studyhub-backend -f"
