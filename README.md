# NOVA E-Commerce

A production-ready RESTful e-commerce backend built with Spring Boot 3, Spring Security (JWT), JPA/Hibernate, and an Nginx reverse proxy on AWS EC2.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| Security | Spring Security + JWT (jjwt 0.11.5) |
| Persistence | Spring Data JPA / Hibernate |
| Database (dev) | H2 (in-memory) |
| Database (prod) | Amazon RDS MySQL 8 |
| Build | Maven 3.8+ |
| Reverse Proxy | Nginx |
| Hosting | AWS EC2 Ubuntu 22.04 |

---

## Project Structure

```
ecommerce/
├── src/main/java/com/ecommerce/
│   ├── EcommerceApplication.java       # Entry point
│   ├── config/
│   │   ├── SecurityConfig.java         # JWT filter chain
│   │   └── JwtUtil.java                # Token generation & validation
│   ├── controller/
│   │   ├── AuthController.java         # /api/auth/*
│   │   ├── ProductController.java      # /api/products/*
│   │   ├── CategoryController.java     # /api/categories/*
│   │   └── OrderController.java        # /api/orders/*
│   ├── model/                          # JPA entities
│   ├── dto/                            # Request/response DTOs
│   └── repository/                     # Spring Data repositories
├── src/main/resources/
│   ├── application.properties          # App config
│   └── static/index.html               # Landing page
├── scripts/
│   └── deploy.sh                       # Automated EC2 setup script
├── DEPLOYMENT.md                       # Full AWS deployment guide
└── pom.xml
```

---

## Local Development

### Prerequisites

- Java 17+
- Maven 3.8+

### Run locally

```bash
# Clone the repository
git clone https://github.com/caveaku/ecommerce-app.git
cd ecommerce-app

# Run with Maven (H2 in-memory DB, no extra config needed)
mvn spring-boot:run
```

The app starts on **http://localhost:8080**.

H2 console (dev only): **http://localhost:8080/h2-console**
- JDBC URL: `jdbc:h2:mem:ecommercedb`
- Username: `sa` / Password: *(empty)*

### Build the JAR

```bash
mvn clean package -DskipTests

# Output
ls target/ecommerce-app-1.0.0.jar
```

---

## Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `JWT_SECRET` | No* | built-in dev key | HS256 signing secret (min 32 chars) |
| `DB_HOST` | Prod only | — | RDS endpoint |
| `DB_NAME` | Prod only | — | Database name |
| `DB_USER` | Prod only | — | Database username |
| `DB_PASSWORD` | Prod only | — | Database password |

> *Always set `JWT_SECRET` in production.

---

## Deployment to AWS EC2

For the full step-by-step guide see **[DEPLOYMENT.md](DEPLOYMENT.md)**. Here is a summary of the four phases:

### Phase 1 — Launch EC2 Instance

1. Go to **EC2 → Launch Instance** in the AWS Console.
2. Select **Ubuntu Server 22.04 LTS**, instance type `t2.micro` (free tier) or `t3.small`.
3. Create a `.pem` key pair and save it securely.
4. Add these inbound Security Group rules:

   | Port | Source | Purpose |
   |------|--------|---------|
   | 22 | Your IP | SSH |
   | 80 | 0.0.0.0/0 | HTTP |
   | 443 | 0.0.0.0/0 | HTTPS |

> Port 8080 must **not** be exposed — Nginx proxies all traffic.

---

### Phase 2 — Server Setup (one-time)

Upload and run the automated setup script from your local machine:

```bash
# Upload the script
scp -i ~/your-key.pem scripts/deploy.sh ubuntu@<EC2-PUBLIC-IP>:~/

# SSH in and run it
ssh -i ~/your-key.pem ubuntu@<EC2-PUBLIC-IP>
sudo bash ~/deploy.sh
```

The script installs Java 17, Nginx, UFW firewall, creates the `ecommerce` system user, configures the Nginx reverse proxy, generates an `.env` secrets file, and registers a `systemd` service that auto-starts on reboot.

---

### Phase 3 — Deploy the Application

```bash
# 1. Build the JAR locally
mvn clean package -DskipTests

# 2. Upload the JAR to the server
scp -i ~/your-key.pem \
  target/ecommerce-app-1.0.0.jar \
  ubuntu@<EC2-PUBLIC-IP>:/opt/ecommerce/

# 3. SSH in and fix ownership
ssh -i ~/your-key.pem ubuntu@<EC2-PUBLIC-IP>
sudo chown ecommerce:ecommerce /opt/ecommerce/ecommerce-app-1.0.0.jar

# 4. Set production secrets
sudo nano /opt/ecommerce/.env
```

Minimum `.env` for production:

```env
JWT_SECRET=your-very-long-random-secret-here-32-chars-min
DB_HOST=your-rds-endpoint.us-east-1.rds.amazonaws.com
DB_NAME=ecommerce
DB_USER=admin
DB_PASSWORD=your-secure-password
```

```bash
# 5. Start the service
sudo systemctl start ecommerce
sudo systemctl status ecommerce
```

---

### Phase 4 — Verify

```bash
# Health check via Nginx
curl http://<EC2-PUBLIC-IP>/actuator/health

# List products
curl http://<EC2-PUBLIC-IP>/api/products
```

---

## Useful Operations

```bash
# Live logs
sudo journalctl -u ecommerce -f

# Restart / stop
sudo systemctl restart ecommerce
sudo systemctl stop ecommerce

# Re-deploy a new JAR
sudo systemctl stop ecommerce
scp -i ~/your-key.pem target/ecommerce-app-1.0.0.jar ubuntu@<EC2-IP>:/opt/ecommerce/
sudo chown ecommerce:ecommerce /opt/ecommerce/ecommerce-app-1.0.0.jar
sudo systemctl start ecommerce
```

---

## API Reference

### Auth

| Method | Endpoint | Auth | Body |
|--------|----------|------|------|
| POST | `/api/auth/register` | Public | `{ "name", "email", "password" }` |
| POST | `/api/auth/login` | Public | `{ "email", "password" }` → returns JWT |

### Products

| Method | Endpoint | Auth |
|--------|----------|------|
| GET | `/api/products` | Public |
| GET | `/api/products/{id}` | Public |
| GET | `/api/products/search?q=term` | Public |
| POST | `/api/products` | Admin |
| PUT | `/api/products/{id}` | Admin |
| DELETE | `/api/products/{id}` | Admin |

### Categories & Orders

| Method | Endpoint | Auth |
|--------|----------|------|
| GET | `/api/categories` | Public |
| GET | `/api/orders` | User (JWT) |
| POST | `/api/orders` | User (JWT) |

### Health

```
GET /actuator/health
```

### Using JWT

After login, include the token in every protected request:

```
Authorization: Bearer <your-jwt-token>
```

---

## Demo Credentials

| Role | Email | Password |
|------|-------|----------|
| Admin | `admin@shop.com` | `admin123` |
| User | `user@shop.com` | `user123` |

---

## Production Checklist

- [ ] Set a strong `JWT_SECRET` (32+ random characters)
- [ ] Switch database from H2 to Amazon RDS MySQL (see [DEPLOYMENT.md](DEPLOYMENT.md#62--switch-to-amazon-rds-mysql))
- [ ] Enable HTTPS with Let's Encrypt (see [DEPLOYMENT.md](DEPLOYMENT.md#61--add-https-with-lets-encrypt))
- [ ] Assign an Elastic IP so your address survives reboots
- [ ] Set `spring.jpa.hibernate.ddl-auto=validate` (not `create-drop`) in prod
- [ ] Enable CloudWatch Logs for centralized log management

---

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| App won't start | `sudo journalctl -u ecommerce -n 50 --no-pager` |
| Port 8080 refused | `sudo ss -tlnp \| grep 8080` |
| Nginx 502 Bad Gateway | App not up yet — check logs above |
| Out of memory | Edit `JAVA_OPTS` in `/etc/systemd/system/ecommerce.service` |
| DB connection refused | Verify RDS security group allows EC2 on port 3306 |
| JWT errors | Ensure `JWT_SECRET` is at least 32 characters |

---

## License

MIT
