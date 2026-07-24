#!/usr/bin/env bash
# One-time setup for the StudyHub backend on a fresh Oracle Cloud Always Free
# AMD micro (VM.Standard.E2.1.Micro, Ubuntu 22.04 x86_64, 1GB RAM) instance.
# Safe to re-run.
set -euo pipefail

REPO_URL="https://github.com/dummytemp87-code/Java_Pkrris.git"
APP_DIR="$HOME/Java_Pkrris"
ENV_FILE="/etc/studyhub-backend.env"
SERVICE_FILE="/etc/systemd/system/studyhub-backend.service"
CADDYFILE="/etc/caddy/Caddyfile"

echo "==> Setting up 4GB swap (required on a 1GB VM to survive the Maven build and give the JVM headroom)..."
if ! swapon --show | grep -q '/swapfile'; then
  sudo fallocate -l 4G /swapfile || sudo dd if=/dev/zero of=/swapfile bs=1M count=4096
  sudo chmod 600 /swapfile
  sudo mkswap /swapfile
  sudo swapon /swapfile
  grep -q '/swapfile' /etc/fstab || echo '/swapfile swap swap defaults 0 0' | sudo tee -a /etc/fstab
fi
echo "    $(swapon --show)"

echo "==> Installing Java 17, Maven, git, curl..."
sudo apt-get update -y
sudo apt-get install -y openjdk-17-jdk-headless maven git curl debian-keyring debian-archive-keyring apt-transport-https gnupg

echo "==> Installing Caddy..."
if ! command -v caddy >/dev/null 2>&1; then
  curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' | sudo gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
  curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' | sudo tee /etc/apt/sources.list.d/caddy-stable.list
  sudo apt-get update -y
  sudo apt-get install -y caddy
fi

echo "==> Cloning/updating StudyHub repo..."
if [ -d "$APP_DIR/.git" ]; then
  git -C "$APP_DIR" pull
else
  git clone "$REPO_URL" "$APP_DIR"
fi

echo "==> Building backend (on a 1 OCPU/1GB VM this can take 10-20 minutes on first run -- be patient)..."
cd "$APP_DIR/spring-backend"
mvn -q -DskipTests clean package

echo "==> Checking secrets file at $ENV_FILE..."
if [ ! -f "$ENV_FILE" ]; then
  sudo cp "$APP_DIR/spring-backend/deploy/studyhub-backend.env.example" "$ENV_FILE"
  sudo chmod 600 "$ENV_FILE"
  echo ""
  echo "!! $ENV_FILE was just created with empty values."
  echo "!! Edit it now (sudo nano $ENV_FILE), fill in DB_USERNAME, DB_PASSWORD, JWT_SECRET,"
  echo "!! YOUTUBE_API_KEY, GEMINI_API_KEY, and CORS_ALLOWED_ORIGINS, then re-run this script."
  exit 1
fi

echo "==> Installing systemd service..."
sudo cp "$APP_DIR/spring-backend/deploy/studyhub-backend.service" "$SERVICE_FILE"
sudo systemctl daemon-reload
sudo systemctl enable studyhub-backend
sudo systemctl restart studyhub-backend

echo "==> Detecting public IP for HTTPS hostname (via nip.io)..."
PUBLIC_IP=$(curl -4 -s ifconfig.me)
PUBLIC_IP_DASHED=$(echo "$PUBLIC_IP" | tr '.' '-')
echo "    Public IP: $PUBLIC_IP  ->  https://${PUBLIC_IP_DASHED}.nip.io"

echo "==> Writing Caddyfile..."
sed "s/{{PUBLIC_IP_DASHED}}/${PUBLIC_IP_DASHED}/" "$APP_DIR/spring-backend/deploy/Caddyfile.template" | sudo tee "$CADDYFILE" > /dev/null
sudo systemctl enable caddy
sudo systemctl restart caddy

echo "==> Opening firewall ports 80/443 (OS-level iptables, in addition to the OCI Security List)..."
sudo iptables -C INPUT -p tcp --dport 80 -j ACCEPT 2>/dev/null || sudo iptables -I INPUT 6 -p tcp --dport 80 -j ACCEPT
sudo iptables -C INPUT -p tcp --dport 443 -j ACCEPT 2>/dev/null || sudo iptables -I INPUT 6 -p tcp --dport 443 -j ACCEPT
sudo netfilter-persistent save 2>/dev/null || sudo sh -c 'iptables-save > /etc/iptables/rules.v4' 2>/dev/null || true

echo ""
echo "==> Done. Backend should be reachable at: https://${PUBLIC_IP_DASHED}.nip.io"
echo "    Check status: sudo systemctl status studyhub-backend"
echo "    Check logs:   sudo journalctl -u studyhub-backend -f"
echo "    Test health:  curl https://${PUBLIC_IP_DASHED}.nip.io/actuator/health"
echo ""
echo "!! Remember: this still requires opening TCP 80 and 443 (and 22 for SSH) in the"
echo "!! OCI Security List / Network Security Group for this VM's subnet in the console --"
echo "!! the OS-level iptables rules above are necessary but not sufficient by themselves."
