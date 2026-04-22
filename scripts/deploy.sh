#!/bin/bash
# =============================================================================
# NOVA E-Commerce — EC2 Ubuntu Deployment Script
# Run this on a fresh Ubuntu 22.04 LTS EC2 instance
# Usage: chmod +x deploy.sh && sudo ./deploy.sh
# =============================================================================
set -e

APP_NAME="ecommerce-app"
APP_DIR="/opt/ecommerce"
JAR_NAME="ecommerce-app-1.0.0.jar"
JAVA_VERSION="17"
APP_PORT="8080"
NGINX_PORT="80"

echo "============================================"
echo " NOVA E-Commerce — EC2 Deployment Script"
echo "============================================"

# ── Step 1: System update ─────────────────────────────────────────────────────
echo "[1/8] Updating system packages..."
apt-get update -y && apt-get upgrade -y

# ── Step 2: Install Java 17 ───────────────────────────────────────────────────
echo "[2/8] Installing Java $JAVA_VERSION..."
apt-get install -y openjdk-17-jdk
java -version

# ── Step 3: Install Nginx ─────────────────────────────────────────────────────
echo "[3/8] Installing Nginx..."
apt-get install -y nginx
systemctl enable nginx

# ── Step 4: Create app directory and user ────────────────────────────────────
echo "[4/8] Creating app user and directories..."
id -u ecommerce &>/dev/null || useradd -r -s /bin/false ecommerce
mkdir -p $APP_DIR/logs
chown -R ecommerce:ecommerce $APP_DIR

# ── Step 5: Configure Nginx reverse proxy ────────────────────────────────────
echo "[5/8] Configuring Nginx..."
cat > /etc/nginx/sites-available/ecommerce <<EOF
server {
    listen $NGINX_PORT;
    server_name _;

    # Gzip compression
    gzip on;
    gzip_types text/plain text/css application/json application/javascript;

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN";
    add_header X-XSS-Protection "1; mode=block";
    add_header X-Content-Type-Options nosniff;

    location / {
        proxy_pass http://127.0.0.1:$APP_PORT;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_cache_bypass \$http_upgrade;
        proxy_read_timeout 90s;
    }
}
EOF

ln -sf /etc/nginx/sites-available/ecommerce /etc/nginx/sites-enabled/
rm -f /etc/nginx/sites-enabled/default
nginx -t && systemctl restart nginx
echo "✅ Nginx configured."

# ── Step 6: Configure environment variables ──────────────────────────────────
echo "[6/8] Setting up environment config..."
cat > $APP_DIR/.env <<EOF
# JWT Secret — CHANGE THIS in production!
JWT_SECRET=nova-production-secret-key-$(openssl rand -hex 16)

# For production MySQL (fill in your RDS details):
# DB_HOST=your-rds-endpoint.rds.amazonaws.com
# DB_NAME=ecommerce
# DB_USER=admin
# DB_PASSWORD=yourpassword
EOF
chmod 600 $APP_DIR/.env
chown ecommerce:ecommerce $APP_DIR/.env
echo "✅ Environment file created at $APP_DIR/.env"

# ── Step 7: Create systemd service ───────────────────────────────────────────
echo "[7/8] Creating systemd service..."
cat > /etc/systemd/system/ecommerce.service <<EOF
[Unit]
Description=NOVA E-Commerce Application
After=network.target
Wants=network-online.target

[Service]
Type=simple
User=ecommerce
Group=ecommerce
WorkingDirectory=$APP_DIR

# Load environment variables
EnvironmentFile=$APP_DIR/.env

# Java options — tune heap for your instance size
Environment="JAVA_OPTS=-Xms256m -Xmx512m -XX:+UseG1GC"

ExecStart=/usr/bin/java \$JAVA_OPTS -jar $APP_DIR/$JAR_NAME
ExecStop=/bin/kill -TERM \$MAINPID

# Restart on failure
Restart=always
RestartSec=10
SuccessExitStatus=143

# Output logs to journald
StandardOutput=journal
StandardError=journal
SyslogIdentifier=ecommerce

# Security hardening
NoNewPrivileges=true
PrivateTmp=true

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable ecommerce
echo "✅ Systemd service created."

# ── Step 8: Configure firewall ────────────────────────────────────────────────
echo "[8/8] Configuring UFW firewall..."
apt-get install -y ufw
ufw default deny incoming
ufw default allow outgoing
ufw allow ssh
ufw allow http
ufw allow https
echo "y" | ufw enable
echo "✅ Firewall configured."

echo ""
echo "============================================"
echo "  ✅ Server setup complete!"
echo "============================================"
echo ""
echo "Next steps:"
echo "  1. Upload your JAR:  scp target/$JAR_NAME ubuntu@<EC2-IP>:$APP_DIR/"
echo "  2. Set permissions:  sudo chown ecommerce:ecommerce $APP_DIR/$JAR_NAME"
echo "  3. Start the app:    sudo systemctl start ecommerce"
echo "  4. Check status:     sudo systemctl status ecommerce"
echo "  5. View logs:        sudo journalctl -u ecommerce -f"
echo "  6. App URL:          http://\$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4)"
echo ""
