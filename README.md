# NOVA E-Commerce

A Spring Boot e-commerce REST API with JWT authentication.

---

## Tech Stack

- Java 17, Spring Boot 3.2
- Spring Security + JWT
- H2 in-memory database
- Maven
- Docker

---

## Prerequisites

- An AWS EC2 instance running **Ubuntu 22.04**
- Inbound Security Group rules:

  | Port | Source | Purpose |
  |------|--------|---------|
  | 22 | Your IP | SSH |
  | 8080 | 0.0.0.0/0 | App access |

---

## Deployment Steps

### 1. SSH into your EC2 instance

```bash
chmod 400 your-key.pem
ssh -i your-key.pem ubuntu@<EC2-PUBLIC-IP>
```

### 2. Install Java 17

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk
java -version
```

### 3. Clone the repository

```bash
git clone https://github.com/caveaku/ecommerce-app.git
cd ecommerce-app
```

### 4. Install Maven

```bash
sudo apt install -y maven
```

### 5. Build the app

```bash
mvn clean package -DskipTests
```

### 6. Run the app

```bash
java -jar target/ecommerce-app-1.0.0.jar
```

The app will be available at **http://\<EC2-PUBLIC-IP\>:8080**

---

## Test the API

```bash
# Health check
curl http://<EC2-PUBLIC-IP>:8080/actuator/health

# Register a user
curl -X POST http://<EC2-PUBLIC-IP>:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"John","email":"john@test.com","password":"password123"}'

# Login and get JWT token
curl -X POST http://<EC2-PUBLIC-IP>:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john@test.com","password":"password123"}'

# List products (use the token from login above)
curl http://<EC2-PUBLIC-IP>:8080/api/products \
  -H "Authorization: Bearer <your-token>"
```

### Demo credentials

| Role | Email | Password |
|------|-------|----------|
| Admin | `admin@shop.com` | `admin123` |
| User | `user@shop.com` | `user123` |

---

## Run with Docker (Alternative)

### 1. Install Docker

```bash
sudo apt update
sudo apt install -y docker.io
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker ubuntu
newgrp docker
```

### 2. Build the Docker image

```bash
docker build -t ecommerce-app .
```

### 3. Run the container

```bash
docker run -d -p 8080:8080 --name ecommerce ecommerce-app
```

The app will be available at **http://\<EC2-PUBLIC-IP\>:8080**

### Docker management commands

```bash
docker ps                      # list running containers
docker logs ecommerce          # view logs
docker logs -f ecommerce       # live logs
docker restart ecommerce       # restart container
docker stop ecommerce          # stop container
docker rm ecommerce            # remove container
docker rmi ecommerce-app       # remove image
```

---

## Keep the App Running (optional)

If you want the app to keep running after you close your SSH session:

```bash
nohup java -jar target/ecommerce-app-1.0.0.jar > app.log 2>&1 &

# Check logs
tail -f app.log

# Stop the app
kill $(lsof -t -i:8080)
```

---

## API Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/auth/register` | None | Register |
| POST | `/api/auth/login` | None | Login, returns JWT |
| GET | `/api/products` | JWT | List products |
| GET | `/api/products/{id}` | JWT | Get product |
| GET | `/api/products/search?q=` | JWT | Search products |
| POST | `/api/products` | Admin JWT | Create product |
| PUT | `/api/products/{id}` | Admin JWT | Update product |
| DELETE | `/api/products/{id}` | Admin JWT | Delete product |
| GET | `/api/categories` | JWT | List categories |
| GET | `/api/orders` | JWT | My orders |
| POST | `/api/orders` | JWT | Place order |
| GET | `/actuator/health` | None | Health check |
