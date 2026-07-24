#!/usr/bin/env bash
# Lets SSH work over port 443 (via sslh) for networks/ISPs that block outbound
# port 22, while Caddy continues to serve real HTTPS on the same public port 443.
# sslh listens on the public 443, inspects each connection, and forwards SSH
# traffic to local sshd (22) and everything else (TLS) to Caddy (moved to 8443).
#
# Run this AFTER setup-vm.sh has already completed (Caddy must be installed).
# Safe to re-run.
set -euo pipefail

CADDYFILE="/etc/caddy/Caddyfile"

echo "==> Installing sslh..."
echo "sslh sslh/inetd_or_standalone select standalone" | sudo debconf-set-selections
sudo DEBIAN_FRONTEND=noninteractive apt-get update -y
sudo DEBIAN_FRONTEND=noninteractive apt-get install -y sslh

echo "==> Configuring sslh (public 443 -> sshd:22 or Caddy:8443, by protocol)..."
sudo tee /etc/default/sslh > /dev/null << 'EOF'
RUN=yes
DAEMON_OPTS="--user sslh --listen 0.0.0.0:443 --ssh 127.0.0.1:22 --tls 127.0.0.1:8443 --pidfile /var/run/sslh.pid"
EOF

echo "==> Moving Caddy's HTTPS listener to 8443 (behind sslh)..."
if [ ! -f "$CADDYFILE" ]; then
  echo "!! $CADDYFILE not found -- run setup-vm.sh first to install Caddy."
  exit 1
fi
if ! grep -q "https_port 8443" "$CADDYFILE"; then
  TMP=$(mktemp)
  { echo "{"; echo "    https_port 8443"; echo "}"; echo; cat "$CADDYFILE"; } > "$TMP"
  sudo cp "$TMP" "$CADDYFILE"
  rm "$TMP"
fi

echo "==> Enabling and starting sslh, restarting Caddy..."
sudo systemctl enable sslh
sudo systemctl restart sslh
sudo systemctl restart caddy

echo ""
echo "==> Done. SSH now also works over port 443, in addition to 22:"
echo "    ssh -p 443 -i <your-key> ubuntu@<VM_PUBLIC_IP>"
echo "    Check sslh:  sudo systemctl status sslh --no-pager"
echo "    Check Caddy: sudo systemctl status caddy --no-pager"
