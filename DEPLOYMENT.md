# NOVA E-Commerce — AWS EC2 Deployment Guide

## Architecture Overview

```
Internet → AWS Security Group → EC2 Ubuntu 22.04
                                 ├── Nginx (port 80/443)  ← reverse proxy
                                 └── Spring Boot (port 8080, internal only)
                                      └── H2 (dev) or RDS MySQL (prod)
```

---

## Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| Java JDK | 17 | Build & run the app |
| Maven | 3.8+ | Build tool |
| AWS CLI | v2 | AWS interaction |
| SSH client | any | EC2 access |

---

## PHASE 1 — Launch EC2 Instance

### 1.1 — Create the instance (AWS Console)

1. Go to **EC2 → Launch Instance**
2. Choose:
   - **AMI**: Ubuntu Server 22.04 LTS (free tier eligible)
   - **Instance type**: `t2.micro` (free tier) or `t3.small` for production
   - **Key pair**: Create a new `.pem` key pair and save it securely
3. **Network settings** → Edit:
   - VPC: default (or your custom VPC)
   - Enable **Auto-assign Public IP**
4. **Security Group rules** — Add these inbound rules:

| Type | Protocol | Port | Source | Purpose |
|------|----------|------|--------|---------|
| SSH | TCP | 22 | Your IP only | Remote access |
| HTTP | TCP | 80 | 0.0.0.0/0 | Web traffic |
| HTTPS | TCP | 443 | 0.0.0.0/0 | SSL (after setup) |

> ⚠️ **Never** open port 8080 to the internet — Nginx handles all traffic.

5. **Storage**: 20 GB gp3 (default 8 GB is fine for dev)
6. Click **Launch Instance**

### 1.2 — Or via AWS CLI

```bash
# Create security group
aws ec2 create-security-group \
  --group-name ecommerce-sg \
  --description "E-Commerce App SG"

# Add rules
aws ec2 authorize-security-group-ingress --group-name ecommerce-sg --protocol tcp --port 22  --cidr $(curl -s https://checkip.amazonaws.com)/32
aws ec2 authorize-security-group-ingress --group-name ecommerce-sg --protocol tcp --port 80  --cidr 0.0.0.0/0
aws ec2 authorize-security-group-ingress --group-name ecommerce-sg --protocol tcp --port 443 --cidr 0.0.0.0/0

# Launch instance
aws ec2 run-instances \
  --image-id ami-0c7217cdde317cfec \   # Ubuntu 22.04 us-east-1 (check your region!)
  --instance-type t2.micro \
  --key-name your-key-pair \
  --security-groups ecommerce-sg \
  --count 1 \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=ecommerce-app}]'
```

---

## PHASE 2 — Build the Application Locally

```bash
# Clone / navigate to the project
cd ecommerce-app/

# Build the fat JAR (skip tests for speed)
mvn clean package -DskipTests

# Verify the JAR was created
ls -lh target/ecommerce-app-1.0.0.jar
```

---

## PHASE 3 — Connect & Set Up the EC2 Server

### 3.1 — SSH into the instance

```bash
# Fix key permissions (required on Mac/Linux)
chmod 400 ~/your-key.pem

# Get your instance's public IP from the console, then:
ssh -i ~/your-key.pem ubuntu@<EC2-PUBLIC-IP>
```

### 3.2 — Run the automated setup script

```bash
# Upload the deploy script first
scp -i ~/your-key.pem scripts/deploy.sh ubuntu@<EC2-PUBLIC-IP>:~/

# SSH in and run it
ssh -i ~/your-key.pem ubuntu@<EC2-PUBLIC-IP>
sudo bash ~/deploy.sh
```

**What the script installs:**
- Java 17 (OpenJDK)
- Nginx (reverse proxy)
- UFW firewall rules
- systemd service (`ecommerce.service`)
- App directory at `/opt/ecommerce`
- Dedicated `ecommerce` system user

---

## PHASE 4 — Deploy the Application

### 4.1 — Upload the JAR

```bash
# From your local machine:
scp -i ~/your-key.pem \
  target/ecommerce-app-1.0.0.jar \
  ubuntu@<EC2-PUBLIC-IP>:/opt/ecommerce/

# Fix ownership on the server
ssh -i ~/your-key.pem ubuntu@<EC2-PUBLIC-IP>
sudo chown ecommerce:ecommerce /opt/ecommerce/ecommerce-app-1.0.0.jar
```

### 4.2 — Configure secrets

```bash
# Edit the environment file (pre-created by deploy.sh)
sudo nano /opt/ecommerce/.env
```

For production with RDS, add:
```env
JWT_SECRET=your-very-long-random-secret-here
DB_HOST=your-rds-endpoint.us-east-1.rds.amazonaws.com
DB_NAME=ecommerce
DB_USER=admin
DB_PASSWORD=your-secure-password
```

### 4.3 — Start the service

```bash
sudo systemctl start ecommerce
sudo systemctl status ecommerce
```

Expected output:
```
● ecommerce.service - NOVA E-Commerce Application
     Active: active (running) since ...
```

### 4.4 — Verify it's running

```bash
# Check the app responds on localhost
curl http://localhost:8080/actuator/health

# Check the API
curl http://localhost:8080/api/products

# Check through Nginx
curl http://<EC2-PUBLIC-IP>/api/products
```

---

## PHASE 5 — Useful Operations

### View logs in real-time
```bash
sudo journalctl -u ecommerce -f
```

### Restart the app
```bash
sudo systemctl restart ecommerce
```

### Stop / disable
```bash
sudo systemctl stop ecommerce
sudo systemctl disable ecommerce
```

### Check Nginx status
```bash
sudo systemctl status nginx
sudo nginx -t          # test config
sudo tail -f /var/log/nginx/error.log
```

### Re-deploy a new JAR (zero-config update)
```bash
# Stop, upload new JAR, start again
sudo systemctl stop ecommerce
scp -i ~/your-key.pem target/ecommerce-app-1.0.0.jar ubuntu@<IP>:/opt/ecommerce/
sudo chown ecommerce:ecommerce /opt/ecommerce/ecommerce-app-1.0.0.jar
sudo systemctl start ecommerce
```

---

## PHASE 6 — Production Hardening (Optional but Recommended)

### 6.1 — Add HTTPS with Let's Encrypt (requires a domain name)

```bash
sudo apt install certbot python3-certbot-nginx -y
sudo certbot --nginx -d yourdomain.com -d www.yourdomain.com
sudo systemctl reload nginx

# Auto-renew cron
sudo crontab -e
# Add: 0 12 * * * /usr/bin/certbot renew --quiet
```

### 6.2 — Switch to Amazon RDS (MySQL)

1. Create an RDS MySQL 8.0 instance in the same VPC
2. Add an inbound rule to RDS security group: port 3306 from the EC2 security group
3. Update `/opt/ecommerce/.env` with RDS credentials
4. In `application.properties`, uncomment the MySQL section and comment out H2
5. Restart: `sudo systemctl restart ecommerce`

### 6.3 — Set up an Elastic IP

```bash
# Allocate a static IP so reboots don't change your address
aws ec2 allocate-address --domain vpc

# Associate it with your instance
aws ec2 associate-address \
  --instance-id <your-instance-id> \
  --allocation-id <eipalloc-id>
```

### 6.4 — Enable CloudWatch Logs

```bash
sudo apt install amazon-cloudwatch-agent -y

# Configure the agent to stream journald logs
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-config-wizard
```

---

## API Reference

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/auth/register` | Public | Create account |
| POST | `/api/auth/login` | Public | Login, get JWT |
| GET | `/api/products` | Public | List all products |
| GET | `/api/products/{id}` | Public | Get product |
| GET | `/api/products/search?q=` | Public | Search products |
| GET | `/api/categories` | Public | List categories |
| POST | `/api/products` | Admin | Create product |
| PUT | `/api/products/{id}` | Admin | Update product |
| DELETE | `/api/products/{id}` | Admin | Soft-delete product |
| GET | `/api/orders` | User | My orders |
| POST | `/api/orders` | User | Place order |
| GET | `/actuator/health` | Public | Health check |

### Demo Credentials

| Role | Email | Password |
|------|-------|----------|
| Admin | `admin@shop.com` | `admin123` |
| User | `user@shop.com` | `user123` |

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| App won't start | `sudo journalctl -u ecommerce -n 50 --no-pager` |
| Port 8080 refused | Check `sudo ss -tlnp \| grep 8080` |
| Nginx 502 Bad Gateway | App hasn't started yet — check logs |
| Out of memory | Increase heap: edit `JAVA_OPTS` in the `.service` file |
| DB connection refused | Verify RDS security group allows EC2 inbound on 3306 |
| JWT errors | Ensure `JWT_SECRET` in `.env` is at least 32 characters |
